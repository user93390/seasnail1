package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.Snail.Plus.Addon;

import java.util.Objects;

public class SelfAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private long lastPlaceTime = 0;

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the sand is getting placed")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
            .name("support")
            .description("Places support blocks (recommended)")
            .defaultValue(true)
            .build());
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());
    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables the module when you have placed the anvil")
            .defaultValue(true)
            .build());
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());

    public SelfAnvil() {
        super(Addon.Snail, "Self Anvil+", "Places a anvil in the air to burrow yourself");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < delay.get() * 1000) return;
        lastPlaceTime = time;

        BlockPos playerPos = Objects.requireNonNull(mc.player).getBlockPos().up(2);
        FindItemResult Anvil = InvUtils.findInHotbar(Items.ANVIL);
        int AnvilSlot = Anvil.slot();
        assert mc.world != null;

        if (mc.world.getBlockState(playerPos).getBlock().equals(Blocks.AIR)) {
            InvUtils.swap(AnvilSlot, true);
            PlaceAnvil(mc.player);
        }

        if (support.get()) {
            PlaceSupportBlocks(mc.player);
        }

        if (autoDisable.get() && mc.world.getBlockState(playerPos).getBlock().equals(Blocks.ANVIL)) {
            toggle();
        }
    }

    public void PlaceSupportBlocks(PlayerEntity player) {
        assert mc.player != null;
        BlockPos supportPosNorth = mc.player.getBlockPos().north(1);
        BlockPos supportPosNorthUpOne = mc.player.getBlockPos().north(1).up(1);
        BlockPos supportPosNorthUpTwo = mc.player.getBlockPos().north(1).up(2);

        BlockUtils.place(supportPosNorth, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        BlockUtils.place(supportPosNorthUpOne, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
        BlockUtils.place(supportPosNorthUpTwo, InvUtils.findInHotbar(Items.OBSIDIAN), rotate.get(), 0, false);
    }

    public void PlaceAnvil(PlayerEntity player) {
        assert mc.player != null;
        BlockPos playerPos = mc.player.getBlockPos().up(2);

        BlockUtils.place(playerPos, InvUtils.findInHotbar(Items.ANVIL), rotate.get(), 0, false);
    }

    @EventHandler
    public void AnvilRender(Render3DEvent event) {
        BlockPos Pos = Objects.requireNonNull(mc.player).getBlockPos().up(2);
        if (Pos != null && mc.player != null && sideColor != null && lineColor != null && mc.world != null)
            event.renderer.box(Pos, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
    }
}
