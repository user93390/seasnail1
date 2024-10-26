package org.snail.plus.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class extrapolationUtils {

    public static Vec3d predictEntityPos(PlayerEntity entity, Integer extrapolationTicks) {
        if (entity.prevX != entity.getX() && entity.prevY != entity.getY() && entity.prevZ != entity.getZ()) {
            double x = entity.getX() + (entity.getX() - entity.prevX) * extrapolationTicks;
            double y = entity.getY() + (entity.getY() - entity.prevY) * extrapolationTicks;
            double z = entity.getZ() + (entity.getZ() - entity.prevZ) * extrapolationTicks;
            return new Vec3d(x, y, z);
        }
        return entity.getPos();
    }
}