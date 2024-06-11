package org.Snail.Plus.modules.misc;

import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.TPSSyncUtil;
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

    public enum HandMode {
        Offhand,
        MainHand,
        Packet,
        none
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

    private final Setting<Integer> pitch = sgGeneral.add(new IntSetting.Builder()
            .name("pitch")
            .description("Where to set pitch.")
            .defaultValue(90)
            .range(-90, 90)
            .sliderMax(90)
            .visible(() -> rotate.get() == RotateMode.Pitch)
            .build());

    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("Swings your hand (won't really change anything)")
            .defaultValue(false)
            .build());

    private final Setting<HandMode> handSwing = sgGeneral.add(new EnumSetting.Builder<HandMode>()
            .name("swing")
            .description("Swing method")
            .defaultValue(HandMode.MainHand)
            .build());

    private long lastPlaceTime = 0;
    private int originalSlot = -1;

    public XPautomation() {
        super(Addon.Snail, "Auto-XP+", "Better auto-XP");
    }

    @Override
    public void onActivate() {
        checkArmorDurability();
        moveXPBottleToHotbar();
    }

    @Override
    public void onDeactivate() {
        returnXPBottleToOriginalSlot();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (isArmorFullDurability() && smartMode.get()) {
            ChatUtils.info("Your armor is at full HP, disabling...");
            toggle();
            return;
        }

        if (mc.player.getHealth() <= health.get()) {
            return;
        }

        double currentDelay = sync.get() ? 1.0 / TPSSyncUtil.getCurrentTPS() : delay.get();
        long time = System.currentTimeMillis();

        if ((time - lastPlaceTime) < currentDelay * 1000) return;
        lastPlaceTime = time;

        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);
        if (!exp.found() || !exp.isHotbar()) {
            replenish();
            return;
        }

        switch (rotate.get()) {
            case Pitch:
                mc.player.setPitch(pitch.get());
                break;
            case Packet:
                Rotations.rotate(mc.player.getYaw(), 90);
                break;
            default:
                break;
        }

        if (autoSwitch.get() == AutoSwitchMode.Normal) {
            useXPBottle(exp);
        } else if (autoSwitch.get() == AutoSwitchMode.Silent) {
            silentUseXPBottle(exp);
        }
    }

    private void useXPBottle(FindItemResult exp) {
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        swingHand();
    }

    private void silentUseXPBottle(FindItemResult exp) {
        InvUtils.swap(exp.slot(), true);
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        swingHand();
        InvUtils.swapBack();
    }

    private void swingHand() {
        if (swing.get()) {
            switch (handSwing.get()) {
                case MainHand:
                    mc.player.swingHand(Hand.MAIN_HAND);
                    break;
                case Offhand:
                    mc.player.swingHand(Hand.OFF_HAND);
                    break;
                case Packet:
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    break;
                case none:
                    break;
                default:
                    break;
            }
        }
    }

    private void moveXPBottleToHotbar() {
        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);
        if (exp.found() && !exp.isHotbar() && !exp.isOffhand()) {
            originalSlot = exp.slot();
            InvUtils.move().from(exp.slot()).toHotbar(slot.get() - 1);
        }
    }

    private void returnXPBottleToOriginalSlot() {
        if (originalSlot != -1) {
            FindItemResult exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
            if (exp.found()) {
                InvUtils.move().from(exp.slot()).to(originalSlot);
                originalSlot = -1;
            }
        }
    }

    private void checkArmorDurability() {
        if (smartMode.get() && isArmorFullDurability()) {
            ChatUtils.info("Your armor is at full HP, disabling...");
            toggle();
        }
    }

    private boolean isArmorFullDurability() {
        return StreamSupport.stream(mc.player.getArmorItems().spliterator(), false)
                .allMatch(itemStack -> itemStack.getDamage() == 0);
    }

    private void replenish() {
        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);
        if (exp.found() && !exp.isHotbar()) {
            originalSlot = exp.slot();
            InvUtils.move().from(exp.slot()).toHotbar(slot.get() - 1);
        }
    }
}
