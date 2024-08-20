package org.snail.plus.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class AutoEZ extends Module {

    final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<List<String>> messageSetting = sgGeneral.add(new StringListSetting.Builder()
            .name("message")
            .description("Custom message to send.")
            .defaultValue("LMAO {victim} just died at {coords}! go get his shitty loot", "looks like {victim} doesn't have snail++", "I play fortnite duos with your mom {victim}")
            .build());

    private final Setting<Boolean> dm = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-dm")
            .description("Sends the message to the player.")
            .defaultValue(true)
            .build());

    private boolean messageSent = false;

    public AutoEZ() {
        super(Addon.Snail, "Auto EZ+", "Sends a custom message when a player dies");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (messageSent) return;

        for (PlayerEntity player : Objects.requireNonNull(mc.world).getPlayers()) {
            if (player != mc.player && !Friends.get().isFriend(player) && player.getHealth() <= 0) {
                List<String> messages = messageSetting.get();
                if (messages.isEmpty()) {
                    warning("you have no messages set for autoEZ+");
                    return;
                }

                int randomIndex = (int) (Math.random() * messages.size());
                String msg = messages.get(randomIndex)
                        .replace("{victim}", WorldUtils.getName(player))
                        .replace("{coords}", WorldUtils.getCoords(player));

                if (dm.get()) {
                    ChatUtils.sendPlayerMsg("/msg " + player.getName().getString() + " " + msg);
                } else {
                    ChatUtils.sendPlayerMsg(msg);
                }

                messageSent = true;
                return;
            }
        }
    }
}