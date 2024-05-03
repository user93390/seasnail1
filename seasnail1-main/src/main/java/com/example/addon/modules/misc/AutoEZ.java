package com.example.addon.modules.misc;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import net.minecraft.text.Text;


public class AutoEZ extends Module {

    final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    final Setting<Boolean> showMessage = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-EZ")
        .description("Send a GG message when you kill someone")
        .defaultValue(true)
        .build()
    );

    final Setting<Boolean> showPlayerName = sgGeneral.add(new BoolSetting.Builder()
        .name("show-name")
        .description("Show the name of the player you killed")
        .defaultValue(true)
        .build()
    );
    
    final Setting<String> messageSetting = sgGeneral.add(new StringSetting.Builder()
        .name("message")
        .description("Custom message to send")
        .defaultValue("GG (player)")
        .build()
    );

    public AutoEZ() {
        super(Addon.MISC, "Auto Ez", "Say a message when you kill someone ");
    }

    @EventHandler
    private void onPlayerRemoved(EntityRemovedEvent event) {
        if (!showMessage.get() || !(event.entity instanceof PlayerEntity)) return;

        PlayerEntity removedPlayer = (PlayerEntity) event.entity;

        // Check if the removed player's last attacker is you
        Entity lastAttacker = removedPlayer.getRecentDamageSource().getAttacker();
        if (lastAttacker instanceof PlayerEntity && lastAttacker == MinecraftClient.getInstance().player) {
            String message = messageSetting.get().replace("(player)", removedPlayer.getName().getString());
            ChatUtils.sendMsg(Text.of(message));
        }
    }
}
