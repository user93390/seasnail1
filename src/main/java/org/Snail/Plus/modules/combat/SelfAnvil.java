package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;
import org.snail.plus.utils.WorldUtils;

import java.util.Objects;

public class SelfAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private long lastPlaceTime = 0;
    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
            .name("support")
            .description("Places support blocks (recommended)")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder()
            .name("auto center")
            .description("puts you to the center of the block you are standing on")
            .defaultValue(true)
            .build());
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());
    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables the module when you have placed the anvil")
            .defaultValue(true)
            .build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    public SelfAnvil() {
        super(Addon.Snail, "Self Anvil+", "Places a anvil in the air to burrow yourself");
    }
    @Override
    public void onActivate() {
        isReady = false;
        placed = false;
    }
    @Override
    public void onDeactivate() {
        lastPlaceTime = 0;
        placed = false;
        isReady = false;


    }

    private BlockPos pos;
    private boolean isReady;
    private boolean placed;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        pos = Objects.requireNonNull(mc.player).getBlockPos().up(2);
        FindItemResult Anvil = InvUtils.findInHotbar(Items.ANVIL);
        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < delay.get() * 1000) return;
        lastPlaceTime = time;
        if (isReady) {
            if (autoDisable.get()) {
                toggle();
            } else {
                return;
            }
        }
        if (WorldUtils.isAir(pos)) {
            InvUtils.swap(Anvil.slot(), true);
            Objects.requireNonNull(mc.interactionManager).interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false));
            InvUtils.swapBack();
            placed = true;
            if (center.get() && !CombatUtils.isCentered(this.mc.player)) {
                PlayerUtils.centerPlayer();
            }
        }
        if (placed) {
            isReady = true;
        }
        if (support.get()) {
            PlaceSupportBlocks();
        }
    }

    public void PlaceSupportBlocks() {
        BlockPos supportPosNorth = Objects.requireNonNull(mc.player).getBlockPos().north(1);
        BlockPos supportPosNorthUpOne = mc.player.getBlockPos().north(1).up(1);
        BlockPos supportPosNorthUpTwo = mc.player.getBlockPos().north(1).up(2);
        InvUtils.swap(InvUtils.findInHotbar(Items.OBSIDIAN).slot(), true);
        Rotations.rotate(Rotations.getYaw(supportPosNorthUpOne), Rotations.getPitch(supportPosNorthUpOne) ,100, false, () -> {
            Objects.requireNonNull(mc.interactionManager).interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(supportPosNorth.getX(), supportPosNorth.getY(), supportPosNorth.getZ()), Direction.UP, supportPosNorth, false));
            Objects.requireNonNull(mc.interactionManager).interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(supportPosNorthUpOne.getX(), supportPosNorthUpOne.getY(), supportPosNorthUpOne.getZ()), Direction.UP, supportPosNorthUpOne, false));
            Objects.requireNonNull(mc.interactionManager).interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(supportPosNorthUpTwo.getX(), supportPosNorthUpTwo.getY(), supportPosNorthUpTwo.getZ()), Direction.UP, supportPosNorthUpTwo, false));
            InvUtils.swapBack();
        });
    }

    @EventHandler
    public void AnvilRender(Render3DEvent event) {
        pos = Objects.requireNonNull(mc.player).getBlockPos().up(2);
        event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
    }
}
