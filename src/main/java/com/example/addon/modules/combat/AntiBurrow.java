package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.Box;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.concurrent.TimeUnit;

import static java.awt.Color.blue;
import static java.awt.Color.red;

public class AntiBurrow extends Module {
    public enum ShapeMode {
        line,
        side,
        both,
    }
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private PlayerEntity target;

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius players can be in to be targeted.")
            .defaultValue(5)
            .range(0, 7)
            .sliderRange(0, 7)
            .build()
    );

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables the module when you have placed the sand")
            .defaultValue(true)
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


    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build()
    );
    
    private final Setting<Boolean> color = sgGeneral.add(new BoolSetting.Builder()
            .name("color")
            .description("Shows colors when placing")
            .defaultValue(true)
            .build()
    );
    
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the block overlay.")
        .defaultValue(new SettingColor(255, 255, 255, 45))
        .visible(color::get)
        .build());

    
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of the lines")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(color::get)
        .build()
    );
    private final Setting<SettingColor> sidecolor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Color of the sides")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(color::get)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Shape to render")
        .defaultValue(ShapeMode.both)
        .build()
    );
    
    public AntiBurrow() {
        super(Addon.COMBAT, "Anti Burrow", "Disables a meta on most anarchy servers");
    }

    private long lastPlaceTime = 0;

    @EventHandler
    private void onTick(TickEvent.Pre event) throws InterruptedException {
        target = TargetUtils.getPlayerTarget(range.get(), priority.get()); // Initialize the target variable

        if (target == null || TargetUtils.isBadTarget(target, range.get())) return;

        BlockPos targetPos = target.getBlockPos();

        long time = System.currentTimeMillis();
        if ((time- lastPlaceTime) < delay.get() * 1000) return;
        lastPlaceTime = time;

        BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.OAK_BUTTON, Items.BIRCH_BUTTON, Items.ACACIA_BUTTON, Items.DARK_OAK_BUTTON, Items.STONE_BUTTON, Items.SPRUCE_BUTTON), rotate.get(), 0, false);

        Vec3d playerPos = mc.player.getPos();
        Vec3d closestPlayerPos = mc.world.getClosestPlayer(playerPos.x, playerPos.y, playerPos.z, 100, false).getPos();

        if (autoDisable.get()) {
            this.toggle();
            ChatUtils.sendMsg(Formatting.RED, "Anti Burrow has been Disabled");
        }
    }
}
