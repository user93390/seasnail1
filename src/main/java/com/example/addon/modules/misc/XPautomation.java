package com.example.addon.modules.misc;

import com.example.addon.Addon;
import com.example.addon.utils.TPSSyncUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import java.util.stream.StreamSupport;

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
            .build());

    private final Setting<RotateMode> rotate = sgGeneral.add(new EnumSetting.Builder<RotateMode>()
            .name("rotation")
            .description("Rotation method")
            .defaultValue(RotateMode.Packet)
            .build());

    public final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds.")
            .defaultValue(1.0)
            .range(0.0, 100.0)
            .sliderRange(0.0, 100.0)
            .build());

    private final Setting<Boolean> sync = sgGeneral.add(new BoolSetting.Builder()
            .name("tps-sync")
            .description("Sync the delay with server TPS")
            .defaultValue(false)
            .onChanged(enabled -> TPSSyncUtil.setSyncEnabled(enabled))
            .build());

    private final Setting<Boolean> smartMode = sgGeneral.add(new BoolSetting.Builder()
            .name("smart-mode")
            .description("Disables the module when armor is at max HP.")
            .defaultValue(false)
            .build());

    public final Setting<Double> health = sgGeneral.add(new DoubleSetting.Builder()
            .name("pause-health")
            .description("Pauses when you go below a certain health.")
            .defaultValue(5.0)
            .range(0.0, 36.0)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Integer> slot = sgGeneral.add(new IntSetting.Builder()
            .name("hotbar-slot")
            .description("Hotbar slot to swap XP bottle to before using.")
            .defaultValue(1)
            .min(1)
            .max(9)
            .sliderMin(1)
            .sliderMax(9)
            .build());

    private final Setting<Integer> Pitch = sgGeneral.add(new IntSetting.Builder()
            .name("pitch")
            .description("Where to set pitch.")
            .defaultValue(90)
            .range(-90, 90)
            .sliderMax(90)
            .visible(() -> rotate.get() == RotateMode.Pitch)
            .build());

            private final Setting<Boolean> Swing = sgGeneral.add(new BoolSetting.Builder()
            .name("Swing hand")
            .description("Swings your hand (won't really change anything)")
            .defaultValue(false)
            .build());

    private long lastPlaceTime = 0;
    private int originalSlot = -1;

    public XPautomation() {
        super(Addon.MISC, "Auto-XP+", "Better auto-XP");
    }

    @Override
    public void onActivate() {
        if (smartMode.get() && isArmorFullDurability()) {
            ChatUtils.info("Your armor is at full HP, disabling...");
            toggle();
        }

        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);
        if (exp.found() && !exp.isHotbar() && !exp.isOffhand()) {
            originalSlot = exp.slot();
            InvUtils.move().from(exp.slot()).toHotbar(slot.get() - 1);
        }
    }

    @Override
    public void onDeactivate() {
        if (originalSlot != -1) {
            FindItemResult exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
            if (exp.found()) {
                InvUtils.move().from(exp.slot()).to(originalSlot);
                originalSlot = -1;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        double currentDelay = sync.get() ? 1.0 / TPSSyncUtil.getCurrentTPS() : delay.get();
        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);

        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < currentDelay * 1000)
            return;

        lastPlaceTime = time;
       


        if (autoSwitch.get() == AutoSwitchMode.Normal) {
            if (exp.found()) {
                if (exp.isHotbar()) {
                    if (rotate.get() == RotateMode.Pitch) {
                        mc.player.setPitch(Pitch.get());
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        if(Swing.get()) {
                            mc.player.swingHand(Hand.MAIN_HAND);
                        }
                    } else if (rotate.get() == RotateMode.Packet) {
                        Rotations.rotate(mc.player.getYaw(), 90, () -> mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND));
                        if(Swing.get()) {
                            mc.player.swingHand(Hand.MAIN_HAND);
                        }
                    
                    } else {
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        if(Swing.get()) {
                            mc.player.swingHand(Hand.MAIN_HAND);
                        }
                        
                    }
                }
            }
        } else if (autoSwitch.get() == AutoSwitchMode.Silent) {
            if (exp.found()) {
                if (exp.isHotbar()) {
                    if (rotate.get() == RotateMode.Pitch) {
                        mc.player.setPitch(Pitch.get());
                        InvUtils.swap(exp.slot(), true);
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        if(Swing.get()) {
                            mc.player.swingHand(Hand.MAIN_HAND);
                        }
                        InvUtils.swapBack();
                    } else if (rotate.get() == RotateMode.Packet) {
                        Rotations.rotate(mc.player.getYaw(), 90, () -> {
                            InvUtils.swap(exp.slot(), true);
                            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                            if(Swing.get()) {
                                mc.player.swingHand(Hand.MAIN_HAND);
                            }
                            InvUtils.swapBack();
                        });
                    } else {
                        InvUtils.swap(exp.slot(), true);
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        if(Swing.get()) {
                            mc.player.swingHand(Hand.MAIN_HAND);
                        }
                        InvUtils.swapBack();
                    }
                }
            }
        }


    }

    private boolean isArmorFullDurability() {
        return StreamSupport.stream(mc.player.getArmorItems().spliterator(), false)
                .allMatch(itemStack -> itemStack.getDamage() == 0);
    }
}
