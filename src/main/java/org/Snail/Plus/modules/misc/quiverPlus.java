package org.Snail.Plus.modules.misc;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.Snail.Plus.Addon;

public class quiverPlus extends Module {
    public quiverPlus() {
        super(Addon.Snail, "Snail Quiver", "Shoots arrows at yourself.");

        final SettingGroup sgGeneral = settings.getDefaultGroup();
        final SettingGroup sgArrows = settings.getDefaultGroup();

        final Setting<Integer> minHealth = sgGeneral.add(new IntSetting.Builder()
                .name("Health")
                .description("The minimum health required to shoot an arrow.")
                .defaultValue(10)
                .min(0)
                .max(36)
                .sliderMin(0)
                .sliderMax(36)
                .build());
        final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
                .name("cooldown")
                .description("How many ticks between shooting effects (19 minimum for NCP).")
                .defaultValue(10)
                .range(0, 36)
                .sliderRange(0, 36)
                .build());
    }
}
/*<----------- TODO: make this work -----------> */