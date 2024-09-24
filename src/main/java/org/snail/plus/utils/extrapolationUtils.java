package org.snail.plus.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public  class extrapolationUtils {

    public static Box predictEntityBox(PlayerEntity entity, Box box, Integer extrapolationTicks) {
        double x = entity.getX() + (entity.getX() - entity.prevX) * extrapolationTicks;
        double y = entity.getY() + (entity.getY() - entity.prevY) * extrapolationTicks;
        double z = entity.getZ() + (entity.getZ() - entity.prevZ) * extrapolationTicks;
        return box.offset(x, y, z);
    }

    public static Vec3d predictEntityPos(PlayerEntity entity, Integer extrapolationTicks) {
        double x = entity.getX() + (entity.getX() - entity.prevX) * extrapolationTicks;
        double y = entity.getY() + (entity.getY() - entity.prevY) * extrapolationTicks;
        double z = entity.getZ() + (entity.getZ() - entity.prevZ) * extrapolationTicks;
        return new Vec3d(x, y, z);
    }
}
