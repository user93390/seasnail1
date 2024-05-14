package com.example.addon.modules.render;

import java.util.ArrayList;
import java.util.List;

import com.example.addon.Addon;

import meteordevelopment.meteorclient.events.world.TickEvent.Post;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import com.example.addon.modules.misc.Notifications;

public class BurrowEsp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get shown")
            .defaultValue(4)
            .min(0)
            .sliderMax(6)
            .build());
    
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
    .name("Color")
    .description("The Color for positions to be placed.")
    .defaultValue(new SettingColor(255, 0, 0, 75))
    .build());

    private final List<BlockPos> blockList = new ArrayList<>();

    private MinecraftClient mc;

    public BurrowEsp() {
        super(Addon.RENDER, "burrow-esp", "Shows you if players are burrowed.");
    }

    @Override
    public void onActivate() {
        mc = MinecraftClient.getInstance();
    }
 
    @EventHandler
private void onTick(Post event) {
    if (mc.world == null) return; // Check if the world is null
    AbstractClientPlayerEntity player = mc.player; // Get the local player
    
    // Calculate the new Y coordinate by adding 0.4 and then converting it to an integer
    PlayerEntity target = TargetUtils.getPlayerTarget(range.get().doubleValue(), SortPriority.LowestDistance);
    if (target != null) {
        BlockPos targetPos = new BlockPos((int) player.getPos().x, (int) (player.getPos().y + 0.4), (int) player.getPos().z);
        
        RenderUtils.renderTickingBlock(targetPos, color.get(), color.get(), ShapeMode.Both, 5, 5, true, false);
        }
    }
}

