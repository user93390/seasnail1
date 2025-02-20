package dev.seasnail1.modules.render;


import com.jcraft.jorbis.Block;
import dev.seasnail1.Addon;
import dev.seasnail1.utilities.MathHelper;
import meteordevelopment.meteorclient.events.render.GetFovEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;

import static dev.seasnail1.utilities.MathHelper.getHoles;

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
            event.fov = Fov.get().floatValue();
        } catch (Exception e) {
            info("An error occurred while changing the FOV");
            Addon.Logger.error("An error occurred while changing the FOV  {}", Arrays.toString(e.getStackTrace()));
        }
    }
}