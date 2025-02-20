package dev.seasnail1.utilities;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class MathHelper {
    static Map<Vec3d, Entity> movements = new HashMap<>();
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

    public static List<BlockPos> getHoles(List<BlockPos> sphere, double radius) {
        List<BlockPos> holes = new ArrayList<>();

        sphere.forEach(blockPos -> {
            for(Direction direction : Direction.values()) {
                if(WorldUtils.isAir(blockPos, false)) {
                    BlockPos offset = blockPos.offset(direction);

                    if(CombatUtils.isValidBlock(offset)) {
                        holes.add(blockPos);
                    }
                }
            }
        });

        return holes;
    }

    public static BlockPos getCrosshairBlock() {
        HitResult hitResult = mc.crosshairTarget;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hitResult).getBlockPos();
        }
        return null;
    }

    public static boolean rayCast(Vec3d blockPos) {
        HitResult hitResult = mc.world.raycast(new RaycastContext(new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()), blockPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        return hitResult.getType() != HitResult.Type.MISS;
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
