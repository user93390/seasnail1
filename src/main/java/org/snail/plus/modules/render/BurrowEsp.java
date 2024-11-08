package org.snail.plus.modules.render;

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
import net.minecraft.util.math.Vec3d;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;

import java.util.ArrayList;
import java.util.List;

public class BurrowEsp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignores friends.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> performance = sgGeneral.add(new BoolSetting.Builder()
            .name("performance")
            .description("Improves performance")
            .defaultValue(true)
            .build());

    private final Setting<Integer> maxPlayers = sgGeneral.add(new IntSetting.Builder()
            .name("max-players")
            .description("The maximum amount of burrowed players to render.")
            .sliderRange(1, 10)
            .visible(performance::get)
            .build());


    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The range to render burrowed players.")
            .defaultValue(4.0)
            .sliderRange(0.0, 10.0)
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
    private void onTick(TickEvent.Pre event) {
        burrowedPlayers.clear();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (ignoreFriends.get() && Friends.get().isFriend(player)) continue;
            if (CombatUtils.isBurrowed(player) && mc.player.distanceTo(player) <= range.get()) {
                burrowedPlayers.add(player);
                if (performance.get() && burrowedPlayers.size() >= maxPlayers.get()) break;
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        for (PlayerEntity player : burrowedPlayers) {
            Vec3d pos = new Vec3d(player.getX(), player.getY() + 0.4, player.getZ());
            event.renderer.box(BlockPos.ofFloored(pos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}