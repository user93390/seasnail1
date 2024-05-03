package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;;


public class AnchorAuraPlus extends Module {
         public AnchorAuraPlus() {
                super(Addon.COMBAT, "SnailAnchorAura", "Harms players around you with respawn anchors");

                
                final SettingGroup sgGeneral = settings.getDefaultGroup();
                final SettingGroup sgRange = settings.createGroup("Place");
                final SettingGroup sgFacePlace = settings.createGroup("Face Place");
                final SettingGroup sgDisable = settings.createGroup("Disable");
                final SettingGroup sgRender = settings.createGroup("Render");
                final SettingGroup sgRotationMode = settings.createGroup("Rotation Mode");
                final Setting<Boolean> facePlace = sgFacePlace.add(new BoolSetting.Builder()
                                .name("Enabled")
                                .description("Enable Face Place feature")
                                .defaultValue(false)
                                .build());

                final Setting<Integer> targetRange = sgRange.add(new IntSetting.Builder()
                                .name("target range")
                                .description("target distance")
                                .defaultValue(4)
                                .min(0)
                                .max(10)
                                .sliderMin(0)
                                .sliderMax(10)
                                .build());

                final Setting<Integer> healthThreshold = sgFacePlace.add(new IntSetting.Builder()
                                .name("Health Threshold")
                                .description("The health threshold to trigger Face Place")
                                .defaultValue(6)
                                .min(0)
                                .max(36)
                                .sliderMin(0)
                                .sliderMax(36)
                                .visible(facePlace::get) 
                                .build());

                final Setting<Integer> armorThreshold = sgFacePlace.add(new IntSetting.Builder()
                                .name("Armor Threshold")
                                .description("The armor durability threshold to trigger Face Place")
                                .defaultValue(6)
                                .min(0)
                                .max(100)
                                .sliderMin(0)
                                .sliderMax(100)
                                .visible(facePlace::get)
                                .build());

                final Setting<Boolean> disabler = sgDisable.add(new BoolSetting.Builder()
                                .name("Disabler")
                                .description("Disable the module")
                                .defaultValue(true)
                                .build());

                final Setting<Boolean> disableInNether = sgDisable.add(new BoolSetting.Builder()
                                .name("Disable in Nether")
                                .description("Disable when in the Nether")
                                .visible(disabler::get)
                                .defaultValue(true)
                                .build());

                final Setting<Boolean> disableOutOfGlowstone = sgDisable.add(new BoolSetting.Builder()
                                .name("Disable Out of Glowstone")
                                .description("Disable when you have no glowstone in your hotbar")
                                .visible(disabler::get)
                                .defaultValue(true)
                                .build());

                final Setting<Boolean> disableOutOfRespawnAnchor = sgDisable.add(new BoolSetting.Builder()
                                .name("Disable Out of Respawn Anchor")
                                .description("Disable when you have no respawn anchors in your hotbar")
                                .visible(disabler::get)
                                .defaultValue(true)
                                .build());
                                final Setting<Integer> MinHealth = sgFacePlace.add(new IntSetting.Builder()
                                .name("Min health")
                                .description("Disable when you have X amout of health")
                                .visible(disabler::get)
                                .defaultValue(2)
                                .build());
                final Setting<Integer> placeRate = sgGeneral.add(new IntSetting.Builder()
                                .name("Place Rate")
                                .description("How often should the module place anchors")
                                .defaultValue(1)
                                .build());

                final Setting<Integer> breakRate = sgGeneral.add(new IntSetting.Builder()
                                .name("Break Rate")
                                .description("How often should the module break anchors")
                                .defaultValue(1)
                                .build());

                final Setting<Integer> facePlaceRate = sgFacePlace.add(new IntSetting.Builder()
                                .name("Face Place Rate")
                                .description("How often should the module face place anchors")
                                .defaultValue(1)
                                .build());

                final Setting<Integer> facePlaceBreakRate = sgFacePlace.add(new IntSetting.Builder()
                                .name("Face Place Break Rate")
                                .description("How often should the module break anchors during face place")
                                .defaultValue(1)
                                .build());

                final Setting<Integer> faceBreakRate = sgFacePlace.add(new IntSetting.Builder()
                                .name("Face Break Rate")
                                .description("How often should the module break anchors during face place")
                                .defaultValue(1)
                                .build());
                final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
                                .name("shape-mode")
                                .description("How the shapes are rendered.")
                                .defaultValue(ShapeMode.Both)
                                .build());

                final Setting<Boolean> renderPlace = sgRender.add(new BoolSetting.Builder()
                                .name("render-place")
                                .description("Renders the block where it is placing an anchor.")
                                .defaultValue(true)
                                .build());
                final Setting<SettingColor> placeSideColor = sgRender.add(new ColorSetting.Builder()
                                .name("place-side-color")
                                .description("The side color for positions to be placed.")
                                .defaultValue(new SettingColor(255, 0, 0, 75))
                                .visible(renderPlace::get)
                                .build());

                final Setting<SettingColor> placeLineColor = sgRender.add(new ColorSetting.Builder()
                                .name("place-line-color")
                                .description("The line color for positions to be placed.")
                                .defaultValue(new SettingColor(255, 0, 0, 255))
                                .visible(renderPlace::get)
                                .build());

                final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder()
                                .name("render-break")
                                .description("Renders the block where it is breaking an anchor.")
                                .defaultValue(true)
                                .build());

                final Setting<SettingColor> breakSideColor = sgRender.add(new ColorSetting.Builder()
                                .name("break-side-color")
                                .description("The side color for anchors to be broken.")
                                .defaultValue(new SettingColor(255, 0, 0, 75))
                                .visible(renderBreak::get)
                                .build());

                final Setting<SettingColor> breakLineColor = sgRender.add(new ColorSetting.Builder()
                                .name("break-line-color")
                                .description("The line color for anchors to be broken.")
                                .defaultValue(new SettingColor(255, 0, 0, 255))
                                .visible(renderBreak::get)
                                .build());
        }
        private PlayerEntity target;
        @EventHandler
        private void onTick(TickEvent.Post event) {
                if (mc.world.getDimension().respawnAnchorWorks()) {
                        error("You cannot use Module while in the nether... disabling");
                        toggle();
                        return;
                }
                if (EntityUtils.getTotalHealth(mc.player) < 1) {
                        error("Your health is too low... disabling");
                        toggle();
                        error("You cannot use Module while in the nether... disabling");
                        return;
                }
                //find respawn anchor
                FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
                //find glowstone
                FindItemResult glowStone = InvUtils.findInHotbar(Items.GLOWSTONE);
                
                //if theres no anchor or glowstone found, disable
                if(!anchor.found() || !glowStone.found()) {
                  warning ("No anchor or glowstone found... disabling");
                  toggle();
                  System.out.println("no glowstone or anchors");
                  return;
                }
         }       //finding a target

        }