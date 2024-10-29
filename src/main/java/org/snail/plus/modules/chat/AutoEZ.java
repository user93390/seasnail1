package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class AutoEZ extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> messageSetting = sgGeneral.add(new StringListSetting.Builder()
            .name("message")
            .description("Custom message to send.")
            .defaultValue("LMAO {victim} just died at {coords}! go get his shitty loot",
                    "looks like {victim} doesn't have snail++",
                    "I play fortnite duos with your mom {victim}")
            .build());

    private final Setting<Boolean> dm = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-dm")
            .description("Sends the message to the player.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> requireLastAttacker = sgGeneral.add(new BoolSetting.Builder()
            .name("require-last-attacker")
            .description("Requires the last attacker to be the player.")
            .defaultValue(true)
            .build());

    private int ezMsg;

    public AutoEZ() {
        super(Addon.Snail, "Auto EZ+", "Sends a custom message when a player dies");
    }

    @Override
    public void onActivate() {
        ezMsg = -1;
    }

    @Override
    public void onDeactivate() {
        ezMsg = -1;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (PlayerEntity player : Objects.requireNonNull(mc.world).getPlayers()) {
            if (player.isDead() && (!requireLastAttacker.get() || player.getLastAttacker() == mc.player)) {
                sendEzMessage(player);
            }
        }
    }

    private void sendEzMessage(PlayerEntity player) {
        List<String> messages = messageSetting.get();
        ezMsg = new Random().nextInt(messages.size());
        String msg = messages.get(ezMsg)
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