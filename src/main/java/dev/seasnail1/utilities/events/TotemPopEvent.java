package dev.seasnail1.utilities.events;

import net.minecraft.entity.player.PlayerEntity;

public class TotemPopEvent {
    public int totems;
    public PlayerEntity player;

    public TotemPopEvent(int totems, PlayerEntity player) {
        this.totems = totems;
        this.player = player;
    }
}
