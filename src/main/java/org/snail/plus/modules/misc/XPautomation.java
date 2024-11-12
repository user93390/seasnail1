package org.snail.plus.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;
import org.snail.plus.utils.WorldUtils;
import org.snail.plus.utils.swapUtils;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.stream.StreamSupport;

public class XPautomation extends Module {
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

    private final Setting<Boolean> sync = sgGeneral.add(new BoolSetting.Builder()
            .name("tps-sync")
            .description("Sync the delay with server TPS")
            .defaultValue(false)
            .build());

    public final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("how fast to interact with xp")
            .defaultValue(1.0)
            .sliderRange(0.0, 100.0)
            .visible(() -> !sync.get())
            .build());

    private final Setting<WorldUtils.HandMode> handSwing = sgGeneral.add(new EnumSetting.Builder<WorldUtils.HandMode>()
            .name("swing")
            .description("Swing method")
            .defaultValue(WorldUtils.HandMode.MainHand)
            .build());

    private final Setting<Boolean> smartRotate = sgGeneral.add(new BoolSetting.Builder()
            .name("smart-rotate")
            .description("rotates up to give you slightly more xp")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> autoMend = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-enable")
            .description("automatically enables when your armour is low")
            .defaultValue(false)
            .build());

    private final Setting<Double> durability = sgGeneral.add(new DoubleSetting.Builder()
            .name("durability")
            .description("the durability to enable auto-mend")
            .defaultValue(10.0)
            .sliderRange(0.0, 100.0)
            .visible(autoMend::get)
            .build());

    private final Setting<Boolean> smartPause = sgGeneral.add(new BoolSetting.Builder()
            .name("smart-pause")
            .description("pauses when you are in danger")
            .defaultValue(false)
            .visible(autoMend::get)
            .build());

    private final Setting<Boolean> onlyHole = sgGeneral.add(new BoolSetting.Builder()
            .name("only-hole")
            .description("only mends when you are in a hole")
            .defaultValue(false)
            .visible(autoMend::get)
            .build());

    private int slot = -1;
    private FindItemResult item;

    public XPautomation() {
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

        double yaw =   Rotations.getYaw(mc.player.getBlockPos().down(1));
        double pitch = smartRotate.get() ? Rotations.getPitch(mc.player.getBlockPos().up(2))
                : Rotations.getPitch(mc.player.getBlockPos().down(1));
        if (mc.player.getHealth() <= pauseHealth.get()) return;

        item = InvUtils.find(Items.EXPERIENCE_BOTTLE);
        slot = item.slot();

        if (autoMend.get() && shouldEnable()) {
            Rotations.rotate(yaw, pitch,  (() -> interact()));
        }

        Rotations.rotate(yaw, pitch,  (() -> interact()));

        mc.player.swingHand(WorldUtils.swingHand(handSwing.get()));

        if (!item.found()) {
            error("No XP bottles in inventory");
            toggle();
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

    public boolean shouldEnable() {
        DefaultedList<ItemStack> armour = mc.player.getInventory().armor;

        double totalDurability = 0;

        for (ItemStack itemStack : armour) {
            totalDurability += itemStack.getMaxDamage() - itemStack.getDamage();

            if(totalDurability <= durability.get()) {
                if (smartPause.get() ) {

                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isArmorFullDurability() {
        return StreamSupport.stream(Objects.requireNonNull(mc.player).getArmorItems().spliterator(), false)
                .allMatch(itemStack -> itemStack.getDamage() == 0);
    }
}