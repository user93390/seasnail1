package org.Snail.Plus.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import org.Snail.Plus.Addon;

import java.util.Objects;

public class MiddleClick extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> quickSwap = sgGeneral.add(new BoolSetting.Builder()
            .name("quick swap")
            .description("Uses a better swapping algorithm")
            .defaultValue(true)
            .build());
    private final Setting<Mode> whenFlying = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("when flying")
            .description("What item to use when flying")
            .defaultValue(Mode.FireWork)
            .build());
    private final Setting<Mode> lookingAir = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("when looking at air")
            .description("What item to use when looking at air")
            .defaultValue(Mode.Pearl)
            .build());
    private final Setting<Mode> lookingGround = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("when looking at ground")
            .description("What item to use when looking at ground")
            .defaultValue(Mode.xp)
            .build());

    public MiddleClick() {
        super(Addon.Snail, "middle-click+", "Allows you to use different items when you middle click");
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        FindItemResult xp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
        FindItemResult firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        FindItemResult chorus = InvUtils.findInHotbar(Items.CHORUS_FRUIT);
        FindItemResult pearl = InvUtils.findInHotbar(Items.ENDER_PEARL);
        FindItemResult snow = InvUtils.findInHotbar(Items.SNOWBALL);
        PlayerEntity player = mc.player;

        if (Objects.requireNonNull(player).isAlive() && !player.isSpectator()) {
            if (player.getAbilities().flying) {
                handleSwap(whenFlying.get(), xp, firework, chorus, pearl, snow);
            } else {
                HitResult hitResult = player.raycast(20, 0, true);

                if (hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                    Direction side = blockHitResult.getSide();

                    if (side == Direction.UP || side == Direction.DOWN) {
                        handleSwap(lookingGround.get(), xp, firework, chorus, pearl, snow);
                    } else {
                        handleSwap(lookingAir.get(), xp, firework, chorus, pearl, snow);
                    }
                } else if (hitResult.getType() == HitResult.Type.MISS) {
                    handleSwap(lookingAir.get(), xp, firework, chorus, pearl, snow);
                }
            }
        }
    }

    private void handleSwap(Mode mode, FindItemResult xp, FindItemResult firework, FindItemResult chorus, FindItemResult pearl, FindItemResult snow) {
        switch (mode) {
            case xp:
                InvUtils.swap(xp.slot(), true);
                break;
            case FireWork:
                InvUtils.swap(firework.slot(), true);
                break;
            case chorusFruit:
                InvUtils.swap(chorus.slot(), true);
                break;
            case Pearl:
                InvUtils.swap(pearl.slot(), true);
                break;
            case Snowball:
                InvUtils.swap(snow.slot(), true);
                break;
        }
    }

    public enum Mode {
        Pearl,
        chorusFruit,
        FireWork,
        Snowball,
        xp,
    }
}
