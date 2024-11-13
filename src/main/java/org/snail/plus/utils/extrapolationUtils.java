package org.snail.plus.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class extrapolationUtils {

    public static Box predictEntityBox(PlayerEntity entity, int extrapolationTicks, boolean format) {
        double x = entity.getX() + (entity.getX() - entity.prevX) * extrapolationTicks;
        double y = entity.isOnGround() ? entity.getY() : entity.getY() + (entity.getY() - entity.prevY) * extrapolationTicks;
        double z = entity.getZ() + (entity.getZ() - entity.prevZ) * extrapolationTicks;

        Vec3d pos = new Vec3d(x, y, z);
        if (format) {
            return new Box(
                    pos.x - entity.getWidth() / 2,
                    pos.y,
                    pos.z - entity.getWidth() / 2,
                    pos.x + entity.getWidth() / 2,
                    pos.y + entity.getHeight(),
                    pos.z + entity.getWidth() / 2
            );
        } else {
            return new Box(pos, pos);
        }
    }

    public static Vec3d predictEntityVe3d(PlayerEntity entity, int extrapolationTicks) {
        double x = entity.getX() + (entity.getX() - entity.prevX) * extrapolationTicks;
        double y = entity.isOnGround() ? entity.getY() : entity.getY() + (entity.getY() - entity.prevY) * extrapolationTicks;
        double z = entity.getZ() + (entity.getZ() - entity.prevZ) * extrapolationTicks;

        return new Vec3d(x, y, z);
    }
}