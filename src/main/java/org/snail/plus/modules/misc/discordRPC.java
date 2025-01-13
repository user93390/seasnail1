package org.snail.plus.modules.misc;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import org.snail.plus.Addon;
import org.snail.plus.utilities.serverUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class discordRPC extends Module {
    private static final RichPresence RPC = new RichPresence();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> message = sgGeneral.add(new StringListSetting.Builder()
            .name("message")
            .description("The message to display in discord")
            .defaultValue(List.of("Playing on {server} with {players} players"))
            .build());

    private final Setting<Integer> updateInterval = sgGeneral.add(new IntSetting.Builder()
            .name("update-interval")
            .description("The interval in seconds to update the details.")
            .defaultValue(10)
            .sliderRange(1, 10000)
            .build());

    private final Setting<Integer> randomDelay = sgGeneral.add(new IntSetting.Builder()
            .name("random-delay")
            .description("The random delay in seconds to adjust the message display time.")
            .defaultValue(5)
            .sliderRange(1, 10000)
            .build());

    private ScheduledExecutorService scheduler;
    private final Random random = new Random();

    public discordRPC() {
        super(Addon.Snail, "Discord RPC", "Shows your discord status in game");
    }

    @Override
    public void onActivate() {
        DiscordIPC.start(1289704022231617659L, null);
        RPC.setStart(System.currentTimeMillis() / 1000L);
        scheduler = Executors.newScheduledThreadPool(1);
        scheduleNextUpdate();
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private void scheduleNextUpdate() {
        int delay = updateInterval.get() + random.nextInt(randomDelay.get());
        scheduler.schedule(this::updateDetails, delay, TimeUnit.MILLISECONDS);
    }

    private void updateDetails() {
        try {
            List<String> messages = message.get();
            String msg = messages.size() > 1 ? messages.get(random.nextInt(messages.size())) : messages.getFirst();
            msg = msg
                    .replace("{server}", mc.isInSingleplayer() ? Utils.getWorldName() : serverUtils.serverIp())
                    .replace("{players}", String.valueOf(serverUtils.currentPlayers()))
                    .replace("{fps}", String.valueOf(mc.getCurrentFps())
                            .replace("{ping}", String.valueOf(serverUtils.currentPing())));
            RPC.setDetails(msg);
            DiscordIPC.setActivity(RPC);
            scheduleNextUpdate();
        } catch (Exception e) {
            error("An error occurred while updating discord rpc");
            Addon.LOGGER.error("An error occurred while updating discord rpc {}", Arrays.toString(e.getStackTrace()));
        }
    }
}