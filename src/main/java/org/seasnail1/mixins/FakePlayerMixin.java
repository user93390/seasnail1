package org.seasnail1.mixins;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.player.FakePlayer;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.seasnail1.utilities.events.PlayerMoveEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(FakePlayer.class)
public class FakePlayerMixin {
    @Unique
    private final List<PlayerMoveEvent> recordedMovements = new ArrayList<>();
    @Unique
    @Final
    public Setting<String> name;

    @Final
    private SettingGroup sgGeneral;
    @Unique
    private Setting<Boolean> loop = null;

    @Unique
    private boolean recording = false;
    @Unique
    private boolean looping = false;
    @Unique
    private int loopIndex = 0;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        loop = sgGeneral.add(new BoolSetting.Builder()
                .name("loop")
                .description("Whether to loop the recorded movement after playing.")
                .defaultValue(true)
                .build());
    }

    @Inject(method = "getWidget", at = @At("RETURN"), cancellable = true, remap = false)
    private void onGetWidget(GuiTheme theme, CallbackInfoReturnable<WWidget> info) {
        WVerticalList button = theme.verticalList();
        WHorizontalList wHorizontalList = theme.horizontalList();

        WButton start = wHorizontalList.add(theme.button("Start Recording")).widget();
        WButton stop = wHorizontalList.add(theme.button("Stop Recording")).widget();
        WButton play = wHorizontalList.add(theme.button("Play Recording")).widget();
        WButton clear = wHorizontalList.add(theme.button("Clear all recordings")).widget();

        start.action = () -> {
            seasnail1$stopRecording();
            seasnail1$startRecording();
        };

        clear.action = () -> {
            seasnail1$stopRecording();
            seasnail1$stopLooping();
            recordedMovements.clear();
        };

        stop.action = this::seasnail1$stopRecording;
        play.action = this::seasnail1$startLooping;

        button.add(info.getReturnValue());
        button.add(theme.horizontalSeparator()).expandX();
        button.add(wHorizontalList);

        info.setReturnValue(button);
    }

    @Unique
    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            for (FakePlayerEntity fakePlayer : FakePlayerManager.getFakePlayers()) {
                for (PlayerEntity fakePlayerEntity : mc.world.getPlayers()) {
                    if (fakePlayerEntity != fakePlayer) continue;

                    if (recording) {
                        recordedMovements.add(new PlayerMoveEvent(
                                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                mc.player.getYaw(), mc.player.getPitch(),
                                mc.player.getMovementDirection(), mc.player.limbAnimator.getSpeed(),
                                mc.player.getHeadYaw(), mc.player.getBodyYaw()
                        ));
                    }

                    if (looping && !recordedMovements.isEmpty()) {
                        PlayerMoveEvent movement = recordedMovements.get(loopIndex);
                        PlayerMoveEvent nextMovement = recordedMovements.get((loopIndex + 1) % recordedMovements.size());

                        double t = (double) loopIndex / recordedMovements.size();
                        double interpolatedX = movement.x + t * (nextMovement.x - movement.x);
                        double interpolatedY = movement.y + t * (nextMovement.y - movement.y);
                        double interpolatedZ = movement.z + t * (nextMovement.z - movement.z);

                        fakePlayerEntity.updatePosition(interpolatedX, interpolatedY, interpolatedZ);
                        fakePlayerEntity.setAngles(movement.yaw, movement.pitch);
                        fakePlayerEntity.setYaw(movement.yaw);
                        fakePlayerEntity.setPitch(movement.pitch);

                        float interpolatedLimbSpeed = (float) (movement.limbSpeed + t * (nextMovement.limbSpeed - movement.limbSpeed));
                        float scale = 1.0f;
                        fakePlayerEntity.limbAnimator.updateLimbs(interpolatedLimbSpeed, 1, scale);
                        fakePlayerEntity.headYaw = movement.headYaw;
                        fakePlayerEntity.bodyYaw = movement.bodyYaw;

                        loopIndex = (loopIndex + 1) % recordedMovements.size();

                        if (loopIndex == 0) {
                            if (loop.get()) {
                                seasnail1$startLooping();
                            } else {
                                seasnail1$stopLooping();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            ChatUtils.error("An error occurred while playing the recording.");
            throw new RuntimeException(e);
        }
    }

    @Unique
    public void seasnail1$startRecording() {
        recordedMovements.clear();
        recording = true;
    }

    @Unique
    public void seasnail1$stopRecording() {
        recording = false;
    }

    @Unique
    public void seasnail1$startLooping() {
        looping = true;
        loopIndex = 0;
    }

    @Unique
    public void seasnail1$stopLooping() {
        looping = false;
    }
}