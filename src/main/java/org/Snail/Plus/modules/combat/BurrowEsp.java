package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;

import java.util.Objects;

import static meteordevelopment.meteorclient.utils.entity.TargetUtils.*;

public class BurrowEsp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> Range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range to place pistons and redstone.")
            .defaultValue(4.0)
            .sliderMin(1.0)
            .sliderMax(10.0)
            .build());

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape Mode")
            .description("the shape mode")
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
    private PlayerEntity target;
    private BlockPos pos;
    public BurrowEsp() {
        super(Addon.Snail, "Burrow Esp", "highlights burrowed players");
    }
    @EventHandler
    public void onTick(TickEvent.Pre event) {
        target = getPlayerTarget(Range.get(), SortPriority.LowestDistance);
        if(target == null) return;
        pos = Objects.requireNonNull(target).getBlockPos();
        if (isBadTarget(target, Range.get())) return;
    }

    @EventHandler
    public void onRnder(Render3DEvent event) {
        if (CombatUtils.isBurrowed(target)) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
    @Override
    public String getInfoString() {
        if(target == null) return null;
        String text;
        if(CombatUtils.isBurrowed(target)) {
            text = "Burrowed";
        } else {
            text = "Not Burrowed";
        }
        return text;
    }
}