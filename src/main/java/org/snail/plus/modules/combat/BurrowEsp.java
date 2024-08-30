package org.snail.plus.modules.combat;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final List<PlayerEntity> burrowedPlayers = new ArrayList<>();

    public BurrowEsp() {
        super(Addon.Snail, "Burrow esp", "highlights burrowed players");
    }

    @Override
    public void onActivate() {
        burrowedPlayers.clear();
    }

    @Override
    public void onDeactivate() {
        burrowedPlayers.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        burrowedPlayers.clear();
        for (PlayerEntity target : mc.world.getPlayers()) {
            if (target == mc.player || Friends.get().isFriend(target)) continue;
            if (CombatUtils.isBurrowed(target)) {
                burrowedPlayers.add(target);
            }
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        for (BlockPos pos : burrowedPlayers.stream().map(PlayerEntity::getBlockPos).toList()) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @Override
    public String getInfoString() {
        return String.valueOf(burrowedPlayers.size());
    }
}