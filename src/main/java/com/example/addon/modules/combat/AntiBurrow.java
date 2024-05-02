package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import meteordevelopment.meteorclient.utils.world.BlockUtils;

public class AntiBurrow extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private PlayerEntity target; // Declare the target variable

    // General
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius players can be in to be targeted.")
            .defaultValue(5)
            .range(0, 7)
            .sliderRange(0, 7)
            .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The radius buttons can be placed.")
            .defaultValue(4)
            .range(0, 6)
            .sliderRange(0, 6)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates towards the webs when placing.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build()
    );

    public AntiBurrow() {
        super(Addon.COMBAT, "anti-burrow", "almost disables a meta on every server");
    }

    
    private void onTick(TickEvent.Pre event) {
        target = TargetUtils.getPlayerTarget(range.get(), priority.get());

        if (target == null || TargetUtils.isBadTarget(target, range.get())) return;

        BlockPos targetPos = target.getBlockPos();
            BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.OAK_BUTTON), rotate.get(), 0, false);
    }
}
