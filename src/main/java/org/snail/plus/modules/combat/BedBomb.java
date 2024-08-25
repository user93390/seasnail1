package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.tick.Tick;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;

import java.util.ArrayList;
import java.util.Objects;

public class BedBomb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    public final Setting<Boolean> DamageSync = sgDamage.add(new BoolSetting.Builder()
            .name("damage Sync")
            .description("only places when target can really take damage")
            .defaultValue(false)
            .build());
    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
            .name("break Delay")
            .defaultValue(3)
            .sliderMax(20)
            .sliderMin(0)
            .build());
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
            .name("place Delay")
            .defaultValue(3)
            .sliderMax(20)
            .sliderMin(0)
            .build());
    private final SettingGroup sgAntiCheat = settings.createGroup("anti-cheat");
    public final Setting<Boolean> airPlace = sgAntiCheat.add(new BoolSetting.Builder()
            .name("air-place")
            .description("air places blocks")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> RaytraceBypass = sgAntiCheat.add(new BoolSetting.Builder()
            .name("Raytrace Bypass")
            .description("tries bypassing Constantiam's raytrace checks")
            .defaultValue(false)
            .build());
    private final SettingGroup sgSmart = settings.createGroup("Smart");
    public final Setting<Boolean> AutoTrap = sgSmart.add(new BoolSetting.Builder()
            .name("Trap target")
            .description("enables auto-trap+ to prevent the target from running away")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> autoLay = sgSmart.add(new BoolSetting.Builder()
            .name("Auto lay")
            .description("makes the break delay higher so the target cannot stand up")
            .defaultValue(false)
            .build());
    public final Setting<Integer> layDelay = sgSmart.add(new IntSetting.Builder()
            .name("lay delay")
            .description("lay-down delay")
            .defaultValue(3)
            .sliderMax(10)
            .sliderMin(1)
            .visible(autoLay::get)
            .build());
    private final SettingGroup sgBreaker = settings.createGroup("String breaker");
    public final Setting<Boolean> StringBreaker = sgBreaker.add(new BoolSetting.Builder()
            .name("breaker")
            .description("bypasses anti-bed modules by breaking string, slabs and other")
            .defaultValue(true)
            .build());
    public final Setting<Boolean> SmartBreaker = sgBreaker.add(new BoolSetting.Builder()
            .name("smart breaker")
            .description("doesn't break unnecessary strings ")
            .defaultValue(true)
            .visible(StringBreaker::get)
            .build());
    private final Setting<Double> BreakerDelay = sgBreaker.add(new DoubleSetting.Builder()
            .name("break delay")
            .description("how many seconds to wait")
            .defaultValue(4.0)
            .visible(StringBreaker::get)
            .build());
    private final SettingGroup sgAutoCraft = settings.createGroup("Auto Craft");
    public final Setting<Boolean> AutoCraft = sgAutoCraft.add(new BoolSetting.Builder()
            .name("auto-craft")
            .description("crafts beds using wool and planks")
            .defaultValue(true)
            .build());
    private final Setting<Double> CraftDelay = sgAutoCraft.add(new DoubleSetting.Builder()
            .name("Auto-Craft delay")
            .description("delay to craft beds")
            .defaultValue(3.0)
            .sliderMax(36.0)
            .sliderMin(0.0)
            .visible(AutoCraft::get)
            .build());
    public final Setting<Boolean> shiftClick = sgAutoCraft.add(new BoolSetting.Builder()
            .name("shift click")
            .description("shift clicks the item to give you the max amount of items possible")
            .defaultValue(true)
            .visible(AutoCraft::get)
            .build());
    private final Setting<tablePlace> PlacePriority = sgAutoCraft.add(new EnumSetting.Builder<tablePlace>()
            .name("table Y priority")
            .description("the table priority")
            .defaultValue(tablePlace.current)
            .visible(AutoCraft::get)
            .build());
    public final Setting<Boolean> airPlaceTable = sgAutoCraft.add(new BoolSetting.Builder()
            .name("air-Place table")
            .description("allows you to air place crafting tables, may not work on all servers")
            .defaultValue(true)
            .visible(AutoCraft::get)
            .build());
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final Setting<Integer> RadiusZ = sgGeneral.add(new IntSetting.Builder()
            .name("Radius Z")
            .description("the radius for Z")
            .defaultValue(1)
            .sliderMax(5)
            .sliderMin(1)
            .build());
    private final Setting<Integer> RadiusX = sgGeneral.add(new IntSetting.Builder()
            .name("Radius X")
            .description("the radius for X")
            .defaultValue(1)
            .sliderMax(5)
            .sliderMin(1)
            .build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotation")
            .description("Rotates towards the block when placing.")
            .defaultValue(false)
            .build());
    private final Setting<SwapMode> swapMethod = sgGeneral.add(new EnumSetting.Builder<SwapMode>()
            .name("swap mode")
            .description("how to swap")
            .defaultValue(SwapMode.silent)
            .build());
    private final Setting<Boolean> strictDirection = sgAntiCheat.add(new BoolSetting.Builder()
            .name("strict direction")
            .description("Only places beds in the direction you are facing")
            .defaultValue(false)
            .build());
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("the target range")
            .defaultValue(3.0)
            .build());
    private final Setting<SafetyMode> safety = sgDamage.add(new EnumSetting.Builder<SafetyMode>()
            .name("safe-mode")
            .description("Safety mode")
            .defaultValue(SafetyMode.safe)
            .build());
    private final Setting<Double> maxSelfDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("max self damage score")
            .description("the max amount to deal to you")
            .defaultValue(3.0)
            .sliderMax(36.0)
            .sliderMin(0.0)
            .visible(() -> safety.get() != SafetyMode.off)
            .build());
    private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder()
            .name("min damage score")
            .description("the lowest amount of damage you should deal to the target (higher = less targets | lower = more targets)")
            .defaultValue(3.0)
            .sliderMax(36.0)
            .sliderMin(0.0)
            .visible(() -> safety.get() != SafetyMode.off)
            .build());
    private final Setting<Double> DamageRatio = sgDamage.add(new DoubleSetting.Builder()
            .name("damage ratio")
            .description("the ratio. min damage / maxself")
            .defaultValue(3.0)
            .sliderMax(36.0)
            .sliderMin(0.0)
            .visible(() -> safety.get() != SafetyMode.off)
            .visible(() -> safety.get() == SafetyMode.safe)
            .build());
    private final Setting<Boolean> MultiTask = sgMisc.add(new BoolSetting.Builder()
            .name("multi-task")
            .description("allows you to use different items when the module is interacting / placing")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> pauseUse = sgMisc.add(new BoolSetting.Builder()
            .name("pause on use")
            .description("pauses the module when you use a item")
            .defaultValue(false)
            .visible(() -> !MultiTask.get())
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    public BedBomb() {
        super(Addon.Snail, "bed Aura+", "Places beds and breaks beds around targets to deal massive damage");
    }
    private BlockPos[] BedPos = new BlockPos[0];
    private Thread BedTread;
    private volatile boolean isRunning = false;

    @Override
    public void onActivate() {
        BedPos = null;
        isRunning = true;
        BedTread = new Thread(() -> {
            while (isRunning) {
                ontick(null);
                try {
                    Thread.sleep(50); // Sleep for 50ms (equivalent to 20 ticks per second)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Bed Thread");
        BedTread.start();
        direction = CardinalDirection.North;
    }
    private CardinalDirection direction;
    @Override
    public void onDeactivate() {
        BedPos = null;
        isRunning = false;
        direction = CardinalDirection.North;
    }

    @EventHandler
    public void ontick(TickEvent.Post event) {
        if (Objects.requireNonNull(mc.world).getDimension().bedWorks()) {
            toggle();
            return;
        }
        if(pauseUse.get() && mc.player.isUsingItem()) return;
        for(PlayerEntity Player : mc.world.getPlayers()) {
            if (mc.player.distanceTo(Player) <= range.get()) {
                if (Player == mc.player || Friends.get().isFriend(Player)) continue;
                BedPos = positions(Player);
                double yaw = switch (direction) {
                    case East -> 90;
                    case South -> 180;
                    case West -> -90;
                    default -> 0;
                };

                for (BlockPos pos : BedPos) {
                    for (Direction dir : Direction.values()) {
                        if (strictDirection.get() && WorldUtils.strictDirection(pos.offset(dir), dir.getOpposite())) continue;
                    }
                    if(rotate.get()) {
                        Rotations.rotate(yaw, (Rotations.getPitch(pos)));
                    }
                    breakBed();
                }
            }
        }
    }
    public BlockPos[] positions(PlayerEntity entity) {
        ArrayList<BlockPos> posList = new ArrayList<>();
        int radiusSquared = RadiusX.get() * RadiusX.get(); // Calculate the square of the radius

        // Iterate through a square area around the target
        for (int x = -RadiusX.get(); x <= RadiusX.get(); x++) {
            for (int z = -RadiusZ.get(); z <= RadiusZ.get(); z++) {
                // Calculate the squared distance from the target
                int distanceSquared = x * x + z * z;

                // If the squared distance is within the radius squared, add the position
                if (distanceSquared <= radiusSquared) {
                    BlockPos pos = entity.getBlockPos().add(x, 0, z);
                    Rotations.rotate(Rotations.getYaw(pos), (Rotations.getPitch(pos)));
                    if (WorldUtils.isAir(pos) && !entity.getBoundingBox().intersects(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)) {
                        posList.add(pos);
                        return posList.toArray(new BlockPos[0]); // Return the first valid position
                    } else {
                        // If it's not air, try to find a valid position nearby
                        for (int yOffset = -1; yOffset <= 1; yOffset++) {
                            BlockPos nearbyPos = pos.add(0, yOffset, 0);
                            if (WorldUtils.isAir(nearbyPos)  && !entity.getBoundingBox().intersects(nearbyPos.getX(), nearbyPos.getY(), nearbyPos.getZ(), nearbyPos.getX() + 1, nearbyPos.getY() + 1, nearbyPos.getZ() + 1)) {
                                posList.add(nearbyPos);
                                return posList.toArray(new BlockPos[0]); // Return the first valid position
                            }
                        }
                    }
                }
            }
        }

        // Always place anchors above and below the target
        if (WorldUtils.isAir(entity.getBlockPos().up(2)) && !entity.getBoundingBox().intersects(entity.getBlockPos().up(2).getX(), entity.getBlockPos().up(2).getY(), entity.getBlockPos().up(2).getZ(), entity.getBlockPos().up(2).getX() + 1, entity.getBlockPos().up(2).getY() + 1, entity.getBlockPos().up(2).getZ() + 1)) {
            posList.add(entity.getBlockPos().up(2));
            return posList.toArray(new BlockPos[0]); // Return the first valid position
        }
        if (WorldUtils.isAir(entity.getBlockPos().up(2))  && !entity.getBoundingBox().intersects(entity.getBlockPos().down(1).getX(), entity.getBlockPos().down(1).getY(), entity.getBlockPos().down(1).getZ(), entity.getBlockPos().down(1).getX() + 1, entity.getBlockPos().down(1).getY() + 1, entity.getBlockPos().down(1).getZ() + 1)) {
            posList.add(entity.getBlockPos().down(1));
            return posList.toArray(new BlockPos[0]); // Return the first valid position
        }

        return posList.toArray(new BlockPos[0]); // If no valid positions are found, return an empty array
    }

    public synchronized void breakBed() {
        FindItemResult bed = InvUtils.findInHotbar(ItemStack -> ItemStack.getItem() instanceof BedItem);
        if (bed.found()) {
            for (BlockPos pos : BedPos) {
                InvUtils.swap(bed.slot(), true);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN, pos, false));

                if(!WorldUtils.isAir(pos)) {
                    mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.DOWN, pos, false));
                }
            }
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (BedPos != null) {
            for (BlockPos pos : BedPos) {
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();

                switch (direction) {
                    case North ->
                            event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    case South ->
                            event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    case East ->
                            event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    case West ->
                            event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }
        }
    }
    public enum SafetyMode {
        safe,
        balance,
        off,
    }

    public enum tablePlace {
        up,
        down,
        current,
    }

    public enum SwapMode {
        silent,
        normal,
        invSwitch,
    }
}