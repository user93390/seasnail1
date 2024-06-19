package org.Snail.Plus.modules.misc;

import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.mixin.ClientPlayNetworkHandlerAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.message.LastSeenMessagesCollector;
import net.minecraft.network.message.MessageBody;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.Snail.Plus.Addon;

import java.time.Instant;

public class AutoEZ extends Module {
    private Thread thread;
    private volatile boolean running;

    final SettingGroup sgGeneral = settings.getDefaultGroup();

   private final Setting<Boolean> global = sgGeneral.add(new BoolSetting.Builder()
        .name("global-chat")
        .description("Sends the message in the global chat.")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> dm = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-dm")
            .description("Sends a message to the player.")
            .defaultValue(true)
            .visible(global::get)
            .build());

    final Setting<String> messageSetting = sgGeneral.add(new StringSetting.Builder()
        .name("message")
        .description("Custom message to send.")
        .defaultValue("EZ {player}, Snail++ owns all!")
        .build());

    public AutoEZ() {
        super(Addon.Snail, "Auto ez+", "Sends a toxic message when a player dies");
    }

    @Override
    public void onActivate() {
        running = true;
        thread = new Thread(() -> {
            while (running) {

            }
        });
        thread.start();
    }

    @EventHandler
    private void onPlayerRemoved(EntityRemovedEvent event) {
        if (event == null || !(event.entity instanceof PlayerEntity removedPlayer)) return;

        if (removedPlayer == mc.player || mc.player == null) return;

        Entity lastAttacker = removedPlayer.getRecentDamageSource() != null ? removedPlayer.getRecentDamageSource().getAttacker() : null;

        if (lastAttacker instanceof PlayerEntity && lastAttacker == MinecraftClient.getInstance().player) {
            String msg = messageSetting.get()
                .replace("{player}", removedPlayer.getName().getString());

            if (dm.get()) {
                ChatUtils.sendPlayerMsg("/msg " + removedPlayer.getName().getString() + " " + msg);
            }

            if (global.get()) {
                Instant instant = Instant.now();
                long l = NetworkEncryptionUtils.SecureRandomUtil.nextLong();
                ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
                assert handler != null;
                LastSeenMessagesCollector.LastSeenMessages lastSeenMessages = ((ClientPlayNetworkHandlerAccessor) handler).getLastSeenMessagesCollector().collect();
                MessageSignatureData messageSignatureData = ((ClientPlayNetworkHandlerAccessor) handler).getMessagePacker().pack(new MessageBody(msg, instant, l, lastSeenMessages.lastSeen()));
                handler.sendPacket(new ChatMessageC2SPacket(msg, instant, l, messageSignatureData, lastSeenMessages.update()));
            }
        }
    }

    @Override
    public void onDeactivate() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }
}
