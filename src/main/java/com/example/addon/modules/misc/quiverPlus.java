package com.example.addon.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.AbstractBlockAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.ArrayList;
import java.util.List;

import com.example.addon.Addon;

public class quiverPlus extends Module {
    public quiverPlus() {
        super(Addon.MISC, "sn++ quiver", "Shoots arrows at yourself."); 
         
         final SettingGroup sgGeneral = settings.getDefaultGroup();
         final SettingGroup sgArrows = settings.getDefaultGroup();

         final Setting<Integer> minHealth = sgGeneral.add(new IntSetting.Builder()
         .name("Health")
         .description("The minimum health required to shoot an arrow.")
         .defaultValue(10)
         .min(0)
         .max(36)
         .sliderMin(0) 
         .sliderMax(36) 
         .build()
         );
         final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
         .name("cooldown")
         .description("How many ticks between shooting effects (19 minimum for NCP).")
         .defaultValue(10)
         .range(0,36)
         .sliderRange(0,36)
         .build()
     );
 
    }
}
