package org.seasnail1.utilities.events;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PlayerDeathEvent {
    public PlayerEntity player;
    public BlockPos pos;
    public boolean selfKilled;

    public PlayerDeathEvent(PlayerEntity player, BlockPos pos, boolean selfKilled) {
        this.player = player;
        this.pos = pos;
        this.selfKilled = selfKilled;
    }
}
