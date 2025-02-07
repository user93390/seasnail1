package dev.seasnail1.modules.render;


import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import dev.seasnail1.Addon;

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
        super(Addon.CATEGORY, "fov-control", "Allows you to change your field of view");
    }

    @EventHandler
    private void FOVModify(GetFovEvent event) {
        try {
            event.fov = Fov.get();
        } catch (Exception e) {
            info("An error occurred while changing the FOV");
            Addon.Logger.error("An error occurred while changing the FOV  {}", Arrays.toString(e.getStackTrace()));
        }
    }
}