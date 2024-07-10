package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.Offhand;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationCalculator;
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.CombatUtils;

import java.util.Objects;

public class AutoCrystalChronos extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(4)
            .min(0)
            .sliderMax(10)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the block is being broken.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> antisuicide = sgGeneral.add(new BoolSetting.Builder()
            .name("antisuicide")
            .description("Antisuicide")
            .defaultValue(false)
            .build());

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());


    public AutoCrystalChronos() {
        super(Addon.Snail, "AutoCrystalChronos", "better/worse autocrystal bro");
    }

    private long lastPlaceTime = 0;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());

        BlockPos targetPos = target.getBlockPos();


        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < delay.get() * 1000) return;
        lastPlaceTime = time;


            if (CombatUtils.isSurrounded(target) == false & BlockUtils.canPlace(targetPos.north(1))) {
                BlockUtils.place(targetPos.north(1), InvUtils.findInHotbar(Items.END_CRYSTAL), rotate.get(), 0, false);

                RenderUtils.renderTickingBlock(targetPos.north(1).down(1), Color.RED, Color.RED, ShapeMode.Both, 0, 10, true, false);

            }

            else if (CombatUtils.isSurrounded(target) == false & BlockUtils.canPlace(targetPos.east(1))) {
            BlockUtils.place(targetPos.east(1), InvUtils.findInHotbar(Items.END_CRYSTAL), rotate.get(), 0, false);

            RenderUtils.renderTickingBlock(targetPos.east(1).down(1), Color.RED, Color.RED, ShapeMode.Both, 0, 10, true, false);
            }


            else if (CombatUtils.isSurrounded(target) == false & BlockUtils.canPlace(targetPos.south(1))) {
            BlockUtils.place(targetPos.south(1), InvUtils.findInHotbar(Items.END_CRYSTAL), rotate.get(), 0, false);

            RenderUtils.renderTickingBlock(targetPos.south(1).down(1), Color.RED, Color.RED, ShapeMode.Both, 0, 10, true, false);
            }


            else if (CombatUtils.isSurrounded(target) == false & BlockUtils.canPlace(targetPos.west(1))) {
            BlockUtils.place(targetPos.west(1), InvUtils.findInHotbar(Items.END_CRYSTAL), rotate.get(), 0, false);

            RenderUtils.renderTickingBlock(targetPos.west(1).down(1), Color.RED, Color.RED, ShapeMode.Both, 0, 10, true, false);
            }

        }

    }
