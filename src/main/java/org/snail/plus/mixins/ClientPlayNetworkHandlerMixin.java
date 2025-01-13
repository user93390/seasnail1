package org.snail.plus.mixins;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.snail.plus.utilities.events.PlayerDeathEvent;
import org.snail.plus.utilities.events.TotemPopEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Unique
    int i = 0;
    @Unique
    private final Set<Packet<?>> processedPackets = new HashSet<>();

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (mc.world != null && packet.getEntity(mc.world) instanceof PlayerEntity player) {
            if (processedPackets.contains(packet)) return;

            processedPackets.add(packet);
            //packet status 35 is totem pop
            switch (packet.getStatus()) {
                case 35 -> {
                    i++;
                    MeteorClient.EVENT_BUS.post(new TotemPopEvent(i, player));
                }
                //packet status 3 is player death
                case 3 -> {
                    boolean selfKilled = player.getLastAttacker() == mc.player;
                    PlayerDeathEvent event = new PlayerDeathEvent(player, player.getBlockPos(), selfKilled);
                    MeteorClient.EVENT_BUS.post(event);
                }
            }

            if (player.isDead()) {
                i = 0;
            }
        }
    }
}