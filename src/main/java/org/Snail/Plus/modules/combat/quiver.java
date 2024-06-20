package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import org.Snail.Plus.Addon;

import java.util.List;

public class quiver extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private long lastPlaceTime = 0;

    private final Setting<Integer> minHealth = sgGeneral.add(new IntSetting.Builder()
            .name("min Health")
            .description("min health to shoot at")
            .defaultValue(10)
            .min(0)
            .max(36)
            .sliderMin(0)
            .sliderMax(36)
            .build());

    private final Setting<Double> Delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("pull down speed")
            .defaultValue(10)
            .range(0, 36)
            .sliderRange(0, 36)
            .build());

    private final Setting<List<StatusEffect>> Effects = sgGeneral.add(new StatusEffectListSetting.Builder()
            .name("effects")
            .description("Which effects to shoot you with.")
            .defaultValue(StatusEffects.SPEED.value())
            .build());

    private final Setting<Boolean> BetterSwap = sgGeneral.add(new BoolSetting.Builder()
            .name("better swap")
            .description("allows you to swap even if your bow is not in your hotbar. Patched on 1.20.4+ paper servers")
            .defaultValue(false)
            .build());

    private final Setting<Integer> Slot = sgGeneral.add(new IntSetting.Builder()
            .name("Slot")
            .description("The slot to quick swap to. Won't really matter sense the swap back delay is 0")
            .visible(BetterSwap::get)
            .sliderMin(1)
            .sliderMax(10)
            .build());

    public quiver() {
        super(Addon.Snail, "quiver+", "shoots positive effects at you");
    }

    public boolean usingBow = mc.options.useKey.isPressed();
    public FindItemResult Bow = InvUtils.find(Items.BOW);

    public void onTick(TickEvent.Pre event) {
        FindItemResult Arrows = InvUtils.find(Items.ARROW);

        if (Bow != null && Arrows != null && mc.player != null && !usingBow) {
            Bowdelay(Delay.get());
            mc.options.useKey.setPressed(true);

            usingBow = true;
        }
    }

    public void Bowdelay(double delay) {
        assert mc.player != null;
        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < delay * 1000) return;
        lastPlaceTime = time;

        if (BowItem.getPullProgress(mc.player.getItemUseTime()) == Delay.get()) {
            InvUtils.swap(Bow.slot(), false);

        } else if (BetterSwap.get() && BowItem.getPullProgress(mc.player.getItemUseTime()) == Delay.get()) {
            InvUtils.quickSwap().from(Bow.slot()).to(Slot.get());
            InvUtils.swap(Bow.slot(), false);
        } else {
        }
    }
} /*<----------- TODO: make it use tipped arrows -----------> */