package dev.seasnail1.modules.combat;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.CombatUtils;
import dev.seasnail1.utilities.WorldUtils;
import dev.seasnail1.utilities.swapUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public class selfAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where the anvil is placed.")
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
            .description("Disables the module when you have placed the anvil.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> autoCenter = sgGeneral.add(new BoolSetting.Builder()
            .name("auto center")
            .description("centers you when placing the anvil")
            .defaultValue(true)
            .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side color")
            .description("Side color")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line color")
            .description("Line color")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build());


    private long lastPlaceTime = 0;

    public selfAnvil() {
        super(Addon.CATEGORY, "self-Anvil", "Places an anvil on the top of your head to burrow yourself.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            FindItemResult anvil = InvUtils.findInHotbar(Items.ANVIL);

            if (autoCenter.get() && !CombatUtils.isCentered(mc.player)) {
                PlayerUtils.centerPlayer();
                info("centered");
                return;
            }

            if (mc.world.getBlockState(mc.player.getBlockPos().up(2)).isAir()) {
                if (anvil.found()) {
                    if (WorldUtils.isAir(mc.player.getBlockPos().up(2), false)) {
                        if (support.get()) {
                            PlaceSupportBlocks(mc.player, InvUtils.findInHotbar(Items.OBSIDIAN));
                        }

                        long time = System.currentTimeMillis();
                        if ((time - lastPlaceTime) < delay.get() * 1000) return;
                        lastPlaceTime = time;
                        WorldUtils.placeBlock(anvil, mc.player.getBlockPos().up(2), WorldUtils.HandMode.MainHand, WorldUtils.DirectionMode.Down, true, swapUtils.swapMode.silent, rotate.get());
                    }
                }
            }
            if (autoDisable.get() && mc.world.getBlockState(mc.player.getBlockPos().up(2)).getBlock().equals(Blocks.ANVIL)) {
                toggle();
            }
        } catch (Exception e) {
            error("An error occurred while placing the anvil: " + e.getMessage());
            Addon.Logger.error("An error occurred while placing the anvil: {}", Arrays.toString(e.getStackTrace()));
        }
    }

    private void PlaceSupportBlocks(PlayerEntity player, FindItemResult obsidian) {
        for (int i = 0; i <= 2; i++) {
            WorldUtils.placeBlock(obsidian, player.getBlockPos().north(1).up(i), WorldUtils.HandMode.MainHand, WorldUtils.DirectionMode.Down, true, swapUtils.swapMode.silent, rotate.get());
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        BlockPos pos = mc.player.getBlockPos().up(2);
        if (pos != null) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
        }
    }
}