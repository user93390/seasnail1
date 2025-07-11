package dev.seasnail1.modules.misc;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.MathHelper;
import dev.seasnail1.utilities.SwapUtils;
import dev.seasnail1.utilities.WorldUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Arrays;
import java.util.stream.StreamSupport;

public class AutoExp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Double> pauseHealth = sgGeneral.add(new DoubleSetting.Builder().name("pause-health").description("Pauses when you go below a certain health.").defaultValue(5.0).sliderRange(0.0, 36.0).build());
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final Setting<Double> pitch = sgGeneral.add(new DoubleSetting.Builder().name("pitch").description("Pitch to look at when using XP bottles.").defaultValue(-90.0).sliderRange(-90.0, 90.0).build());
    private final Setting<Integer> rotationSteps = sgGeneral.add(new IntSetting.Builder().name("rotation-steps").description("The number of steps to rotate. 1 or 2 recommended.").defaultValue(1).sliderRange(1, 10).build());
    private final Setting<SwapUtils.swapMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<SwapUtils.swapMode>().name("swap-mode").description("Swapping method. IGNORE MOVE MODE.").defaultValue(SwapUtils.swapMode.silent).build());

    private final Setting<Integer> moveSlot = sgGeneral.add(new IntSetting.Builder().name("slot").description("the slot to move the xp to").defaultValue(0).sliderRange(0, 10).visible(() -> autoSwitch.get().equals(SwapUtils.swapMode.silent) || autoSwitch.get().equals(SwapUtils.swapMode.normal)).build());

    private final Setting<WorldUtils.HandMode> handSwing = sgGeneral.add(new EnumSetting.Builder<WorldUtils.HandMode>().name("swing").description("Swing method").defaultValue(WorldUtils.HandMode.MainHand).build());

    private final Setting<Integer> cooldownTime = sgGeneral.add(new IntSetting.Builder().name("cooldown-time").description("Cooldown time between using XP bottles (in ticks).").defaultValue(20).min(0).sliderMax(100).build());

    private final Setting<Integer> minXPBottles = sgGeneral.add(new IntSetting.Builder().name("min-xp-bottles").description("Minimum number of XP bottles required to continue automation.").defaultValue(1).min(1).sliderMax(64).build());

    private final Setting<Double> armorDurabilityThreshold = sgGeneral.add(new DoubleSetting.Builder().name("armor-durability-threshold").description("Durability threshold for armor before using XP bottles.").defaultValue(0.1).sliderRange(0.0, 1.0).build());

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder().name("auto-disable").description("Automatically disables the module full armour durability.").defaultValue(true).build());

    private final Setting<Boolean> damagePause = sgPause.add(new BoolSetting.Builder().name("damage-pause").description("Pause when taking damage.").defaultValue(false).build());

    private final Setting<Boolean> pauseUse = sgPause.add(new BoolSetting.Builder().name("pause-use").description("Pause using xp bottles when using other items.").defaultValue(false).build());
    float health = 0;
    private int slot = -1;
    private final Runnable interact = () -> {
        interact();
        mc.player.swingHand(WorldUtils.swingHand(handSwing.get()));
    };
    private FindItemResult item;
    private long lastUseTime = 0;
    private final Runnable reset = () -> {
        slot = -1;
        item = null;
        lastUseTime = 0;
    };

    public AutoExp() {
        super(Addon.CATEGORY, "armor-Repair", "Automatically interacts with xp bottles to repair armor");
    }

    @Override
    public void onActivate() {
        reset.run();
        health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            if (damagePause.get() && health > mc.player.getHealth() + mc.player.getAbsorptionAmount()) return;

            if (pauseUse.get() && mc.player.isUsingItem()) return;

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
            double time = 50;
            if (currentTime - lastUseTime < cooldownTime.get() * time) return;

            slot = item.slot();
            Rotations.rotate(mc.player.getYaw(), pitch.get(), 100, interact);
            MathHelper.updateRotation(rotationSteps.get());

            lastUseTime = currentTime;

            health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        } catch (Exception e) {
            error("An error occurred while using XP bottles");
            Addon.Logger.error("An error occurred while using XP bottles {}", Arrays.toString(e.getStackTrace()));
        }
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
                SwapUtils.pickSwitch(slot);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                SwapUtils.pickSwapBack();
            }

            case normal -> {
                InvUtils.move().from(slot).to(moveSlot.get() - 1);
                InvUtils.swap(slot, false);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }

            case Move -> {
                error("Move is not a valid option for this module");
                toggle();
            }

            case null, default -> {
                error("Invalid swap mode");
                toggle();
            }
        }
    }

    private boolean isArmorFullDurability() {
        return StreamSupport.stream(mc.player.getArmorItems().spliterator(), false).allMatch(itemStack -> itemStack.getDamage() == 0 || (double) itemStack.getDamage() / itemStack.getMaxDamage() > armorDurabilityThreshold.get());
    }
}