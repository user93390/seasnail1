package org.snail.plus.utils;

import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import org.snail.plus.managers.Managers;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class swapUtils {
    public static int pickSlot = -1;
    //credits to blackout client
    public static boolean pickSwitch(int slot) {
        if (slot >= 0) {
            Managers.swapMng.modifyStartTime = System.currentTimeMillis();
            pickSlot = slot;
            mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(slot));

            return true;
        }
        return false;
    }
    public static void pickSwapBack() {
        if (pickSlot >= 0) {
            mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(pickSlot));
            pickSlot = -1;
        }
    }
    public enum swapMode  {
        Inventory,
        silent,
        normal,
        none
    }
}
