package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
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
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.snail.plus.Addon;

import java.util.Objects;

public class BedBomb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    public final Setting<Boolean> DamageSync = sgDamage.add(new BoolSetting.Builder()
            .name("damage Sync")
            .description("only places when target can really take damage, super useful on constantiam or any strict server")
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
            .description("tries bypassing constantiam's raytrace checks; won't work all the time")
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
            .defaultValue(2)
            .sliderMax(5)
            .sliderMin(-5)
            .build());
    private final Setting<Integer> RadiusX = sgGeneral.add(new IntSetting.Builder()
            .name("Radius X")
            .description("the radius for X")
            .defaultValue(2)
            .sliderMax(5)
            .sliderMin(-5)
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
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
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
    private final long lastLaydown = 0;
    PlayerEntity target;
    BlockPos BedPos;
    CardinalDirection direction;
    private int layTimer;
    private int placeTimer;
    private int breakTimer;
    private long lastLaydownTime = System.currentTimeMillis();
    private long lastPlaceTime = System.currentTimeMillis();
    private long lastBreakTime = System.currentTimeMillis();


    public BedBomb() {
        super(Addon.Snail, "Bed Bomb+", "Places beds and breaks beds around targets to deal massive damage");
    }

    @Override
    public void onActivate() {
        target = null;
        BedPos = null;
        direction = CardinalDirection.North;
        layTimer = layDelay.get();
        placeTimer = placeDelay.get();
        breakTimer = breakDelay.get();

    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (TargetUtils.isBadTarget(target, range.get())) return;
        placeBed(bed, findPosition(target));
        if (canPlaceBed()) {
            placeBed(bed, findPosition(target));
            updatePlaceTime();
        }

        if (BedPos != null && canBreakBed()) {
            breakBed(findPosition(target));
            updateBreakTime();
        }

        if (autoLay.get() && canLayDown()) {

            updateLaydownTime();
        }

        if (AutoTrap.get()) {
            trap();
        }
    }


    public void placeBed(FindItemResult bed, BlockPos BedPos) {
        double yaw = switch (direction) {
            case East -> 90;
            case South -> 180;
            case West -> -90;
            default -> 0;
        };
        if (BedPos != null) {
            Rotations.rotate(yaw, Rotations.getPitch(BedPos), () -> BlockUtils.place(BedPos, bed, rotate.get(), 100));
            if (StringBreaker.get()) {
            breakString();
            }
        }
    }
    /*
    very bad way of making timers, I may improve it at some point in time.
     */
    private boolean canPlaceBed() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastPlaceTime) >= placeDelay.get() * 50;
    }

    private void updatePlaceTime() {
        lastPlaceTime = System.currentTimeMillis();
    }

    private boolean canBreakBed() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastBreakTime) >= breakDelay.get() * 50;
    }

    private void updateBreakTime() {
        lastBreakTime = System.currentTimeMillis();
    }

    private boolean canLayDown() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastLaydownTime) >= layDelay.get() * 50;
    }

    private void updateLaydownTime() {
        lastLaydownTime = System.currentTimeMillis();
    }


    public void trap() {
    }

    public void breakBed(BlockPos BedPos) {
        rayTraceBypass();
        Objects.requireNonNull(mc.interactionManager).interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(BedPos), Direction.UP, BedPos, false));
    }

    public void rayTraceBypass() {
        if (RaytraceBypass.get()) {
            Rotations.rotate(mc.player.getYaw(), -90); //test
        }
    }
    public BlockPos findPosition(PlayerEntity playerEntity) {
        double X = playerEntity.getX() - RadiusX.get();
        double Y = playerEntity.getY();
        if (airPlace.get()) Y = playerEntity.getY() + 1;
        double Z = playerEntity.getZ() - RadiusZ.get();

        return BedPos = new BlockPos(new Vec3i((int) X, (int) Y, (int) Z));
    }

    public void breakString() {
        Boolean webbed = false;
        Boolean doubled = false;
        BlockState blockState = Objects.requireNonNull(mc.world).getBlockState(target.getBlockPos());
        FindItemResult bestSlot = InvUtils.findFastestTool(blockState);
        if (mc.world.getBlockState(target.getBlockPos()).getBlock() == Block.getBlockFromItem(Items.STRING) && !SmartBreaker.get()) {
            webbed = true;
        } else if (mc.world.getBlockState(target.getBlockPos()).getBlock() == Block.getBlockFromItem(Items.STRING) && mc.world.getBlockState(target.getBlockPos().up(1)).getBlock() == Block.getBlockFromItem(Items.STRING) && SmartBreaker.get()) {
            doubled = true;
        }

        if (doubled) {
            InvUtils.swap(bestSlot.slot(), true);
            BlockUtils.breakBlock(target.getBlockPos().up(1), true);
        }
        if (webbed) {
            InvUtils.swap(bestSlot.slot(), true);
            BlockUtils.breakBlock(target.getBlockPos(), true);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (BedPos != null) {
            int x = BedPos.getX();
            int y = BedPos.getY();
            int z = BedPos.getZ();

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

    @Override
    public void onDeactivate() {
        target = null;
        BedPos = null;
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