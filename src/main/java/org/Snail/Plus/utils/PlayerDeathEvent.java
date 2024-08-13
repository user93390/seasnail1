package org.snail.plus.utils;

import meteordevelopment.orbit.EventBus;
import net.minecraft.entity.player.PlayerEntity;

public class PlayerDeathEvent {
    private static final PlayerDeathEvent INSTANCE = new PlayerDeathEvent();
    private PlayerEntity player;

    public static PlayerDeathEvent get(PlayerEntity player) {
        INSTANCE.player = player;
        return INSTANCE;
    }

    public PlayerEntity getPlayer() {
        return this.player;
    }
}