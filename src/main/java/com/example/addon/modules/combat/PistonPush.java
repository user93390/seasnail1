package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
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
import net.minecraft.util.math.BlockPos;

public class PistonPush extends Module {
 private final SettingGroup sgGeneral = settings.getDefaultGroup();


 private final Setting<Double> Range = sgGeneral.add(new DoubleSetting.Builder()
         .name("range")
         .description("the range to place pistons and redstone")
         .defaultValue(4.0)
         .sliderMax(10.0)
         .sliderMin(1.0)
         .build());
 private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
         .name("rotate")
         .description("Automatically rotates towards the position where pistons are getting placed")
         .defaultValue(true)
         .build());

  public PistonPush() {
  super(Addon.Snail, "piston pusher", "");
   }
}