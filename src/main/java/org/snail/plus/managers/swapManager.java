package org.snail.plus.managers;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class swapManager {

    public int slot;
    public long modifyStartTime = 0;

    public swapManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
        slot = 0;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSend(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
            if (packet.getSelectedSlot() >= 0 && packet.getSelectedSlot() <= 8) {
                slot = packet.getSelectedSlot();
            }
        }
    }

    public ItemStack getStack() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return null;
        }
        return mc.player.getInventory().getStack(slot);
    }

    public int getSlot() {
        return slot;
    }

    public boolean isHolding(Item... items) {
        ItemStack stack = getStack();
        if (stack == null) {
            return false;
        }
        for (Item item : items) {
            if (item.equals(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    public boolean isHolding(Item item) {
        ItemStack stack = getStack();
        if (stack == null) {
            return false;
        }
        return stack.getItem().equals(item);
    }
}