 package com.example.addon.modules.misc;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChatControl extends Module {
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");

    private final Setting<Boolean> visual = sgVisualRange.add(new BoolSetting.Builder()
            .name("visual Range")
            .description("Toggle visual range notification.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> checkUuid = sgVisualRange.add(new BoolSetting.Builder()
            .name("checkUuid")
            .description("Toggle checking player UUIDs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> performanceMode = sgVisualRange.add(new BoolSetting.Builder()
    .name("Performance Mode")
    .description("Disable the module when in the game pause menu (GUI).")
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
        ChatUtils.sendMsg(Text.of(Formatting.GREEN + "Enabled Chatcontrol"));
    }

    @Override
    public void onDeactivate() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ChatUtils.sendMsg(Text.of(Formatting.RED + "Disabled Chatcontrol"));
    }

    public void onDisconnect() {
        // Stop the thread and reset the player names map and alerted players set when disconnecting from the server
        running = false;
        playerNames.clear();
        alertedPlayers.clear();
    }
    public void onDeath() {
        // Stop the thread and reset the player names map and alerted players set when you die
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
                    if (target == mc.player) 
                    continue;

                    if (checkUuid.get() && target.getUuidAsString().isEmpty()) continue;

                    UUID uuid = target.getUuid();
                    String name = playerNames.get(uuid);

                    if (name == null)
                     continue;

                    if (alertedPlayers.contains(uuid)) continue; // Skip if player has already been alerted

                    double targetX = target.getX();
                    double targetY = target.getY();
                    double targetZ = target.getZ();

                    double distance = Math.sqrt(Math.pow(targetX - playerX, 2) + Math.pow(targetY - playerY, 2) + Math.pow(targetZ - playerZ, 2));

                    if (distance <= renderDistance * 16) {
                        ChatUtils.sendMsg(Text.of(Formatting.BLUE + name + " is within render distance."));

                        // Play the specified sound when a player is spotted
                        mc.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, 1.0F));

                        alertedPlayers.add(uuid); // Add player to alerted set
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

    private void populatePlayerNames() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            playerNames.put(player.getUuid(), player.getName().getString());
        }
    }
}
