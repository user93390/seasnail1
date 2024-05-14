package com.example.addon.modules.misc;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import net.minecraft.text.Text;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.mixin.ClientPlayNetworkHandlerAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.MinecraftClient;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.MinecraftClient;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import com.example.addon.modules.misc.Notifications;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AntiBot extends Module {
    private final Setting<Boolean> always = settings.getDefaultGroup().add(new BoolSetting.Builder()
            .name("always")
            .description("Always blocks players.")
            .defaultValue(true)
            .build());

    private int bots = 0;
    private int targets = 0;

    public AntiBot() {
        super(Addon.MISC, "Anti-Bot", "No more bots... ♡ - seasnail1");
    }

    @Override
    public void onActivate() {
        bots = 0;
        targets = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!always.get()) return;

        for (PlayerEntity target : MinecraftClient.getInstance().world.getPlayers()) {
            double distance = target.distanceTo(MinecraftClient.getInstance().player);

            if (distance <= MinecraftClient.getInstance().options.getViewDistance().getValue() * 16) {
                bots++;
            }
        }
    }

    @Override
    public void onDeactivate() {
        bots = 0;
        targets = 0;
    }
}