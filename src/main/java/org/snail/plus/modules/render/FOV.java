package org.snail.plus.modules.render;


import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import org.snail.plus.Addon;

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
        super(Addon.Snail, "fov+", "more control over fov");
    }
    @EventHandler
    private void FOVModify(GetFovEvent event) {
        event.fov = Fov.get();
        }
    }