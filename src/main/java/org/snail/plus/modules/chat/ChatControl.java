package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Formatting;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;

import java.util.*;
import java.util.List;

import static net.minecraft.text.Text.of;

public class ChatControl extends Module {

    public static final ChatControl instance = new ChatControl();

    private final SettingGroup sgClient = settings.createGroup("Client");
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");
    private final SettingGroup sgChat = settings.createGroup("Chat");


    public final Setting<SettingColor> color = sgClient.add(new ColorSetting.Builder()
            .name("color")
            .description("The color of the prefix.")
            .defaultValue(new SettingColor(76, 25, 76))
            .build());

    public final Setting<Boolean> improvedMsgs = sgClient.add(new BoolSetting.Builder()
            .name("improved-msgs")
            .description("Improves the look of chat messages.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> visual = sgVisualRange.add(new BoolSetting.Builder()
            .name("visual-range")
            .description("Toggle visual range notification.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> checkUuid = sgVisualRange.add(new BoolSetting.Builder()
            .name("check-uuid")
            .description("Toggle checking player UUIDs.")
            .defaultValue(true)
            .visible(visual::get)
            .build());

    private final Setting<Integer> maxAmount = sgVisualRange.add(new IntSetting.Builder()
            .name("max-amount")
            .description("The cap of how many players the visual range notifies.")
            .defaultValue(3)
            .visible(visual::get)
            .build());

    private final Setting<List<SoundEvent>> sounds = sgVisualRange.add(new SoundEventListSetting.Builder()
            .name("sounds")
            .description("Sounds to play when a player is spotted")
            .build());

    private final Setting<Boolean> coordsProtection = sgChat.add(new BoolSetting.Builder()
            .name("coords protection")
            .description("Prevents you from sending messages in chat that may contain coordinates.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> prefix = sgChat.add(new BoolSetting.Builder()
            .name("prefix")
            .description("Adds a prefix to your chat messages.")
            .defaultValue(true)
            .build());

    private final Setting<String> prefixText = sgChat.add(new StringSetting.Builder()
            .name("prefix-text")
            .description("The text to add as your prefix when you type in chat.")
            .defaultValue("| snail++")
            .visible(prefix::get)
            .build());

    private final Setting<Boolean> green = sgChat.add(new BoolSetting.Builder()
            .name("green-text")
            .description("Adds a '>' to your text to make it green")
            .defaultValue(true)
            .build());


    private final Set<UUID> playersInRange = new HashSet<>();
    private final Set<UUID> alertedPlayers = new HashSet<>();
    public ChatControl() {
        super(Addon.Snail, "Chat Control", "Custom prefix and visual range :)");
    }
    private final int viewDistance = mc.options.getClampedViewDistance();

    public static SettingColor getColor() {
        return instance.color.get();
    }

    @Override
    public void onDeactivate() {
        playersInRange.clear();
        alertedPlayers.clear();
    }

    @Override
    public void onActivate() {
        playersInRange.clear();
        alertedPlayers.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            if (!visual.get() || alertedPlayers.size() >= maxAmount.get()) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            for (PlayerEntity player : Objects.requireNonNull(mc.world).getPlayers()) {
                if (player == mc.player || alertedPlayers.contains(player.getUuid())) continue;
                if (checkUuid.get() && player.getUuidAsString().isEmpty()) continue;
                double distance = player.distanceTo(mc.player);
                if (distance <= viewDistance * 16) {
                    if (!playersInRange.contains(player.getUuid())) {

                        double targetX = Math.round(player.getX());
                        double targetY = Math.round(player.getY());
                        double targetZ = Math.round(player.getZ());

                        ChatUtils.sendMsg(of(Formatting.RED + player.getName().getString() + " is within render distance. (" + targetX + ", " + targetY + ", " + targetZ + ")"));
                        List<SoundEvent> soundEvents = sounds.get();
                        if (!soundEvents.isEmpty()) {
                            mc.getSoundManager().play(WorldUtils.playSound(soundEvents.get(0), 1.0f));
                        }
                        alertedPlayers.add(player.getUuid());
                    }
                }
            }
        } catch (Exception e) {
            ChatUtils.sendMsg(of(Formatting.RED + "Error in onTick: " + e.getMessage()));
        }
    }

    @EventHandler
    private void onPlayerRemove(PlayerRemoveS2CPacket event) {
        for (PlayerEntity player : Objects.requireNonNull(mc.world).getPlayers()) {
            if (player == mc.player || alertedPlayers.contains(player.getUuid())) continue;
            if (checkUuid.get() && player.getUuidAsString().isEmpty()) continue;
            ChatUtils.sendMsg(of(Formatting.GREEN + player.getName().getString() + " has left render distance"));
            List<SoundEvent> soundEvents = sounds.get();
            if (!soundEvents.isEmpty()) {
                mc.getSoundManager().play(WorldUtils.playSound(soundEvents.get(0), 1.0f));
            }
            alertedPlayers.add(player.getUuid());
            playersInRange.clear();
        }
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        try {
            if (coordsProtection.get() && containsCoords(event.message)) {
                event.cancel();
                ChatUtils.sendMsg(of(Formatting.RED + "Your message contains coordinates and was not sent."));
                return;
            }

            if (prefix.get()) {
                event.message = event.message + " " + prefixText.get();
            }

            if (green.get()) {
                event.message = ">" + event.message;
            }
        } catch (Exception e) {
            ChatUtils.sendMsg(of(Formatting.RED + "Error in onMessageSend: " + e.getMessage()));
        }
    }

    private boolean containsCoords(String message) {
        return message.matches(".*\\b-?\\d+\\s+-?\\d+\\s+-?\\d+\\b.*");
    }
}