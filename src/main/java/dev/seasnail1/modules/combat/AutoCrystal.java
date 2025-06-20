package dev.seasnail1.modules.combat;

import dev.seasnail1.Addon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

/*
TODO: implement logic and make it work
 */

public class AutoCrystal extends Module {
    // Settings
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Placement
    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder().name("place").defaultValue(true).build());
    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder().name("place-range").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Boolean> support = sgPlace.add(new BoolSetting.Builder().name("support-block").description("Places obsidian under crystals if needed.").defaultValue(true).build());
    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder().name("place-delay").defaultValue(1).min(0).sliderMax(20).build());

    // Breaking
    private final Setting<Boolean> break_ = sgBreak.add(new BoolSetting.Builder().name("break").defaultValue(true).build());
    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder().name("break-range").defaultValue(4.5).min(0).sliderMax(6).build());
    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder().name("break-delay").defaultValue(1).min(0).sliderMax(20).build());

    // Damage
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range").defaultValue(12.5).min(0).sliderMax(16).build());
    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder().name("min-damage").defaultValue(6.5).min(0).sliderMax(36).build());
    private final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder().name("max-self-damage").defaultValue(12.5).min(0).sliderMax(36).build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side color").defaultValue(new SettingColor(0, 255, 255, 50)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line color").defaultValue(new SettingColor(0, 255, 255, 255)).build());


    public AutoCrystal() {
        super(Addon.CATEGORY, "Auto-Crystal", "The most optimized autoCrystal");
    }

    @Override
    public void onActivate() {

    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        // event.renderer.box(pos.down(1), sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
    }
}