package com.example.addon.modules.misc;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.message.LastSeenMessagesCollector;
import net.minecraft.network.message.MessageBody;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import meteordevelopment.meteorclient.mixin.ClientPlayNetworkHandlerAccessor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;

public class ChatControl extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");
    private final SettingGroup sgChat = settings.createGroup("Chat");
    private final SettingGroup sgSpammer = settings.createGroup("Spammer");

    private final Setting<Boolean> visual = sgVisualRange.add(new BoolSetting.Builder()
        .name("visual-range")
        .description("Toggle visual range notification.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> checkUuid = sgVisualRange.add(new BoolSetting.Builder()
        .name("check-uuid")
        .description("Toggle checking player UUIDs.")
        .defaultValue(true)
        .visible(visual::get)
        .build()
    );

    private final Setting<Integer> maxAmount = sgVisualRange.add(new IntSetting.Builder()
        .name("max-amount")
        .description("The cap of how many players the visual range notifies.")
        .defaultValue(3)
        .visible(visual::get)
        .build()
    );

    private final Setting<Boolean> coordsProtection = sgChat.add(new BoolSetting.Builder()
        .name("coords-protection")
        .description("Prevents you from sending messages in chat that may contain coordinates.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> prefix = sgChat.add(new BoolSetting.Builder()
        .name("prefix")
        .description("Adds a prefix to your chat messages.")
        .defaultValue(true)
        .build()
    );
    
    

    private final Setting<String> prefixText = sgChat.add(new StringSetting.Builder()
        .name("prefix-text")
        .description("The text to add as your prefix.")
        .defaultValue(" | snail++")
        .visible(prefix::get)
        .build()
    );
    private final Setting<Boolean> green = sgChat.add(new BoolSetting.Builder()
        .name("Green text")
        .description("adds a '>' to your text to make it green")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Boolean> spam = sgSpammer.add(new BoolSetting.Builder()
        .name("spammer")
        .description("Spams words")
        .defaultValue(false)
        .build()
    );

    private final Setting<String> spamText = sgSpammer.add(new StringSetting.Builder()
        .name("spam-text")
        .description("Spam text")
        .defaultValue("Snail++ owns all!")
        .visible(spam::get)
        .build()
    );

    private final Setting<Boolean> onlyDm = sgSpammer.add(new BoolSetting.Builder()
        .name("only-dm")
        .description("Only spams in people's DMs")
        .defaultValue(false)
        .visible(spam::get)
        .build()
    );

    private final Setting<Double> delay = sgSpammer.add(new DoubleSetting.Builder()
        .name("delay")
        .description("Delay in seconds between each text message")
        .defaultValue(1.0)
        .min(0)
        .sliderMax(5)
        .visible(spam::get)
        .build()
    );

    private final Setting<Boolean> bypass = sgSpammer.add(new BoolSetting.Builder()
        .name("bypass")
        .description("Bypasses antispam plugins")
        .defaultValue(false)
        .visible(spam::get)
        .build()
    );

    private final Set<UUID> alertedPlayers = new HashSet<>();
    private long lastPlaceTime = 0;

    public ChatControl() {
        super(Addon.MISC, "Chat Control", "View way more chat stuff, very useful for anarchy servers");
    }

    @Override
    public void onActivate() {
        alertedPlayers.clear();
    }

    @Override
    public void onDeactivate() {
        alertedPlayers.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!visual.get() || alertedPlayers.size() >= maxAmount.get()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        int viewDistance = mc.options.getClampedViewDistance();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || alertedPlayers.contains(player.getUuid())) continue;

            if (checkUuid.get() && player.getUuidAsString().isEmpty()) continue;

            double distance = player.distanceTo(mc.player);
            if (distance <= viewDistance * 16) {
                ChatUtils.sendMsg(Text.of(Formatting.YELLOW + player.getName().getString() + " is within render distance."));
                mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, 1.0F));
                alertedPlayers.add(player.getUuid());
            }
        }
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (coordsProtection.get() && containsCoords(event.message)) {
            event.cancel();
            ChatUtils.sendMsg(Text.of(Formatting.RED + "Your message contains coordinates, and was not sent."));
            return;
        }

        if (prefix.get()) {
            event.message =  " " + event.message + prefixText.get() + " ";
        }
        if(green.get()) {
            event.message = ">" + event.message;
    }
    }

    @EventHandler
    private void onSpam(SendMessageEvent event) {
        if (spam.get()) {
            long time = System.currentTimeMillis();

            if ((time - lastPlaceTime) < delay.get() * 1000) return;
            lastPlaceTime = time;

            String[] bypassMessages = {
                " | JTHRYURTHURTYHRTYGHRT", 
                " | JHI5747845HUYRBE784", 
                " | 46RF5441||}{}{", 
                "URHIRHIREU4YHERIUYHRETUY I45RYUIH", 
                " | 049485785889U5YUI45TGUY45IG"
            };

            String message = spamText.get();
            
        
            Instant instant = Instant.now();
            long l = NetworkEncryptionUtils.SecureRandomUtil.nextLong();
            net.minecraft.client.network.ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
            LastSeenMessagesCollector.LastSeenMessages lastSeenMessages = ((ClientPlayNetworkHandlerAccessor) handler).getLastSeenMessagesCollector().collect();
            MessageSignatureData messageSignatureData = ((ClientPlayNetworkHandlerAccessor) handler).getMessagePacker().pack(new MessageBody(message, instant, l, lastSeenMessages.lastSeen()));
            handler.sendPacket(new ChatMessageC2SPacket(message, instant, l, messageSignatureData, lastSeenMessages.update()));
            
            event.message = message + (bypass.get() ? bypassMessages[(int)(Math.random() * bypassMessages.length)] : "");
        }
    }

    private boolean containsCoords(String message) {
        // A simple regex to detect coordinates (e.g., "123 64 -321")
        return message.matches(".*\\b-?\\d+\\s+-?\\d+\\s+-?\\d+\\b.*");
    }
}
