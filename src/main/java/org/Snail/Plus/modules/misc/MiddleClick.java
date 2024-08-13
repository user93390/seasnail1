package org.snail.plus.modules.misc;

import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import org.snail.plus.Addon;
import org.snail.plus.utils.SwapUtils;

import java.util.Objects;

import static org.snail.plus.modules.misc.MiddleClick.Mode.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

public class MiddleClick extends Module {

    public enum Mode {
        Pearl,
        chorusFruit,
        FireWork,
        Snowball,
        xp,
    }

    public enum SwapMethod {
        silent,
        normal,
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<SwapMethod> swapMethod = sgGeneral.add(new EnumSetting.Builder<SwapMethod>()
            .name("swap method")
            .description("Method used to swap items")
            .defaultValue(SwapMethod.silent)
            .build());
    private final Setting<Mode> whenFlying = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("when flying")
            .description("What item to use when flying")
            .defaultValue(Mode.FireWork)
            .build());
    private final Setting<Mode> lookingAir = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("when looking at air")
            .description("What item to use when looking at air")
            .defaultValue(Pearl)
            .build());
    private final Setting<Mode> lookingGround = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("when looking at ground")
            .description("What item to use when looking at ground")
            .defaultValue(Mode.xp)
            .build());

    private boolean middleClickPressed = false;

    public MiddleClick() {
        super(Addon.Snail, "middle-click+", "Allows you to use different items when you middle click");
    }

    @EventHandler
    public void onMouseButton(MouseButtonEvent event) {
        if (event.button == GLFW_MOUSE_BUTTON_MIDDLE) {
            if (event.action == KeyAction.Press) {
                middleClickPressed = true;
            } else if (event.action == KeyAction.Release) {
                middleClickPressed = false;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (!middleClickPressed || mc.currentScreen != null) return;

        PlayerEntity player = mc.player;
        if (Objects.requireNonNull(player).isAlive() && !player.isSpectator()) {
            HitResult hitResult = player.raycast(20, 0, true);

            if (player.getAbilities().flying) {
                swapAndUse(whenFlying.get());
            } else if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                Direction side = blockHitResult.getSide();

                if (side == Direction.UP || side == Direction.DOWN) {
                    swapAndUse(lookingGround.get());
                } else {
                    swapAndUse(lookingAir.get());
                }
            } else if (hitResult.getType() == HitResult.Type.MISS) {
                swapAndUse(lookingAir.get());
            }
        }
    }

    private void swapAndUse(Mode mode) {
        FindItemResult xp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
        FindItemResult firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        FindItemResult chorus = InvUtils.findInHotbar(Items.CHORUS_FRUIT);
        FindItemResult pearl = InvUtils.findInHotbar(Items.ENDER_PEARL);
        FindItemResult snow = InvUtils.findInHotbar(Items.SNOWBALL);

        switch (mode) {
            case xp:
                swapAndInteract(xp);
                break;
            case FireWork:
                swapAndInteract(firework);
                break;
            case chorusFruit:
                swapAndInteract(chorus);
                break;
            case Pearl:
                swapAndInteract(pearl);
                break;
            case Snowball:
                swapAndInteract(snow);
                break;
        }
    }

    private void swapAndInteract(FindItemResult item) {
        int selectedSlot = Objects.requireNonNull(mc.player).getInventory().selectedSlot;
        switch (swapMethod.get()) {

            case silent:
                SwapUtils.SilentSwap(item.slot(), 1.0);
                Objects.requireNonNull(mc.interactionManager).interactItem(mc.player, Hand.MAIN_HAND);
                InvUtils.swapBack();
                break;
            case normal:
                SwapUtils.Normal(item.slot(), 1.0F);
                Objects.requireNonNull(mc.interactionManager).interactItem(mc.player, Hand.MAIN_HAND);
                break;
        }
    }
}
