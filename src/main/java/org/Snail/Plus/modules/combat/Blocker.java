package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.Snail.Plus.Addon;

public class Blocker extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> Rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("Rotate")
            .description("If the module should rotate")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> attack = sgGeneral.add(new BoolSetting.Builder()
            .name("attack crystals")
            .description("attacks crystals blocking you're surround")
            .defaultValue(true)
            .build());
    private final Setting<Double> AttackDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("attack delay")
            .description("attack delay")
            .defaultValue(0.250)
            .sliderMin(0.0)
            .sliderMax(1)
            .visible(attack::get)
            .build());

    private final Setting<Boolean> Place = sgGeneral.add(new BoolSetting.Builder()
            .name("Place")
            .description("if the module should attack crystals")
            .defaultValue(true)
            .build());
    private final Setting<Double> PlaceDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("Place delay")
            .description("Place delay")
            .defaultValue(0.250)
            .sliderMin(0.0)
            .sliderMax(1)
            .visible(Place::get)
            .build());

    private final Setting<Boolean> Render = sgGeneral.add(new BoolSetting.Builder()
            .name("Render")
            .description("renders the placed positions")
            .defaultValue(true)
            .build());
    private final Setting<Integer> rendertime = sgGeneral.add(new IntSetting.Builder()
            .name("render time")
            .description("render time")
            .defaultValue(3)
            .sliderMax(100)
            .sliderMin(1)
            .visible(Render::get)
            .build());
    private final Setting<AutoTrap.RenderMode> renderMode = sgGeneral.add(new EnumSetting.Builder<AutoTrap.RenderMode>()
            .name("render mode")
            .description("render mode")
            .defaultValue(AutoTrap.RenderMode.normal)
            .visible(Render::get)
            .build());

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(Render::get)
            .build());

    public Blocker() {
        super(Addon.Snail, "Blocker", "defends you're surround using obsidian");
        }
    }