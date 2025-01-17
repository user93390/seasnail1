package org.snail.plus.modules.render;


import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import org.snail.plus.Addon;

import java.util.Arrays;

public class FOV extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> Fov = sgGeneral.add(new IntSetting.Builder()
            .name("FOV")
            .description("the fov")
            .defaultValue(130)
            .min(1)
            .sliderMax(280)
            .build());

    public FOV() {
        super(Addon.Snail, "fov+", "Allows you to change your field of view");
    }

    @EventHandler
    private void FOVModify(GetFovEvent event) {
        try {
            event.fov = Fov.get();
        } catch (Exception e) {
            info("An error occurred while changing the FOV");
            Addon.LOGGER.error("An error occurred while changing the FOV  {}", Arrays.toString(e.getStackTrace()));
        }
    }
}