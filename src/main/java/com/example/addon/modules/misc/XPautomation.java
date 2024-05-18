package com.example.addon.modules.misc;

import com.example.addon.Addon;
import com.example.addon.utils.TPSSyncUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class XPautomation extends Module {

    public enum AutoSwitchMode {
        Silent,
        Normal
    }

    public enum RotateMode {
        Packet,
        Pitch,
        None,
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<AutoSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>()
        .name("swap-mode")
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
        .description("Rotation method")
        .defaultValue(RotateMode.Packet)
        .build()
    );

    public final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("delay")
        .description("Delay in seconds.")
        .defaultValue(1.0)
        .range(0.0, 100.0)
        .sliderRange(0.0, 100.0)
        .build()
    );

    private final Setting<Boolean> sync = sgGeneral.add(new BoolSetting.Builder()
        .name("tps-sync")
        .description("Sync the delay with server TPS")
        .defaultValue(false)
        .onChanged(enabled -> TPSSyncUtil.setSyncEnabled(enabled))
        .build()
    );

    public final Setting<Double> health = sgGeneral.add(new DoubleSetting.Builder()
        .name("pause-health")
        .description("Pauses when you go below a certain health.")
        .defaultValue(5.0)
        .range(0.0, 36.0)
        .sliderRange(0.0, 36.0)
        .build()
    );

    private final Setting<Boolean> multitask = sgGeneral.add(new BoolSetting.Builder()
        .name("multi-task")
        .description("Allows you to mine and eat at the same time while throwing xp down (may not work on some servers)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> pauseWhenMining = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-mine")
        .description("Pauses when mining")
        .defaultValue(true)
        .visible(multitask::get)
        .build()
    );

    private final Setting<Boolean> pauseWhenEating = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses when eating")
        .defaultValue(true)
        .visible(multitask::get)
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

    private final Setting<Integer> Pitch = sgGeneral.add(new IntSetting.Builder()
        .name("pitch")
        .description("Where to set pitch.")
        .defaultValue(90)
        .range(-90, 90)
        .sliderMax(90)
        .visible(() -> rotate.get() == RotateMode.Pitch)
        .build()
    );

    private long lastPlaceTime = 0;

    public XPautomation() {
        super(Addon.MISC, "Auto-XP+", "Better auto-XP");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        double currentDelay = sync.get() ? 1.0 / TPSSyncUtil.getCurrentTPS() : delay.get();
        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);
        
        long time = System.currentTimeMillis();
        
        if ((time - lastPlaceTime) < currentDelay * 1000) return;

        lastPlaceTime = time;
        
        if (exp.found()) {
            if (!exp.isHotbar() && !exp.isOffhand()) {
                InvUtils.move().from(exp.slot()).toHotbar(slot.get() - 1);
            }
            if(exp.isHotbar()) {
                InvUtils.find(Items.EXPERIENCE_BOTTLE);
            }
                    if (rotate.get() == RotateMode.Pitch) {
                        mc.player.setPitch(Pitch.get());
                        if (exp.getHand() != null) {
                            mc.interactionManager.interactItem(mc.player, exp.getHand());
                        }
                        InvUtils.swap(exp.slot(), true);
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        InvUtils.swapBack();
                    }

                    if (rotate.get() == RotateMode.Packet) {
                        Rotations.rotate(mc.player.getYaw(), 90, () -> {
                            if (exp.getHand() != null) {
                                mc.interactionManager.interactItem(mc.player, exp.getHand());
                            }
                            InvUtils.swap(exp.slot(), true);
                            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                            InvUtils.swapBack();
                        });
                    }

                    if (rotate.get() == RotateMode.None) {
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