package org.snail.plus.utilities;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class serverUtils {

    public static int currentPlayers() {
        return mc.world.getPlayers().size();
    }

    public static String serverIp() {
        return mc.getCurrentServerEntry().address;
    }

    public static int currentPing() {
        return mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
    }

    public static float serverTps() {
        return mc.world.getTickManager().getTickRate();
    }
}
