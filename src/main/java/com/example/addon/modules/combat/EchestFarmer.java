package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class EchestFarmer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates towards the position where the enderchest is")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> smart = sgGeneral.add(new BoolSetting.Builder()
        .name("smart")
        .description("Only mines until you have the amount of obsidian you need")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("amount")
        .description("The amount of obsidian")
        .defaultValue(64)
        .min(1)
        .sliderMax(128)
        .build()
    );

    private final Setting<Boolean> disable = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Disables when you have the amount you need")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("Color")
        .description("Renders a color where the ender chest is placed")
        .defaultValue(new SettingColor(255, 0, 0, 75))
        .build()
    );

    public EchestFarmer() {
        super(Addon.Snail, "Echest-farmer+", "Mines enderchests for obsidian");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        BlockPos playerPos = mc.player.getBlockPos().add(0, 1, 1);
        if (mc.world.getBlockState(playerPos).getBlock().equals(Blocks.AIR)) {
            if (InvUtils.findInHotbar(Items.ENDER_CHEST) == null) {
                ChatUtils.error("No enderchest in hotbar");
                toggle();
                return;
            }
        }
        if (playerPos != null) {
            BlockUtils.place(playerPos, InvUtils.findInHotbar(Items.ENDER_CHEST), rotate.get(), 0, false);
            RenderUtils.renderTickingBlock(playerPos, color.get(), color.get(), ShapeMode.Both, 5, 5, true, false);
        }
    }
}

