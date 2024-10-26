package org.snail.plus.utils;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class MathUtils {

    public static List<BlockPos> getSphere(BlockPos pos, int radiusXZ, int radiusY) {
        List<BlockPos> sphere = new ArrayList<>();
        int cx = pos.getX();
        int cy = pos.getY();
        int cz = pos.getZ();
        for (int x = cx - radiusXZ; x <= cx + radiusXZ; x++) {
            for (int z = cz - radiusXZ; z <= cz + radiusXZ; z++) {
                for (int y = cy - radiusY; y <= cy + radiusY; y++) {
                    double distXZ = (cx - x) * (cx - x) + (cz - z) * (cz - z);
                    double distY = (cy - y) * (cy - y);
                    if (distXZ < radiusXZ * radiusXZ && distY < radiusY * radiusY) {
                        sphere.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return sphere;
    }
}
