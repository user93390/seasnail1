package org.snail.plus.utils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class serverUtils {

    public static Integer currentPlayers() {
        return mc.world.getPlayers().size();
    }

    public static String serverIp() {
        return mc.getCurrentServerEntry().address;
    }
    public static int currentPing() {
        return mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
    }
}
