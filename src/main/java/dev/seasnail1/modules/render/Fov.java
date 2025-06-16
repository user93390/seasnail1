package dev.seasnail1.modules.render;


import dev.seasnail1.Addon;
import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Fov extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> Fov = sgGeneral.add(new IntSetting.Builder().name("FOV").description("the fov").defaultValue(130).min(1).sliderMax(280).build());

    public Fov() {
        super(Addon.CATEGORY, "Fov+", "Allows you to change your FOV to a custom value.");
    }

    @EventHandler
    private void FOVModify(GetFovEvent event) {
        event.fov = Fov.get().floatValue();
    }
}