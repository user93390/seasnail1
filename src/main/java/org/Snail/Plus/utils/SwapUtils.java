package org.Snail.Plus.utils;

import meteordevelopment.meteorclient.utils.player.InvUtils;


public class SwapUtils {
    private static long lastTimeoutCheck = 0;

    public static void SilentSwap(int ItemSlot, float Delay) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastTimeoutCheck) < Delay * 1000) return;
        lastTimeoutCheck = currentTime;

        InvUtils.swap(ItemSlot, true);

        InvUtils.swapBack();

    }

    public static void invSwitch(int From, int To, Boolean repeat, float Delay) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastTimeoutCheck) < Delay * 1000) return;
        lastTimeoutCheck = currentTime;

        if (repeat) {
            InvUtils.quickSwap().from(From).toId(To);
            InvUtils.quickSwap().from(To).to(From);
        } else {
            InvUtils.quickSwap().from(From).toId(To);
        }
    }

    public static void Normal(int Slot, float Delay) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastTimeoutCheck) < Delay * 1000) return;
        lastTimeoutCheck = currentTime;

        InvUtils.swap(Slot, false);
        InvUtils.swapBack();
    }
}
