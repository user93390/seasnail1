package org.snail.plus.mixins;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.player.FakePlayer;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.snail.plus.utils.PlayerMovement;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
    @Final
    private SettingGroup sgGeneral;

    @Unique
    private final List<PlayerMovement> recordedMovements = new ArrayList<>();
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

        start.action = this::startRecording;
        stop.action = this::stopRecording;
        play.action = this::startLooping;

        button.add(info.getReturnValue());
        button.add(theme.horizontalSeparator()).expandX();
        button.add(wHorizontalList);

        info.setReturnValue(button);
    }

    @Unique
    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (FakePlayerEntity fakePlayer : FakePlayerManager.getFakePlayers()) {
            for(PlayerEntity FakePlayer : mc.world.getPlayers()) {
                if (FakePlayer == fakePlayer) {
                    if (recording) {
                        recordedMovements.add(new PlayerMovement(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch()));
                    }
                    if (looping && !recordedMovements.isEmpty()) {
                        PlayerMovement movement = recordedMovements.get(loopIndex);
                        FakePlayer.updatePosition(movement.x, movement.y, movement.z);
                        FakePlayer.setYaw(movement.yaw);
                        FakePlayer.setPitch(movement.pitch);
                        loopIndex = (loopIndex + 1) % recordedMovements.size();
                        if (loopIndex == 0) {
                            if (loop.get()) {
                                startLooping();
                            } else {
                                stopLooping();
                            }
                        }
                    }
                }
            }
        }
   }

    @Unique
    public void startRecording() {
        recordedMovements.clear();
        recording = true;
    }

    @Unique
    public void stopRecording() {
        recording = false;
    }

    @Unique
    public void startLooping() {
        looping = true;
        loopIndex = 0;
    }

    @Unique
    public void stopLooping() {
        looping = false;
    }
}