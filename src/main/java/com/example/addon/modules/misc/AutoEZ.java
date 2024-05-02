package com.example.addon.modules.misc;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.MinecraftClient;

public class AutoEZ extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final String prefix = Formatting.DARK_RED + "[snail++]";

    private final Setting<String> messageSetting = sgGeneral.add(new StringSetting.Builder()
            .name("message")
            .description("Custom message to send")
            .defaultValue("GG (player)")
            .build()
    );

    private final Setting<Boolean> autoDM = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-DM")
            .description("Send the message in a DM")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> globalChat = sgGeneral.add(new BoolSetting.Builder()
            .name("global-chat")
            .description("Send the message in the server chat")
            .defaultValue(true)
            .build()
    );

    public AutoEZ() {
        super(Addon.MISC, "AutoEZ", "Say a message when you kill someone");
    }

    @EventHandler
    private void onPlayerRemoved(EntityRemovedEvent event) {
        if (!(event.entity instanceof PlayerEntity)) return;

        PlayerEntity removedPlayer = (PlayerEntity) event.entity;

        // Check if the removed player's last attacker is you
        if (removedPlayer.getRecentDamageSource() != null && removedPlayer.getRecentDamageSource().getAttacker() instanceof PlayerEntity) {
            Entity attacker = removedPlayer.getRecentDamageSource().getAttacker();
            if (attacker == MinecraftClient.getInstance().player) {
                String playerName = removedPlayer.getName().getString();
                String message = messageSetting.get().replace("(player)", playerName);

                if (autoDM.get()) {
                    ChatUtils.sendPlayerMsg("/msg " + playerName + " " + message);
                }

                if (globalChat.get()) {
                    ChatUtils.sendMsg(Text.of(playerName + " " + message));
                }
            }
        }
    }

    @Override
    public void onActivate() {
        ChatUtils.sendMsg(Text.of(Formatting.GREEN + "Enabled AutoEZ"));
    }

    @Override
    public void onDeactivate() {
        ChatUtils.sendMsg(Text.of(Formatting.RED + "Disabled AutoEZ"));
    }

    public void sendToggledMsg() {
        ChatUtils.forceNextPrefixClass(getClass());
        String msg = prefix + " " + Formatting.WHITE + name + (isActive() ? Formatting.GREEN + " ON" : Formatting.RED + " OFF");
        ChatUtils.sendMsg(Text.of(msg));
    }
}
