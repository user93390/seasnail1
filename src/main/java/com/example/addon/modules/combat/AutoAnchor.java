package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.renderer.ShapeMode;

public class AutoAnchor extends Module {
    public enum SafetyMode {
        safe,
        balance,
        off,
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private long lastPlaceTime = 0;

    private final Setting<Integer> Range = sgGeneral.add(new IntSetting.Builder()
            .name("range")
            .description("Range to target player(s)")
            .defaultValue(3)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotation")
            .description("Rotates towards the block when placing.")
            .defaultValue(false)
            .build());

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());

    private final Setting<SafetyMode> safety = sgGeneral.add(new EnumSetting.Builder<SafetyMode>()
            .name("safe mode")
            .description("safety mode")
            .defaultValue(SafetyMode.safe)
            .build()
    );

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("Color")
            .description("The Color for positions to be placed.")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());

    private BlockPos targetPos;
    private BlockPos breakPos;
    private boolean shouldSwapToAnchor = false;
    private boolean shouldSwapToGlowstone = false;

    public AutoAnchor() {
        super(Addon.COMBAT, "Auto Anchor", "Automatically places Respawn Anchors near players.");
    }

    @Override
    public void onActivate() {
        targetPos = null;
        breakPos = null;
        shouldSwapToAnchor = false;
        shouldSwapToGlowstone = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ClientPlayerEntity player = mc.player;
        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < delay.get() * 1000) return;
        lastPlaceTime = time;

        if (player == null || player.getHealth() <= 0) return;
        PlayerEntity target = TargetUtils.getPlayerTarget(Range.get(), priority.get());
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);

        // Check if the target is null before proceeding
        if (target == null || target == player || target.distanceTo(player) > Range.get()) {
            return;
        }

        BlockPos targetHeadPos = target.getBlockPos().up(2);
        BlockPos supportPosNorth = target.getBlockPos().north(1);
        BlockPos supportPosNorthUpOne = target.getBlockPos().north(1).up(1);
        BlockPos supportPosNorthUpTwo = target.getBlockPos().north(1).up(2);

        // Place obsidian blocks
        BlockUtils.place(supportPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        BlockUtils.place(supportPosNorthUpOne, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        BlockUtils.place(supportPosNorthUpTwo, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);

        if (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.AIR) {
            // Place a respawn anchor
            if (anchor.isOffhand()) {
                mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetHeadPos.getX() + 0.5, targetHeadPos.getY() + 0.5, targetHeadPos.getZ() + 0.5), Direction.UP, targetHeadPos, true));
            } else {
                InvUtils.swap(anchor.slot(), true);
                mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetHeadPos.getX() + 0.5, targetHeadPos.getY() + 0.5, targetHeadPos.getZ() + 0.5), Direction.UP, targetHeadPos, true));
                shouldSwapToAnchor = true;
            }
        } else if (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            // Interact with the respawn anchor to explode it
            mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetHeadPos.getX() + 0.5, targetHeadPos.getY() + 0.5, targetHeadPos.getZ() + 0.5), Direction.UP, targetHeadPos, true));
        } else if (mc.world.getBlockState(targetHeadPos.up()).getBlock() == Blocks.AIR) {
            // Place glowstone if there's a respawn anchor below
            if (glowStone.isOffhand()) {
                mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetHeadPos.getX() + 0.5, targetHeadPos.getY() + 1.5, targetHeadPos.getZ() + 0.5), Direction.UP, targetHeadPos.up(), true));
            } else {
                InvUtils.swap(glowStone.slot(), true);
                mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(targetHeadPos.getX() + 0.5, targetHeadPos.getY() + 1.5, targetHeadPos.getZ() + 0.5), Direction.UP, targetHeadPos.up(), true));
                shouldSwapToGlowstone = true;
            }
        }

        // Swap to glowstone if necessary
        if (shouldSwapToGlowstone) {
            InvUtils.swap(glowStone.slot(), true);
            shouldSwapToGlowstone = false;
        }

        // Swap to anchor if necessary
        if (shouldSwapToAnchor) {
            InvUtils.swap(anchor.slot(), true);
            shouldSwapToAnchor = false;
        }

        // Break the glowstone if it's placed
        if (mc.world.getBlockState(targetHeadPos).getBlock() == Blocks.GLOWSTONE) {
            BlockUtils.breakBlock(targetHeadPos, false);
        }

        RenderUtils.renderTickingBlock(targetHeadPos, color.get(), color.get(), ShapeMode.Both, 5, 5, true, true);
    }
}

