package org.snail.plus.managers;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
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
}