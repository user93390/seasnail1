package org.snail.plus.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.util.Hand;
import org.snail.plus.Addon;
import org.snail.plus.utils.TPSSyncUtil;
import org.snail.plus.utils.WorldUtils;
import org.snail.plus.utils.swapUtils;

import java.util.Objects;
import java.util.stream.StreamSupport;

public class XPautomation extends Module {



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

    private final Setting<Boolean> clientSide = sgGeneral.add(new BoolSetting.Builder()
            .name("client side")
            .description("only rotates client side")
            .defaultValue(false)
            .visible(() -> rotate.get() == RotateMode.Packet)
            .build());

    public final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds.")
            .defaultValue(1.0)
            .sliderRange(0.0, 100.0)
            .build());

    private final Setting<Boolean> sync = sgGeneral.add(new BoolSetting.Builder()
            .name("tps-sync")
            .description("Sync the delay with server TPS")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> smartMode = sgGeneral.add(new BoolSetting.Builder()
            .name("smart-mode")
            .description("Uses smart calculations")
            .defaultValue(false)
            .build());

    public final Setting<Double> health = sgGeneral.add(new DoubleSetting.Builder()
            .name("pause-health")
            .description("Pauses when you go below a certain health.")
            .defaultValue(5.0)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<Integer> slot = sgGeneral.add(new IntSetting.Builder()
            .name("hotbar-slot")
            .description("Hotbar slot to swap XP bottle to before using.")
            .defaultValue(1)
            .sliderRange(1, 9)
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

    private final Setting<WorldUtils.HandMode> handSwing = sgGeneral.add(new EnumSetting.Builder<WorldUtils.HandMode>()
            .name("swing")
            .description("Swing method")
            .defaultValue(WorldUtils.HandMode.MainHand)
            .visible(() -> swing.get())
            .build());

    private long lastPlaceTime = 0;
    private int originalSlot = -1;

    public XPautomation() {
        super(Addon.Snail, "Auto-XP+", "Better auto-XP");
    }

    @Override
    public void onActivate() {
        checkArmorDurability();
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
        if (Objects.requireNonNull(mc.player).getHealth() <= health.get()) return;

        double currentDelay = sync.get() ? 1.0 / TPSSyncUtil.getTPS() : delay.get();
        long time = System.currentTimeMillis();

        if ((time - lastPlaceTime) < currentDelay * 1000) return;
        lastPlaceTime = time;
        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);

        if (exp.found()) useXP(exp);

        switch (rotate.get()) {
            case Pitch:
                mc.player.setPitch(pitch.get());
                break;
            case Packet:
                break;
            default:
                break;
        }
    }

    private void moveXPBottleToHotbar(FindItemResult exp) {
        if (exp.found() && !exp.isHotbar() && !exp.isOffhand()) {
            originalSlot = exp.slot();
            InvUtils.move().from(exp.slot()).toHotbar(slot.get());
        }
    }

    private void useXP(FindItemResult exp) {
        if (!exp.found()) return;
        switch (autoSwitch.get()) {
            case Silent:
                moveXPBottleToHotbar(exp);
                InvUtils.swap(slot.get(), true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                InvUtils.swapBack();
                if (!exp.found()) info("No XP bottles found in inventory.");
                break;
            case Normal:
                InvUtils.swap(slot.get(), false);
                mc.interactionManager.interactItem(mc.player, WorldUtils.swingHand(handSwing.get()));
                if (!exp.isHotbar()) reFill();
                if (!exp.found()) info("No XP bottles found in inventory.");
                break;
            case inventory:
                swapUtils.pickSwitch(exp.slot());
                mc.interactionManager.interactItem(mc.player, WorldUtils.swingHand(handSwing.get()));
                swapUtils.pickSwapBack();
                break;
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
            info("Your armor is at full HP, disabling...");
            toggle();
        }
    }

    private boolean isArmorFullDurability() {
        return StreamSupport.stream(Objects.requireNonNull(mc.player).getArmorItems().spliterator(), false)
                .allMatch(itemStack -> itemStack.getDamage() == 0);
    }

    private void reFill() {
        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);
        if (exp.found() && !exp.isHotbar()) {
            originalSlot = exp.slot();
            InvUtils.move().from(exp.slot()).toHotbar(slot.get() - 1);
        }
    }

    public enum AutoSwitchMode {inventory, Silent, Normal}

    public enum RotateMode {Packet, Pitch, None}

}