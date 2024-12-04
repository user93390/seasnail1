package org.snail.plus.mixins;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.FakePlayer;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.snail.plus.utils.movements.PlayerMovement;
import org.snail.plus.utils.WorldUtils;
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
    private final List<PlayerMovement> recordedMovements = new ArrayList<>();
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
            stopRecording();
            startRecording();
        };

        clear.action = () -> {
            stopRecording();
            recordedMovements.clear();
            stopLooping();
        };

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
        synchronized (this) {
            mc.execute(() -> {
                try {
                    for (FakePlayerEntity fakePlayer : FakePlayerManager.getFakePlayers()) {
                        for (PlayerEntity fakePlayerEntity : mc.world.getPlayers()) {
                            if (fakePlayerEntity == fakePlayer) {
                                if (recording) {
                                    recordedMovements.add(new PlayerMovement(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch()));
                                }
                                fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 9999, 2));
                                fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 9999, 4));
                                fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 9999, 1));

                                if (looping && !recordedMovements.isEmpty()) {
                                    PlayerMovement movement = recordedMovements.get(loopIndex);
                                    fakePlayerEntity.updatePosition(movement.x, movement.y, movement.z);
                                    fakePlayerEntity.setVelocity(mc.player.getVelocity().x, mc.player.getVelocity().y, mc.player.getVelocity().z);

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
                } catch (Exception e) {
                    ChatUtils.error("An error occurred while playing the recording.");
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Unique
    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        mc.execute(() -> {
            if (event.packet instanceof ExplosionS2CPacket packet ) {
                for (FakePlayerEntity fakePlayer : FakePlayerManager.getFakePlayers()) {
                    for (PlayerEntity fakePlayerEntity : mc.world.getPlayers()) {
                        float damage = DamageUtils.crystalDamage(fakePlayerEntity, new Vec3d(packet.getX(), packet.getY(), packet.getZ()));
                        if (damage > 0.0F && fakePlayerEntity.timeUntilRegen <= 10) {
                            fakePlayerEntity.hurtTime = 10;
                            fakePlayerEntity.timeUntilRegen = 20;
                            WorldUtils.playSound(SoundEvents.ENTITY_PLAYER_HURT, 1.0F);
                            fakePlayer.setHealth(fakePlayer.getHealth() - damage);

                            if(fakePlayer.getHealth() < Modules.get().get(FakePlayer.class).health.get()) {
                                fakePlayer.setHealth(Modules.get().get(FakePlayer.class).health.get());
                            }
                        }
                    }
                }
            }
        });
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