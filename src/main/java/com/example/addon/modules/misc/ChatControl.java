package com.example.addon.modules.misc;

import com.example.addon.modules.misc.Notifications;
import com.example.addon.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.MinecraftClient;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;

public class ChatControl extends Module {
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");
    private final SettingGroup sgChat = settings.createGroup("Chat"); // Corrected typo here
    
    private final Setting<Boolean> visual = sgVisualRange.add(new BoolSetting.Builder()
            .name("Visual Range")
            .description("Toggle visual range notification.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> checkUuid = sgVisualRange.add(new BoolSetting.Builder()
            .name("Check UUID")
            .description("Toggle checking player UUIDs.")
            .defaultValue(true)
            .visible(visual::get)
            .build()
    );

    private final Setting<Integer> maxAmount = sgVisualRange.add(new IntSetting.Builder()
        .name("Max Amount")
        .description("The cap of how many players the visual range notifies.")
        .defaultValue(3)
        .visible(visual::get)
        .build()
    );
    
    private final Set<UUID> alertedPlayers = new HashSet<>();

    private Field viewDistanceField;

    public ChatControl() {
        super(Addon.MISC, "Chat Control", "View way more chat stuff, very useful for anarchy servers");

        try {
            viewDistanceField = MinecraftClient.getInstance().options.getClass().getDeclaredField("viewDistance");
            viewDistanceField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public void onDisconnect() {
        alertedPlayers.clear();
    }

    public void onDeath() {
        alertedPlayers.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            if (!visual.get() || alertedPlayers.size() >= maxAmount.get()) return;

            int viewDistance = MinecraftClient.getInstance().options.getViewDistance().getValue();
            for (PlayerEntity player : MinecraftClient.getInstance().world.getPlayers()) {
                if (player == MinecraftClient.getInstance().player || alertedPlayers.contains(player.getUuid())) continue;

                if (checkUuid.get() && player.getUuidAsString().isEmpty()) continue;

                double distance = player.distanceTo(MinecraftClient.getInstance().player);

                if (distance <= viewDistance * 16) {
                    ChatUtils.sendMsg(Text.of(Formatting.YELLOW + player.getName().getString() + " is within render distance."));

                    // Play the specified sound when a player is spotted
                    MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, 1.0F));

                    alertedPlayers.add(player.getUuid());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            error("An error occurred while processing Chat Control module: " + e.getMessage() + "report this error to seasnail1 or chronosuser");
        }
    }
}

//MinecraftClient.getInstance().inGameHud.getChatHud().clear(true);