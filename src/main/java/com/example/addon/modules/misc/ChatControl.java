package com.example.addon.modules.misc;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;



public class ChatControl extends Module {
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");
    private final SettingGroup sgPrefix = settings.createGroup("Prefix");
    
    private final Setting<Boolean> visual = sgVisualRange.add(new BoolSetting.Builder()
            .name("Visual Range")
            .description("Toggle visual range notification.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> checkUuid = sgVisualRange.add(new BoolSetting.Builder()
            .name("Check Uuid")
            .description("Toggle checking player UUIDs.")
            .defaultValue(true)
            .build()
    );
    
    private final Setting<Boolean> playSound = sgVisualRange.add(new BoolSetting.Builder()
            .name("Sound")
            .description("Plays a sound when you are near a player")
            .defaultValue(true)
            .build()
    );
    

    private final Map<UUID, String> playerNames = new HashMap<>();
    private final Set<UUID> alertedPlayers = new HashSet<>();
    private boolean running = false;
    private Thread thread;

    public ChatControl() {
        super(Addon.MISC, "Chat Control", "Better Chat");
    }

    @Override
    public void onActivate() {
        running = true;
        thread = new Thread(this::checkPlayers);
        thread.start();
    }

    @Override
    public void onDeactivate() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onDisconnect() {
        // Stop the thread and reset the player names map and alerted players set when disconnecting from the server
        running = false;
        playerNames.clear();
        alertedPlayers.clear();
    }

    private void checkPlayers() {
        while (running) {
            if (mc.world != null && mc.player != null) {
                double playerX = mc.player.getX();
                double playerY = mc.player.getY();
                double playerZ = mc.player.getZ();
                int renderDistance = mc.options.getViewDistance().getValue();

                populatePlayerNames(); // Populate player names map

                for (PlayerEntity target : mc.world.getPlayers()) {
                    if (target == mc.player) continue;

                    if (checkUuid.get() && target.getUuidAsString().isEmpty()) continue;

                    UUID uuid = target.getUuid();
                    String name = playerNames.get(uuid);

                    if (name == null) continue;

                    if (alertedPlayers.contains(uuid)) continue;

                    double targetX = target.getX();
                    double targetY = target.getY();
                    double targetZ = target.getZ();

                    double distance = Math.sqrt(Math.pow(targetX - playerX, 2) + Math.pow(targetY - playerY, 2) + Math.pow(targetZ - playerZ, 2));

                    if (distance <= renderDistance * 16) {
                        if (playSound.get()) {
                            mc.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, 1.0F));
                            ChatUtils.sendMsg(Text.of("A player is within render distance"));
                        }
                    }
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    }

