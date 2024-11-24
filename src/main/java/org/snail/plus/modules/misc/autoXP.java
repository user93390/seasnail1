package org.snail.plus.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;
import org.snail.plus.utils.swapUtils;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.StreamSupport;

public class autoXP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> pauseHealth = sgGeneral.add(new DoubleSetting.Builder()
            .name("pause-health")
            .description("Pauses when you go below a certain health.")
            .defaultValue(5.0)
            .sliderRange(0.0, 36.0)
            .build());

    private final Setting<swapUtils.swapMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<swapUtils.swapMode>()
            .name("swap-mode")
            .description("Swapping method")
            .defaultValue(swapUtils.swapMode.silent)
            .build());

    private final Setting<Integer> moveSlot = sgGeneral.add(new IntSetting.Builder()
            .name("slot")
            .description("the slot to move the xp to")
            .defaultValue(0)
            .min(0)
            .sliderMax(10)
            .visible(() -> autoSwitch.get().equals(swapUtils.swapMode.silent) || autoSwitch.get().equals(swapUtils.swapMode.normal))
            .build());

    private final Setting<WorldUtils.HandMode> handSwing = sgGeneral.add(new EnumSetting.Builder<WorldUtils.HandMode>()
            .name("swing")
            .description("Swing method")
            .defaultValue(WorldUtils.HandMode.MainHand)
            .build());

    private final Setting<Integer> cooldownTime = sgGeneral.add(new IntSetting.Builder()
            .name("cooldown-time")
            .description("Cooldown time between using XP bottles (in ticks).")
            .defaultValue(20)
            .min(0)
            .sliderMax(100)
            .build());

    private final Setting<Integer> minXPBottles = sgGeneral.add(new IntSetting.Builder()
            .name("min-xp-bottles")
            .description("Minimum number of XP bottles required to continue automation.")
            .defaultValue(1)
            .min(1)
            .sliderMax(64)
            .build());

    private final Setting<Double> armorDurabilityThreshold = sgGeneral.add(new DoubleSetting.Builder()
            .name("armor-durability-threshold")
            .description("Durability threshold for armor before using XP bottles.")
            .defaultValue(0.1)
            .sliderRange(0.0, 1.0)
            .build());

     private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Automatically disables the module full armour durability.")
            .defaultValue(true)
            .build());

    private int slot = -1;
    private FindItemResult item;
    private long lastUseTime = 0;;

    private final Runnable reset = () -> {
        slot = -1;
        item = null;
        lastUseTime = 0;
    };

    private final Runnable interact = () -> {
        Rotations.rotate(mc.player.getYaw(), Rotations.getPitch(mc.player.getBlockPos().down(1)), this::interact);
        mc.player.swingHand(WorldUtils.swingHand(handSwing.get()));
    };

    public autoXP() {
        super(Addon.Snail, "Auto-XP+", "Automatically interacts with xp bottles to repair armour");
    }

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        mc.executeTask(() -> {
            if (autoDisable.get() && isArmorFullDurability()) {
                toggle();
                return;
            }
            if (isArmorFullDurability() || mc.player.getHealth() <= pauseHealth.get()) return;

            item = InvUtils.find(Items.EXPERIENCE_BOTTLE);
            if (!item.found() || item.count() < minXPBottles.get()) {
                error("Not enough XP bottles in inventory");
                toggle();
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUseTime < cooldownTime.get() * 50) return;

            slot = item.slot();
            interact.run();
            lastUseTime = currentTime;
        });
    }

    public void interact() {
        switch (autoSwitch.get()) {
            case silent -> {
                InvUtils.move().from(slot).to(moveSlot.get() - 1);
                InvUtils.swap(slot, true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                InvUtils.swapBack();
            }
            case Inventory -> {
                swapUtils.pickSwitch(slot);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                swapUtils.pickSwapBack();
            }
            case normal -> {
                InvUtils.move().from(slot).to(moveSlot.get());
                InvUtils.swap(slot, false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
            case Move -> {
                swapUtils.moveSwitch(slot, moveSlot.get());
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                swapUtils.moveSwitch(moveSlot.get(), slot);
            }
        }
    }

    private boolean isArmorFullDurability() {
        return StreamSupport.stream(Objects.requireNonNull(mc.player).getArmorItems().spliterator(), false)
                .allMatch(itemStack -> itemStack.getDamage() == 0 || (double) itemStack.getDamage() / itemStack.getMaxDamage() > armorDurabilityThreshold.get());
    }
}