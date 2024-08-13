package org.snail.plus.modules.misc;

import meteordevelopment.meteorclient.mixin.ClientPlayNetworkHandlerAccessor;
import meteordevelopment.meteorclient.settings.*;
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
import org.snail.plus.Addon;
import org.snail.plus.utils.PlayerDeathEvent;
import org.snail.plus.utils.WorldUtils;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.lang.String;

public class AutoEZ extends Module {
    private Thread thread;
    private volatile boolean running;
    final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> dm = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-dm")
            .description("Sends a message to the player.")
            .defaultValue(true)
            .build());
    private final Setting<List<String>> Message = sgGeneral.add(new StringListSetting.Builder()
            .name("Message")
            .description("Messages to the player (randomized). you can also use placeholders like: {player}, {health}, {coords}")
            .defaultValue("EZ {player}, snail++ owns me and all.")
            .build());

    public AutoEZ() {
        super(Addon.Snail, "Auto EZ", "sends a toxic message to the player that you killed");
    }

    @Override
    public void onActivate() {
        running = true;
        thread = new Thread(() -> {
            while (running) {
                Thread.onSpinWait();
            }
        });
        thread.start();
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        PlayerEntity victim = event.getPlayer();

            String msg = Message.get().stream()
                    .map(s -> s.replace("{player}", victim.getName().getString())
                            .replace("{health}", String.valueOf(victim.getHealth()))
                            .replace("{coords}", WorldUtils.getCoords(victim))).toString();
            if (dm.get()) {
                ChatUtils.sendPlayerMsg("/msg " + WorldUtils.getName(victim) + " " + Message);
                System.out.println("messages sent");
            } if(!dm.get()) {
                Instant instant = Instant.now();
                long l = NetworkEncryptionUtils.SecureRandomUtil.nextLong();
                ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
                LastSeenMessagesCollector.LastSeenMessages lastSeenMessages = ((ClientPlayNetworkHandlerAccessor) Objects.requireNonNull(handler)).getLastSeenMessagesCollector().collect();
                MessageSignatureData messageSignatureData = ((ClientPlayNetworkHandlerAccessor) handler).getMessagePacker().pack(new MessageBody(msg, instant, l, lastSeenMessages.lastSeen()));
                handler.sendPacket(new ChatMessageC2SPacket(msg, instant, l, messageSignatureData, lastSeenMessages.update()));
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
