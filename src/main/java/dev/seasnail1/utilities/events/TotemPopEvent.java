package dev.seasnail1.utilities.events;

import net.minecraft.entity.player.PlayerEntity;

public class TotemPopEvent {
    public short totems;
    public PlayerEntity player;

    public TotemPopEvent(short totems, PlayerEntity player) {
        this.totems = totems;
        this.player = player;
    }
}
