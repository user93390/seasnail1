package dev.seasnail1.modules.combat;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.CombatUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class SelfAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Automatically rotates towards the position where the anvil is placed.").defaultValue(true).build());

    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder().name("support").description("Places support blocks (recommended)").defaultValue(true).build());

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder().name("delay").description("Delay in seconds between each block placement.").defaultValue(1.0).min(0).sliderMax(5).build());

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder().name("auto-disable").description("Disables the module when you have placed the anvil.").defaultValue(true).build());

    private final Setting<Boolean> autoCenter = sgGeneral.add(new BoolSetting.Builder().name("auto center").description("centers you when placing the anvil").defaultValue(true).build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side color").description("Side color").defaultValue(new SettingColor(255, 0, 0, 75)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line color").description("Line color").defaultValue(new SettingColor(255, 0, 0, 255)).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final long lastPlaceTime = 0;
    private FindItemResult item;
    private BlockPos anvil;

    public SelfAnvil() {
        super(Addon.CATEGORY, "self-Anvil", "Places an anvil on the top of your head to burrow yourself.");
    }

    @Override
    public void onActivate() {
        item = InvUtils.findInHotbar(Items.ANVIL);
        if (item == null) {
            error("You need an anvil in your hotbar to use this module.");
            toggle();
        }
    }

    @Override
    public void onDeactivate() {
        anvil = null;
        item = null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        anvil = mc.player.getBlockPos().up(2);

        if (autoCenter.get() && !CombatUtils.isCentered(mc.player)) {
            PlayerUtils.centerPlayer();
            info("centered");
            return;
        }

        if (autoDisable.get() && mc.world.getBlockState(mc.player.getBlockPos().up(2)).getBlock().equals(Blocks.ANVIL)) {
            toggle();
            return;
        }

        if (System.currentTimeMillis() - lastPlaceTime >= delay.get() * 1000) {
            placeAnvil(mc.player, item);
        }
    }

    private void placeAnvil(PlayerEntity player, FindItemResult anvil) {
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);

        if (anvil == null) return;

        if (support.get()) {
            PlaceSupportBlocks(player, obsidian);
        }

        BlockUtils.place(this.anvil, obsidian, rotate.get(), 100, true);
    }

    private void PlaceSupportBlocks(PlayerEntity player, FindItemResult obsidian) {
        for (int i = 0; i <= 2; i++) {
            BlockUtils.place(player.getBlockPos().north(1), obsidian, rotate.get(), 100, true);
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (this.anvil != null) {
            event.renderer.box(this.anvil, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}