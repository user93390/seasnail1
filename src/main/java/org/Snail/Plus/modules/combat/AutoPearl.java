package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.Snail.Plus.Addon;

import java.util.Objects;

public class AutoPearl extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private long delay = 0;
    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables the module when you have interaction with the pearl")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates towards the pearl when placing.")
            .defaultValue(true)
            .build());
    private final Setting<Double> Pitch = sgGeneral.add(new DoubleSetting.Builder()
            .name("pitch")
            .description("Pitch to rotate towards for the pearl")
            .defaultValue(0.0)
            .sliderMin(-90.0)
            .sliderMax(90.0)
            .visible(rotate::get)
            .build());
    private final Setting<Double> Delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("Delay")
            .description("Delay between actions")
            .defaultValue(0.0)
            .sliderMin(0.0)
            .sliderMax(10.0)
            .build());

    private final Setting<Boolean> Bypass = sgGeneral.add(new BoolSetting.Builder()
            .name("bypass")
            .description("Bypasses antiPhase plugins")
            .defaultValue(false)
            .build());
    private final Setting<Boolean> Debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .description("Debugs the module")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> BetterSwap = sgGeneral.add(new BoolSetting.Builder()
            .name("better swap")
            .description("uses better swapping methods")
            .defaultValue(true)
            .build());

    public AutoPearl() {
        super(Addon.Snail, "Auto Pearl+", "AutoPearl but with cc bypass");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - delay) < Delay.get() * 1000) return;
            delay = currentTime;
            FindItemResult BypassTools = InvUtils.find(Items.FLINT_AND_STEEL, Items.COBWEB);

            FindItemResult Pearl = InvUtils.find(Items.ENDER_PEARL);

            if (Bypass.get() && BypassTools.found()) {
                System.out.println("trying to bypass...");
                Bypass();
            }

            if (Pearl.found()) {
                if (rotate.get()) {
                    Rotations.rotate(Objects.requireNonNull(mc.player).getYaw(), Pitch.get());
                    InvUtils.swap(Pearl.slot(), true);
                    Objects.requireNonNull(mc.interactionManager).interactItem(mc.player, Hand.MAIN_HAND);
                    InvUtils.swapBack();
                    if (autoDisable.get()) {
                        toggle();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Exception caught");
        }
    }

    private void Bypass() {
        FindItemResult BypassTools = InvUtils.find(Items.FLINT_AND_STEEL, Items.COBWEB);
        Rotations.rotate(Objects.requireNonNull(mc.player).getYaw(), Pitch.get());
        InvUtils.swap(BypassTools.slot(), true);
        Objects.requireNonNull(mc.interactionManager).interactItem(mc.player, Hand.MAIN_HAND);
        InvUtils.swapBack();
    }
}