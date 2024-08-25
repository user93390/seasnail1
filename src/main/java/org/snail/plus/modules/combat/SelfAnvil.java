package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;

public class SelfAnvil extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private long lastPlaceTime = 0;

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

    public SelfAnvil() {
        super(Addon.Snail, "Self Anvil+", "Places an anvil in the air to burrow yourself.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < delay.get() * 1000) return;
        lastPlaceTime = time;
        FindItemResult anvil = InvUtils.findInHotbar(Items.ANVIL);
        if(autoCenter.get() && !CombatUtils.isCentered(mc.player)) {
            PlayerUtils.centerPlayer();
            info("centered");
            return;
        }

        if (mc.world.getBlockState(mc.player.getBlockPos().up(2)).isAir()) {
            if (anvil.found()) {
                InvUtils.swap(anvil.slot(), true);
                PlaceAnvil(mc.player.getBlockPos().up(2));
                InvUtils.swapBack();
            }
        }

        if (support.get()) {
            PlaceSupportBlocks(mc.player, InvUtils.findInHotbar(Items.OBSIDIAN));
        }

        if (autoDisable.get() && mc.world.getBlockState(mc.player.getBlockPos().up(2)).getBlock().equals(Blocks.ANVIL)) {
            toggle();
        }
    }

    private void PlaceSupportBlocks(PlayerEntity player, FindItemResult obsidian) {
        for(int i = 0; i <= 2; i++) {

            BlockPos support = new BlockPos((int) player.getX(), (int)player.getY() + i,(int) player.getZ());
            if(rotate.get()) Rotations.rotate(Rotations.getYaw(support), Rotations.getPitch(support));
            InvUtils.swap(obsidian.slot(), true);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(support.getX(), support.getY(), support.getZ()), Direction.UP, support, false));
            InvUtils.swapBack();
        }
    }

    private void PlaceAnvil(BlockPos pos) {
        if(rotate.get()) Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false));
    }

    @EventHandler
    public void AnvilRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        BlockPos pos = mc.player.getBlockPos().up(2);
        if (pos != null) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), (int) 1.0f);
        }
    }
}