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

    private int slot = -1;
    private FindItemResult item;
    private long lastUseTime = 0;

    public autoXP() {
        super(Addon.Snail, "Auto-XP+", "Better auto-XP");
    }

    @Override
    public void onActivate() {
        slot = -1;
    }

    @Override
    public void onDeactivate() {
        slot = -1;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (isArmorFullDurability()) return;

        if (mc.player.getHealth() <= pauseHealth.get()) return;

        item = InvUtils.find(Items.EXPERIENCE_BOTTLE);
        if (!item.found() || item.count() < minXPBottles.get()) {
            error("Not enough XP bottles in inventory");
            toggle();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUseTime < cooldownTime.get() * 50) return;

        slot = item.slot();
        Rotations.rotate(mc.player.getYaw(), Rotations.getPitch(mc.player.getBlockPos().down(1)), this::interact);
        mc.player.swingHand(WorldUtils.swingHand(handSwing.get()));

        lastUseTime = currentTime;
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
        }
    }

    private boolean isArmorFullDurability() {
        return StreamSupport.stream(Objects.requireNonNull(mc.player).getArmorItems().spliterator(), false)
                .allMatch(itemStack -> itemStack.getDamage() == 0 || (double) itemStack.getDamage() / itemStack.getMaxDamage() > armorDurabilityThreshold.get());
    }
}