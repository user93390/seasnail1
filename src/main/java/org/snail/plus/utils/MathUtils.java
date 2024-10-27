package org.snail.plus.utils;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class MathUtils {

    public static List<BlockPos> getSphere(BlockPos pos, double radius) {
        List<BlockPos> sphere = new ArrayList<>();
        int cx = pos.getX();
        int cy = pos.getY();
        int cz = pos.getZ();
        int radiusInt = (int) Math.ceil(radius);

        for (int x = cx - radiusInt; x <= cx + radiusInt; x++) {
            for (int z = cz - radiusInt; z <= cz + radiusInt; z++) {
                for (int y = cy - radiusInt; y <= cy + radiusInt; y++) {
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (cy - y) * (cy - y);
                    if (dist < radius * radius) {
                        sphere.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return sphere;
    }

    public static double getRadius(int zx, int y) {
        return Math.sqrt(zx * zx + y * y);
    }
}
