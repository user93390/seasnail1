package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.Snail.Plus.Addon;

public class PistonPush extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private final Setting<Double> Range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("the range to place pistons and redstone")
            .defaultValue(4.0)
            .sliderMax(10.0)
            .sliderMin(1.0)
            .build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Automatically rotates towards the position where pistons are getting placed")
            .defaultValue(true)
            .build());

    public PistonPush() {
        super(Addon.Snail, "piston pusher", "");
    }
}