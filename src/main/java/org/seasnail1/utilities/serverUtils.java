package org.seasnail1.utilities;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class serverUtils {

    public static int currentPlayers() {
        return mc.world.getPlayers().size();
    }

    public static String serverIp() {
        return mc.getCurrentServerEntry().address;
    }

    public static int currentPing() {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            var playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (playerListEntry != null) {
                return playerListEntry.getLatency();
            }
        }
        return 0;
    }

    public static float serverTps() {
        if (mc.world != null && mc.world.getServer() != null) {
            return mc.world.getServer().getTicks();
        }
        return 20;
    }
}
