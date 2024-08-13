package org.snail.plus.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import static meteordevelopment.meteorclient.MeteorClient.mc;


public class SwapUtils {
    private static long lastTimeoutCheck = 0;

    public static void SilentSwap(int ItemSlot, Double Delay) {

        InvUtils.swap(ItemSlot, true);

        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastTimeoutCheck) < Delay * 1000) return;
        lastTimeoutCheck = currentTime;

        InvUtils.swapBack();

    }

    public static void Normal(int Slot, float Delay) {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastTimeoutCheck) < Delay * 1000) return;
        lastTimeoutCheck = currentTime;

        InvUtils.swap(Slot, false);
        InvUtils.swapBack();
    }
}
