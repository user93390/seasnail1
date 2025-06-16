package dev.seasnail1.utilities;

import dev.seasnail1.managers.Managers;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class SwapUtils {
    public static int pickSlot = -1;
    public static MinecraftClient mc = MinecraftClient.getInstance();

    //credits to Blackout-client
    public static void pickSwitch(int slot) {
        if (!(slot >= 0)) return;

        Managers.swapMng.modifyStartTime = System.currentTimeMillis();
        pickSlot = slot;
        mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(slot));
    }

    public static void pickSwapBack() {
        if (!(pickSlot >= 0)) return;

        mc.getNetworkHandler().sendPacket(new PickFromInventoryC2SPacket(pickSlot));
        pickSlot = -1;
    }

    public static void moveSwitch(int from, int to) {
        ScreenHandler handler = mc.player.currentScreenHandler;
        Managers.swapMng.modifyStartTime = System.currentTimeMillis();

        ItemStack fromStack = handler.getSlot(from).getStack();
        ItemStack toStack = handler.getSlot(to).getStack();

        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(to, toStack);

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), from, to, SlotActionType.SWAP, fromStack, stack));
    }

    public enum swapMode {
        Inventory,
        Move,
        silent,
        normal,
        none
    }
}