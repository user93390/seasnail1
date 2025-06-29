package dev.seasnail1.modules.combat;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.CombatUtils;
import dev.seasnail1.utilities.MathHelper;
import dev.seasnail1.utilities.WorldUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class AutoCrystal extends Module {
    // Settings
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Placement
    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder().name("place").defaultValue(true).build());
    private final Setting<Boolean> placeRotate = sgPlace.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder().name("place-range").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Boolean> support = sgPlace.add(new BoolSetting.Builder().name("support-block").defaultValue(true).build());
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder().name("place-delay").defaultValue(1).min(0).sliderMax(20).build());

    // Breaking
    private final Setting<Boolean> explode = sgBreak.add(new BoolSetting.Builder().name("explode").defaultValue(true).build());
    private final Setting<Boolean> breakRotate = sgBreak.add(new BoolSetting.Builder().name("rotate").defaultValue(true).build());
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder().name("break-range").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder().name("break-delay").defaultValue(1).min(0).sliderMax(20).build());

    // General / Targeting
    private final Setting<Integer> extrapolation = sgGeneral.add(new IntSetting.Builder().name("extrapolation").defaultValue(1).min(0).sliderMax(20).build());
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(12.5).min(0).sliderMax(16).build());
    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder().name("min-damage").defaultValue(6.5).min(0).sliderMax(36).build());
    private final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder().name("max-self-damage").defaultValue(12.5).min(0).sliderMax(36).build());

    // Render
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side color").defaultValue(new SettingColor(0, 255, 255, 50)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line color").defaultValue(new SettingColor(0, 255, 255, 255)).build());

    boolean needsSupport = false;
    boolean shouldPlace, shouldAttack;
    boolean exploded, placed;
    int id = -1;
    Map<BlockPos, DamageData> crystalData = new HashMap<>();
    Set<BlockPos> crystalPos = new HashSet<>();
    PlayerEntity[] entities = new PlayerEntity[0];
    BlockPos bestPos;
    PlayerEntity bestTarget;
    int explodeCooldown, placeCooldown;
    Box box = new Box(0, 0, 0, 0, 0, 0);

    Runnable reset = () -> {
        box = new Box(0, 0, 0, 0, 0, 0);
        explodeCooldown = 0;
        placeCooldown = 0;
        needsSupport = false;
        entities = new PlayerEntity[0];
        id = -1;
        bestTarget = null;
        bestPos = null;
        placed = false;
        exploded = false;
        crystalData.clear();
        shouldAttack = false;
        shouldPlace = false;
        crystalPos = new HashSet<>();
    };

    public AutoCrystal() {
        super(Addon.CATEGORY, "Auto-Crystal", "The most optimized AutoCrystal module ever.");
    }

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    @EventHandler(priority = 100)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        id = -1;
        exploded = false;

        bestTarget = CombatUtils.bestTarget(mc.world.getPlayers(), CombatUtils.filterMode.Closet, targetRange.get());
        if (bestTarget == null) return;

        if (crystalPos.isEmpty() && support.get()) {
            needsSupport = true;
            placeSupport();
        }

        Vec3d extrapolatedPos = bestTarget.getPos().add(bestTarget.getVelocity().multiply(extrapolation.get()));

        for (int x = (int) Math.floor(extrapolatedPos.getX()) - (int) Math.ceil(Math.sqrt(placeRange.get())); x <= (int) Math.floor(extrapolatedPos.getX()) + (int) Math.ceil(Math.sqrt(placeRange.get())); x++) {
            for (int z = (int) Math.floor(extrapolatedPos.getZ()) - (int) Math.ceil(Math.sqrt(placeRange.get())); z <= (int) Math.floor(extrapolatedPos.getZ()) + (int) Math.ceil(Math.sqrt(placeRange.get())); z++) {
                for (int y = (int) Math.floor(extrapolatedPos.getY()) - (int) Math.ceil(Math.sqrt(placeRange.get())); y <= (int) Math.floor(extrapolatedPos.getY()) + (int) Math.ceil(Math.sqrt(placeRange.get())); y++) {
                    BlockPos newPos = new BlockPos(x, y, z);

                    if (!valid(newPos)) continue;

                    crystalPos.add(newPos);
                }
            }
        }
    }

    private boolean valid(BlockPos pos) {
        double placeRangeSquared = placeRange.get() * placeRange.get();

        // Skip positions out of range
        if (mc.player.getBlockPos().getSquaredDistance(pos) > placeRangeSquared) return false;

        BlockPos basePos = pos.down();

        boolean isValid = (mc.world.getBlockState(basePos).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(basePos).getBlock() == Blocks.BEDROCK) && WorldUtils.replaceable(pos) && mc.player.getBlockPos().getSquaredDistance(pos) <= placeRangeSquared;

        if (!isValid) return false;

        int x, y, z;

        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();

        ((IBox) box).meteor$set(x, y, z, x + 1, y + 2, z + 1);

        // Check if the position intersects with entities
        if (intersectsWithEntities(box)) return false;

        // Skip positions that aren't valid or don't have support
        if (!WorldUtils.replaceable(pos)) return false;

        if (mc.world.getBlockState(basePos).getBlock() != Blocks.OBSIDIAN && mc.world.getBlockState(basePos).getBlock() != Blocks.BEDROCK) {
            return false;
        }

        // Calculate damage for valid positions
        float targetDamage = DamageUtils.crystalDamage(bestTarget, pos.toCenterPos());
        float selfDamage = DamageUtils.crystalDamage(mc.player, pos.toCenterPos());

        // Check damage thresholds
        DamageData data = new DamageData(targetDamage, selfDamage);
        if (data.okay()) {
            crystalData.put(pos, data);
        }

        // Set shouldPlace flag if we have valid positions and can place
        if (place.get() && placeCooldown <= 0) {
            placeCooldown = placeDelay.get();
            shouldPlace = true;
        }

        return true;
    }

    private boolean intersectsWithEntities(Box box) {
        return EntityUtils.intersectsWithEntity(box, entity -> !entity.isSpectator());
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        if (placeCooldown > 0) placeCooldown--;
        if (explodeCooldown > 0) explodeCooldown--;

        bestPos = crystalPos.stream()
                .max(Comparator.comparingDouble(p -> {
                    DamageData data = crystalData.get(p);
                    return data != null ? data.targetDamage : -1;
                }))
                .orElse(null);

        if (bestPos == null) return;

        if (shouldPlace && place.get()) {
            doPlace(bestPos);
            shouldPlace = false;
        }

        shouldAttack = true;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal) {
                if (shouldAttack && !exploded && explode.get()) {
                    if (mc.player.getPos().squaredDistanceTo(crystal.getPos()) > breakRange.get() * breakRange.get()) {
                        continue;
                    }
                    id = crystal.getId();
                    doExplode(id);

                    shouldAttack = false;
                    break;
                }
            }
        }

        explodeCooldown = breakDelay.get();
        placed = false;
    }

    private void placeSupport() {
        if (!support.get() || bestTarget == null) return;
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        Set<BlockPos> supportPositions = new HashSet<>(MathHelper.flatRadius(bestTarget.getBlockPos(), Math.sqrt(placeRange.get())));

        if (!supportPositions.isEmpty() && needsSupport) {
            //get highest damage support position
            BlockPos bestSupport = supportPositions.stream()
                    .max(Comparator.comparingDouble(pos -> {
                        DamageData data = crystalData.get(pos);
                        return data != null ? data.targetDamage : 0;
                    }))
                    .orElse(null);

            BlockUtils.place(bestSupport, obsidian, placeRotate.get(), 100, true);

            needsSupport = false;
        }
    }


    public void doPlace(BlockPos pos) {
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!crystal.found()) {
            error("No end crystal found in hotbar!");
            toggle();
            return;
        }

        pos = pos.down();

        BlockHitResult hitResult = new BlockHitResult(pos.toCenterPos(), mc.player.getHorizontalFacing(), pos, false);

        if (placeRotate.get()) {
            Vec3d vector = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            double yaw = Rotations.getYaw(vector);
            double pitch = Rotations.getPitch(vector);

            Rotations.rotate(yaw, pitch, 100);
        }

        if (mc.getNetworkHandler() != null) {
            InvUtils.swap(crystal.slot(), true);
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(crystal.getHand(), hitResult, 0));
            placed = true;
            InvUtils.swapBack();
        }
        placed = true;
    }

    public void doExplode(int id) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        Entity entity = mc.world.getEntityById(id);

        if (entity != null) {
            if (breakRotate.get()) {
                Vec3d vector = entity.getPos();
                double yaw = Rotations.getYaw(vector);
                double pitch = Rotations.getPitch(vector);

                Rotations.rotate(yaw, pitch, 100);
            }

            PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking());

            mc.getNetworkHandler().sendPacket(packet);
        }
        exploded = true;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (bestPos == null || crystalPos == null || mc.world == null || mc.player == null) return;

        if (mc.player.getBlockPos().getSquaredDistance(bestPos) > placeRange.get() * placeRange.get()) return;

        event.renderer.box(bestPos.down(), sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
    }

    @Override
    public String getInfoString() {
        if (bestTarget == null) return "";
        return bestTarget.getName().getString();
    }

    public class DamageData {
        private final float targetDamage;
        private final float selfDamage;

        public DamageData(float targetDamage, float selfDamage) {
            this.targetDamage = targetDamage;
            this.selfDamage = selfDamage;
        }

        public boolean okay() {
            boolean a = targetDamage >= minDamage.get();
            boolean b = selfDamage <= maxSelfDamage.get();

            return a && b;
        }
    }
}