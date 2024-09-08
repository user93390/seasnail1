package org.snail.plus.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.snail.plus.Addon;

public class AntiAim extends Module {
    SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<mode> Mode = sgGeneral.add(new EnumSetting.Builder<mode>()
            .name("mode")
            .description("The mode of the anti aim.")
            .defaultValue(mode.fake)
            .build());

    private final Setting<Double> yaw = sgGeneral.add(new DoubleSetting.Builder()
            .name("yaw")
            .description("The yaw to set your player to.")
            .defaultValue(0.0)
            .min(-180.0)
            .max(180.0)
            .sliderMin(-180.0)
            .sliderMax(180.0)
            .build());

    private final Setting<Double> pitch = sgGeneral.add(new DoubleSetting.Builder()
            .name("pitch")
            .description("The pitch to set your player to.")
            .defaultValue(0.0)
            .min(-90.0)
            .max(90.0)
            .sliderMin(-90.0)
            .sliderMax(90.0)
            .build());

    private final Setting<Item> item = sgGeneral.add(new ItemSetting.Builder()
            .name("item blacklist")
            .description("disables anti aim when holding this item")
            .defaultValue(Items.FIREWORK_ROCKET)
            .build());

    public AntiAim() {
        super(Addon.Snail, "Anti aim", "allows you to spoof rotations");
    }
    @EventHandler
    public void onTick(TickEvent.Post event) {
        float yaw = (float) this.yaw.get().doubleValue();
        float pitch = (float) this.pitch.get().doubleValue();
        if (mc.player == null) return;
        if (mc.player.getMainHandStack().getItem() == item.get() || mc.player.getOffHandStack().getItem() == item.get()) return;
        switch(Mode.get()) {
            case fake:
                Rotations.rotate(yaw, pitch);
                break;
            case real:
                mc.player.setYaw(yaw);
                mc.player.setPitch(pitch);
                break;

            case null, default:
                break;
        }
    }
    public enum mode {
        fake,
        real
    }
}
