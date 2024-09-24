package org.snail.plus.modules.misc;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import org.snail.plus.Addon;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class discordRPC extends Module {
    private static final RichPresence RPC = new RichPresence();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> message = sgGeneral.add(new StringListSetting.Builder()
            .name("message")
            .description("the message")
            .defaultValue("playing on {server} with {players} players")
            .build());

    private final Setting<Boolean> randomMsg = sgGeneral.add(new BoolSetting.Builder()
            .name("random")
            .description("changes the message randomly")
            .defaultValue(true)
            .visible(() -> !message.get().isEmpty())
            .build());

    private String msg;

    public discordRPC() {
        super(Addon.Snail, "Discord RPC", "Shows your discord status in game");
    }

    @Override
    public void onActivate() {
        DiscordIPC.start(1282470171642167428L, null);
        RPC.setStart(System.currentTimeMillis() / 1000L);
        updateDetails();
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
    }

    private void updateDetails() {
        if (randomMsg.get()) {
            msg = message.get().getFirst()
                    .replace("{server}",  this.mc.isInSingleplayer() ? "Singleplayer" : Utils.getWorldName())
                    .replace("{players}", Objects.requireNonNull(mc.world).getPlayers().size() + "");
            RPC.setDetails(msg);
            DiscordIPC.setActivity(RPC);
        }
    }
}