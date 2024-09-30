package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Formatting;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;

import java.util.*;

import static net.minecraft.text.Text.of;

public class ChatControl extends Module {
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");
    private final SettingGroup sgChat = settings.createGroup("Chat");
    private final SettingGroup sgFilter = settings.createGroup("Filter");

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
    private final Setting<Boolean> filter = sgFilter.add(new BoolSetting.Builder()
            .name("filter")
            .description("Filters out messages that contain coordinates.")
            .defaultValue(true)
            .build());
    private final Setting<List<String>> messages = sgFilter.add(new StringListSetting.Builder()
            .name("messages")
            .description("Messages to filter out.")
            .defaultValue("discord./gg/", "https://")
            .visible(filter::get)
            .build());
    private final Setting<Boolean> block = sgFilter.add(new BoolSetting.Builder()
            .name("block player")
            .description("ignores the player if they send the message x amount of times")
            .defaultValue(true)
            .visible(filter::get)
            .build());
    private final Setting<Boolean> info = sgFilter.add(new BoolSetting.Builder()
            .name("send info")
            .description("sends info to chat when a player is blocked")
            .defaultValue(true)
            .visible(filter::get)
            .build());
    private final Setting<Integer> blockAmount = sgFilter.add(new IntSetting.Builder()
            .name("block amount")
            .description("amount of messages to block the player")
            .defaultValue(3)
            .visible(filter::get)
            .visible(block::get)
            .build());
    private Map<UUID, Integer> warnedAmount = new HashMap<>();
    private final Set<UUID> alertedPlayers = new HashSet<>();
    private final Set<UUID> playersInRange = new HashSet<>();
    private final Set<UUID> currentPlayersInRange = new HashSet<>();
    private final Set<UUID> playersWarned = new HashSet<>();

    public ChatControl() {
        super(Addon.Snail, "Chat Control", "Custom prefix and visual range :)");
    }
    private final int viewDistance = mc.options.getClampedViewDistance();

    @Override
    public void onDeactivate() {
        alertedPlayers.clear();
        playersInRange.clear();
        playersWarned.clear();
        warnedAmount.clear();
    }

    @Override
    public void onActivate() {
        alertedPlayers.clear();
        playersInRange.clear();
        playersWarned.clear();
        warnedAmount.clear();
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
                    currentPlayersInRange.add(player.getUuid());
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
    private void onMessageReceive(ReceiveMessageEvent event) {
        try {
            if (filter.get() && !messages.get().isEmpty()) {
                for (String message : messages.get()) {
                    if (event.getMessage().getString().contains(message)) {
                        mc.inGameHud.getChatHud().removeMessage(new MessageSignatureData(event.getMessage().getString().getBytes()));
                        if (info.get()) {
                            ChatUtils.sendMsg(of(Formatting.RED + "Message filtered: " + event.getMessage().getString()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            ChatUtils.sendMsg(of(Formatting.RED + "Error in onMessageReceive: " + e.getMessage()));
        }
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        try {

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

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