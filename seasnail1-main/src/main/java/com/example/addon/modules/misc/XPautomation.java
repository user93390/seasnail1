package com.example.addon.modules.misc;

import com.example.addon.Addon;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;

public class XPautomation extends Module {
    
    public enum AutoSwitchMode {
        Silent,
    }
    public enum RotateMode {
        packet,
        pitch,
        none,
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<AutoSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>()
            .name("swap mode")
            .description("Swapping method")
            .defaultValue(AutoSwitchMode.Silent)
            .build()
    );

    private final Setting<Boolean> auto = sgGeneral.add(new BoolSetting.Builder()
            .name("auto")
            .description("Automatically enables the module when your armor is under the armor threshold")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
            .name("threshold")
            .description("Armor threshold to xp at")
            .defaultValue(20)
            .visible(auto::get)
            .build()
    );

    private final Setting<RotateMode> rotate = sgGeneral.add(new EnumSetting.Builder<RotateMode>()
            .name("rotation")
            .description("what rotate method")
            .defaultValue(RotateMode.packet)
            .build()
    );

    private final Setting<Boolean> multitask = sgGeneral.add(new BoolSetting.Builder()
            .name("multi-task")
            .description("Allows you to mine and eat at the same time while throwing xp down (may not work on some servers)")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> pausewhenmining = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Pauses when mining")
            .defaultValue(true)
            .visible(multitask::get)
            .build()
    );

    private final Setting<Boolean> pausewheneating = sgGeneral.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses when eating")
            .defaultValue(true)
            .visible(multitask::get)
            .build()
    );

    public final Setting<Double> health = sgGeneral.add(new DoubleSetting.Builder()
            .name("pause-health")
            .description("Pauses when you go below a certain health.")
            .defaultValue(5)
            .range(0, 36)
            .sliderRange(0, 36)
            .build()
    );
    private final Setting<Integer> slot = sgGeneral.add(new IntSetting.Builder()
    .name("hotbar-slot")
    .description("Hotbar slot to swap XP bottle to before using.")
    .defaultValue(1)
    .min(1)
    .max(9)
    .sliderMin(1)
    .sliderMax(9)
    .build()
);

    private final Setting<Integer> pitch = sgGeneral.add(new IntSetting.Builder()
    .name("Pitch")
    .description("Where to set pitch.")
    .defaultValue(90)
    .range(-90, 90)
    .sliderMax(90)
    .visible(() -> rotate.get() == RotateMode.pitch)
    .build()
    );

    public XPautomation() {
        super(Addon.MISC, "AutoXP Bypass", "better auto-xp");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if(PlayerUtils.getTotalHealth() <= health.get()) {
            error("below health threshold, enable this when you are at the threshold");
            return;
        }

        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);

        if (exp.found()) {
            if (!exp.isHotbar() && !exp.isOffhand()) {
                InvUtils.move().from(exp.slot()).toHotbar(slot.get() - 1);
            }

            if (rotate.get() == RotateMode.pitch) {
                mc.player.setPitch(pitch.get());
                if (exp.getHand() != null) {
                    mc.interactionManager.interactItem(mc.player, exp.getHand());
                }
                InvUtils.swap(exp.slot(), true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                InvUtils.swapBack();
            }

            if (rotate.get() == RotateMode.packet) {
                Rotations.rotate(mc.player.getYaw(), 90, () -> {
                    if (exp.getHand() != null) {
                        mc.interactionManager.interactItem(mc.player, exp.getHand());
                    }
                    InvUtils.swap(exp.slot(), true);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    InvUtils.swapBack();
                });
            }

            if (rotate.get() == RotateMode.none) {
                if (exp.getHand() != null) {
                    mc.interactionManager.interactItem(mc.player, exp.getHand());
                }
                InvUtils.swap(exp.slot(), true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                InvUtils.swapBack();
            }
        }
    }
}
