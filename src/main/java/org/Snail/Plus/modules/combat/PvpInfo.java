package org.Snail.Plus.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.Sound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.WorldUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PvpInfo extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> Logout = sgGeneral.add(new BoolSetting.Builder()
            .name("Logout")
            .description("alerts when a player combat logs")
            .defaultValue(false)
            .build());
    private final Setting<Double> logoutRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("logout range")
            .description("range for Logout")
            .defaultValue(3.0)
            .sliderMax(50.0)
            .sliderMin(1.0)
            .visible(Logout::get)
            .build());
    private final Setting<Boolean> TotemPop = sgGeneral.add(new BoolSetting.Builder()
            .name("totem pop")
            .description("alerts when a player pops a totem")
            .defaultValue(false)
            .build());
    private final Setting<Double> popRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("totem pop range")
            .description("the totem pop range")
            .defaultValue(3.0)
            .sliderMax(50.0)
            .sliderMin(1.0)
            .visible(TotemPop::get)
            .build());

    private final Setting<Boolean> Ran = sgGeneral.add(new BoolSetting.Builder()
            .name("ran away")
            .description("tells you if a player ran away from you're visual range")
            .defaultValue(false)
            .build());
    private final Setting<Double> ranDistance = sgGeneral.add(new DoubleSetting.Builder()
            .name("min ran distance")
            .description("if a player goes over this value, the player will be defined as 'ran away' ")
            .defaultValue(3.0)
            .sliderMax(50.0)
            .sliderMin(1.0)
            .visible(Ran::get)
            .build());

    private final Setting<Boolean> playSound = sgGeneral.add(new BoolSetting.Builder()
            .name("ran away sound")
            .description("plays a sound when the player has ran away")
            .defaultValue(false)
            .visible(Ran::get)
            .build());

    private final Setting<List<SoundEvent>> Sounds = sgGeneral.add(new SoundEventListSetting.Builder()
            .name("sounds")
            .description("Sounds to play when a player is spotted")
            .visible(playSound::get)
            .build());
    private final List<AbstractClientPlayerEntity> target = new ArrayList<>();

    public PvpInfo() {
        super(Addon.Snail, "PVP info", "useful info for crystal pvp");
    }

    @Override
    public void onActivate() {
        sentMessage = false;
        target.clear();
    }

    @Override
    public void onDeactivate() {
        sentMessage = false;
        target.clear();
    }

    private boolean sentMessage;
    @EventHandler
    private void ontick(TickEvent.Post event) {
        if (sentMessage) {
            return;
        }
        for (AbstractClientPlayerEntity player : Objects.requireNonNull(mc.world).getPlayers()) {
            if (target.contains(mc.player)) continue;
            double distance = player.distanceTo(mc.player);
            String playerUUID = player.getUuidAsString();

            if (distance < ranDistance.get() && playerUUID != null) {
                continue;
            } else {
                if (distance > ranDistance.get() && playerUUID != null) {
                    ChatUtils.sendMsg(Formatting.GREEN, WorldUtils.getName(player) + " has ran away...");
                    List<SoundEvent> soundEvents = Sounds.get();
                    if (playSound.get() && !Sounds.get().isEmpty()) {
                       WorldUtils.playSound(soundEvents.getFirst(), 1.0f);
                    }
                    sentMessage = true;
                }
            }
        }
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        for (AbstractClientPlayerEntity player : Objects.requireNonNull(mc.world).getPlayers()) {
            if (target.contains(mc.player)) continue;
            double distance = player.distanceTo(mc.player);
            String playerUUID = player.getUuidAsString();
            if (distance > logoutRange.get() && playerUUID != null) {
                continue;
            }
            if (event.packet instanceof PlayerRemoveS2CPacket) {
                if (distance <= logoutRange.get() && playerUUID != null) {
                    ChatUtils.sendMsg(Formatting.GREEN, WorldUtils.getName(player) + " has combat logged...");
                }
            }
        }
    }
}