package org.snail.plus.modules.misc;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.screen.Screen;
import org.snail.plus.Addon;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class discordRPC extends Module {
    private static final RichPresence RPC = new RichPresence();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> message = sgGeneral.add(new StringListSetting.Builder()
            .name("message")
            .description("The message to display in discord")
            .defaultValue(List.of("Playing on {server} with {players} players"))
            .build());

    public discordRPC() {
        super(Addon.Snail, "Discord RPC", "Shows your discord status in game");
    }

    @Override
    public void onActivate() {
        DiscordIPC.start(1289704022231617659L, null);
        RPC.setStart(System.currentTimeMillis() / 1000L);
        updateDetails();
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
    }

    private void updateDetails() {
        for (String msg : message.get()) {
            msg = msg
                    .replace("{server}", Utils.getWorldName()
                    .replace(("{players}"), Arrays.toString(Objects.requireNonNull(mc.getServer()).getPlayerNames()).formatted("[" + Arrays.stream(mc.getServer().getPlayerNames()).count() + "]")));
            RPC.setDetails(msg);
            DiscordIPC.setActivity(RPC);
        }
    }
}