package org.snail.plus.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.tick.Tick;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;

import java.util.ArrayList;
import java.util.List;

public class BurrowEsp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range to place pistons and redstone.")
            .defaultValue(4.0)
            .sliderMin(1.0)
            .sliderMax(10.0)
            .build());
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape Mode")
            .description("the shape mode of the box")
            .defaultValue(ShapeMode.Both)
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

    private final List<BlockPos> pos = new ArrayList<>();

    public BurrowEsp() {
        super(Addon.Snail, "Burrow esp", "highlights burrowed players");
    }
    @Override
    public void onDeactivate() {
        pos.clear();
    }
    @EventHandler
    public void ontick(TickEvent.Pre event) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if(mc.player.distanceTo(player) <= range.get()) {

                info(player.getName().getString() + " is burrowed");
            }
        }
    }
    @EventHandler
    public void onRender(Render3DEvent event) {
        for(PlayerEntity player: mc.world.getPlayers()) {
            event.renderer.box(BlockPos.ofFloored(CombatUtils.getBurrowedPos(player)), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            info("rendering");
        }
    }
}