package org.Snail.Plus.modules.misc;

import org.Snail.Plus.Addon;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ChatControl extends Module {
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");
    private final SettingGroup sgChat = settings.createGroup("Chat");

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

    private final Setting<List<SoundEvent>> sounds = sgVisualRange.add(new SoundEventListSetting.Builder()
        .name("sounds")
        .description("Sounds to play when a player is spotted")
        .build());

    private final Setting<Integer> maxAmount = sgVisualRange.add(new IntSetting.Builder()
        .name("max-amount")
        .description("The cap of how many players the visual range notifies.")
        .defaultValue(3)
        .visible(visual::get)
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
        .description("The text to add as your prefix.")
        .defaultValue(" | snail++")
        .visible(prefix::get)
        .build());

    private final Setting<Boolean> green = sgChat.add(new BoolSetting.Builder()
        .name("green-text")
        .description("Adds a '>' to your text to make it green")
        .defaultValue(true)
        .build());

    private final Set<UUID> alertedPlayers = new HashSet<>();
    private final Set<UUID> playersInRange = new HashSet<>();

    public ChatControl() {
        super(Addon.Snail, "Chat Control+", "View way more chat stuff, useful for anarchy servers");
    }

    @Override
    public void onActivate() {
        alertedPlayers.clear();
        playersInRange.clear();
    }

    @Override
    public void onDeactivate() {
        alertedPlayers.clear();
        playersInRange.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            
            if (!visual.get() || alertedPlayers.size() >= maxAmount.get()) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null) return;

            int viewDistance = mc.options.getClampedViewDistance();
            Set<UUID> currentPlayersInRange = new HashSet<>();

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || alertedPlayers.contains(player.getUuid())) continue;
                if (checkUuid.get() && player.getUuidAsString().isEmpty()) continue;

                double distance = player.distanceTo(mc.player);
                if (distance <= viewDistance * 16) {
                    currentPlayersInRange.add(player.getUuid());
                    if (!playersInRange.contains(player.getUuid())) {
                        double targetX = Math.round(player.getX());
                        double targetY = Math.round(player.getY());
                        double targetZ = Math.round(player.getZ());

                        ChatUtils.sendMsg(Text.of(Formatting.GREEN + player.getName().getString() + " is within render distance. (" + targetX + ", " + targetY + ", " + targetZ + ")"));
                        List<SoundEvent> soundEvents = sounds.get();
                        if (!soundEvents.isEmpty()) {
                            mc.getSoundManager().play(PositionedSoundInstance.master(soundEvents.get(0), 1.0F));
                        }
                        alertedPlayers.add(player.getUuid());
                    }
                }
            }

            for (UUID playerUuid : playersInRange) {
                if (!currentPlayersInRange.contains(playerUuid)) {
                    ChatUtils.sendMsg(Text.of(Formatting.RED + "A player has left render distance."));
                }
            }

            playersInRange.clear();
            playersInRange.addAll(currentPlayersInRange);
        } catch (Exception e) {
            ChatUtils.sendMsg(Text.of(Formatting.RED + "Error in onTick: " + e.getMessage()));
        }
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            if (coordsProtection.get() && containsCoords(event.message)) {
                event.cancel();
                ChatUtils.sendMsg(Text.of(Formatting.RED + "Your message contains coordinates, and was not sent."));
                return;
            }

            if (prefix.get()) {
                event.message = event.message + prefixText.get();
            }

            if (green.get()) {
                event.message = ">" + event.message;
            }
        } catch (Exception e) {
            ChatUtils.sendMsg(Text.of(Formatting.RED + "Error in onMessageSend: " + e.getMessage()));
        }
    }

    private boolean containsCoords(String message) {
        return message.matches(".*\\b-?\\d+\\s+-?\\d+\\s+-?\\d+\\b.*");
    }
}
