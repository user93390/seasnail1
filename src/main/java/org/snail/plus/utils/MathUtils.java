package org.snail.plus.utils;


import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MathUtils {

    private static float currentYaw;
    private static float currentPitch;
    private static float targetYaw;
    private static float targetPitch;
    private static int currentStep;

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

    public static void rayCast(BlockPos blockPos, int rotationSteps) {
        Vec3d playerPos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        Vec3d blockTopCenter = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 1, blockPos.getZ() + 0.5);

        double diffX = blockTopCenter.x - playerPos.x;
        double diffY = blockTopCenter.y - playerPos.y;
        double diffZ = blockTopCenter.z - playerPos.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        if (mc.world.raycast(new RaycastContext(playerPos, blockTopCenter, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player)).getType() != HitResult.Type.MISS) {
            if (WorldUtils.isAir(mc.player.getBlockPos().up(2))) {
                pitch = -90;
                Rotations.rotate(yaw, pitch);
                updateRotation(rotationSteps);
            }
        } else {
            Rotations.rotate(yaw, pitch);
            updateRotation(rotationSteps);
        }
    }

    public static void updateRotation(int steps) {
        if (currentStep < steps) {
            float interpolatedYaw = currentYaw + (targetYaw - currentYaw) * (currentStep / (float) steps);
            float interpolatedPitch = currentPitch + (targetPitch - currentPitch) * (currentStep / (float) steps);
            mc.player.setYaw(interpolatedYaw);
            mc.player.setPitch(interpolatedPitch);
            currentStep++;
        }
    }
}