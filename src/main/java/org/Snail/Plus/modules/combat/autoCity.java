package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.snail.plus.Addon;
import org.snail.plus.modules.misc.StealthMine;
import org.snail.plus.utils.CombatUtils;
import org.snail.plus.utils.WorldUtils;

import java.util.Objects;


public class autoCity extends Module {


    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(4)
            .min(0)
            .sliderMax(10)
            .build());
    private final Setting<Boolean> AntiBurrow = sgGeneral.add(new BoolSetting.Builder()
            .name("burrow")
            .description("mines players burrows")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the block is being broken.")
            .defaultValue(true)
            .build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());
    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
            .name("support-place")
            .description("Places support blocks (Obsidian).")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> onlyInHoles = sgGeneral.add(new BoolSetting.Builder()
            .name("only holes")
            .description("only targets players in holes")
            .defaultValue(true)
            .build());
    private BlockPos currentPos;
    public autoCity() {
        super(Addon.Snail, "Auto city+", "Uses StealthMine+ to auto city blocks");
    }

    @Override
    public void onActivate() {
        if(!Modules.get().get(StealthMine.class).isActive()) {
            Modules.get().get(StealthMine.class).toggle(); //auto activate if using autocity
        }
        currentPos = null;
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        if (target != null) {
            try {
                BlockPos East = target.getBlockPos().east(1);
                BlockPos West = target.getBlockPos().west(1);
                BlockPos North = target.getBlockPos().north(1);
                BlockPos South = target.getBlockPos().south(1);
                BlockPos BurrowPos = target.getBlockPos();

                BlockPos SupportPosNorth = target.getBlockPos().north(1).down(1);
                BlockPos SupportPosSouth = target.getBlockPos().south(1).down(1);
                BlockPos SupportPosEast = target.getBlockPos().east(1).down(1);
                BlockPos SupportPosWest = target.getBlockPos().west(1).down(1);

                if (onlyInHoles.get() && !CombatUtils.isSurrounded(target)) return;

                if (Objects.requireNonNull(mc.world).getBlockState(East).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(East).isAir()) {
                    currentPos = East;
                    if (support.get() && mc.world.getBlockState(SupportPosEast).isAir()) {
                        SupportPlace(SupportPosEast);
                    }
                } else {
                    if (mc.world.getBlockState(West).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(West).isAir()) {
                        currentPos = West;
                        StealthMine.blockPos.set(currentPos);
                        if (support.get() && mc.world.getBlockState(SupportPosWest).isAir()) {
                            SupportPlace(SupportPosWest);
                        }
                    } else {
                        if (mc.world.getBlockState(North).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(North).isAir()) {
                            currentPos = North;
                            //used to break blocks
                            StealthMine.blockPos.set(currentPos);
                            if (support.get() && mc.world.getBlockState(SupportPosNorth).isAir()) {
                                SupportPlace(SupportPosNorth);
                            }
                        } else {
                            if (mc.world.getBlockState(South).getBlock() != Blocks.BEDROCK && !mc.world.getBlockState(South).isAir()) {

                                currentPos = South;
                                StealthMine.blockPos.set(currentPos);
                                if (support.get() && mc.world.getBlockState(SupportPosSouth).isAir()) {
                                    SupportPlace(SupportPosSouth);
                                }
                            }
                        }
                    }
                }
                if (AntiBurrow.get() && CombatUtils.isBurrowed(target) && WorldUtils.isAir(BurrowPos) && WorldUtils.isBreakable(BurrowPos)) {
                    currentPos = BurrowPos;
                    StealthMine.blockPos.set(currentPos);
                }
            } catch (Exception e) {
                System.out.println("Exception caught -> " + e.getCause() + ". Message -> " + e.getMessage());
            }
        }
    }
    private void SupportPlace(BlockPos pos) {
        FindItemResult Block = InvUtils.findInHotbar(Items.OBSIDIAN);
        BlockUtils.place(pos, Block, rotate.get(), 100, true);
    }
    @Override
    public void onDeactivate() {
        currentPos = null;
    }
}
