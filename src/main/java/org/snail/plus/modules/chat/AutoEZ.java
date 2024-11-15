package org.snail.plus.modules.chat;

import java.util.List;
import java.util.Random;

import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;
import org.snail.plus.utils.WorldUtils;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

public class AutoEZ extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> messageSetting = sgGeneral.add(new StringListSetting.Builder()
            .name("message")
            .description("Custom message to send.")
            .defaultValue("")
            .build());

    private final Setting<Boolean> dm = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-dm")
            .description("Sends the message to the player.")
            .defaultValue(true)
            .build());

    private boolean sent = false;

    public AutoEZ() {
        super(Addon.Snail, "Auto EZ+", "Sends a message when a player dies");
    }

    @Override
    public void onActivate() {
        sent = false;
    }

    @Override
    public void onDeactivate() {
        sent = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (sent) {
            return;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (CombatUtils.getLastAttacker(player) == mc.player) {
                sendEzMessage(player);
                sent = true;
                break;
            }
        }
    }

    private void sendEzMessage(PlayerEntity player) {
        List<String> messages = messageSetting.get();
        String msg = messages.get(new Random().nextInt(messages.size()))
                .replace("{victim}", WorldUtils.getName(player))
                .replace("{coords}", WorldUtils.getCoords(player));
        sendMsg(msg, dm.get());
    }

    private void sendMsg(String msg, boolean dm) {
        if (dm) {
            ChatUtils.sendPlayerMsg("/msg " + WorldUtils.getName(mc.player) + " " + msg);
        } else {
            mc.player.networkHandler.sendChatMessage(msg);
        }
    }
}
