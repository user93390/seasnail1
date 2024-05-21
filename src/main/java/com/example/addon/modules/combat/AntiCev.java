package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.util.ArrayList;
import java.util.List;



public class AntiCev extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> zeroTick = sgGeneral.add(new BoolSetting.Builder()
            .name("zero-tick")
            .description("Instantly attacks the crystal(s).")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each attack.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .visible(zeroTick::get)
            .build()
    );

    private final Setting<Boolean> place = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-block")
            .description("Places obsidian to block future civs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> attackEntities = sgGeneral.add(new BoolSetting.Builder()
            .name("attack-entities")
            .description("Attack nearby entities such as boats and end crystals.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("Maximum range to search for entities.")
            .defaultValue(5.0)
            .min(0.0)
            .sliderMax(10.0)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the entity.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> pitch = sgGeneral.add(new IntSetting.Builder()
            .name("pitch")
            .description("The pitch angle for rotation.")
            .defaultValue(90)
            .min(-90)
            .max(90)
            .sliderMax(90)
            .visible(rotate::get)
            .build()
    );
    private final Setting<Boolean> onlyInHoles = sgGeneral.add(new BoolSetting.Builder()
            .name("only in hole")
            .description("Very recommended (may be buggy if you turn this off).")
            .defaultValue(true)
            .build()
    );

    public AntiCev() {
        super(Addon.COMBAT, "Anti-Cev", "The best anticev, made by seasnail1.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        BlockPos playerPos = mc.player.getBlockPos();

        BlockPos crystalNorth = playerPos.north(1).up(2);
        BlockPos crystalEast = playerPos.east(1).up(2);
        BlockPos crystalSouth = playerPos.south(1).up(2);
        BlockPos crystalWest = playerPos.west(1).up(2);

        BlockUtils.breakBlock(crystalNorth, false);
        BlockUtils.breakBlock(crystalEast, false);
        BlockUtils.breakBlock(crystalSouth, false);
        BlockUtils.breakBlock(crystalWest, false);

        if (onlyInHoles.get() && !isSurrounded(mc.player)) {
            return;
        }

        if (attackEntities.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player || !entity.isAlive() || entity.squaredDistanceTo(new Vec3d(playerPos.getX(), playerPos.getY(), playerPos.getZ())) > range.get() * range.get()) continue;
                if(rotate.get()) {
                    // Calculate yaw and pitch angles to point towards the entity's position
                    double yaw = Math.toDegrees(Math.atan2(entity.getZ() - mc.player.getZ(), entity.getX() - mc.player.getX())) - 90;
                    double pitch = Math.toDegrees(Math.atan2(entity.getY() - mc.player.getY(), Math.sqrt((entity.getX() - mc.player.getX()) * (entity.getX() - mc.player.getX()) + (entity.getZ() - mc.player.getZ()) * (entity.getZ() - mc.player.getZ()))));
                    
                    // Rotate the player's view towards the entity
                    Rotations.rotate(yaw, pitch, () -> {
                        mc.interactionManager.attackEntity(mc.player, entity);
                    });
                } else {
                    mc.interactionManager.attackEntity(mc.player, entity);
                }
            }
        }

        if (place.get()) {
            BlockPos obbyNorth = playerPos.north(1).up(2);
            BlockPos obbyEast = playerPos.east(1).up(2);
            BlockPos obbySouth = playerPos.south(1).up(2);
            BlockPos obbyWest = playerPos.west(1).up(2);
            BlockUtils.place(obbyNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
            BlockUtils.place(obbyEast, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
            BlockUtils.place(obbySouth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
            BlockUtils.place(obbyWest, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        }
    }

    private boolean isSurrounded(PlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();
        List<BlockPos> positions = new ArrayList<>();

        // Add surrounding block positions
        positions.add(playerPos.north());
        positions.add(playerPos.east());
        positions.add(playerPos.south());
        positions.add(playerPos.west());

        for (BlockPos pos : positions) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock() == Blocks.AIR) return false;
        }

        return true;
    }
}
