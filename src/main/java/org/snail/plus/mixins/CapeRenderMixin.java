package org.snail.plus.mixins;

import meteordevelopment.meteorclient.utils.network.Capes;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.snail.plus.Addon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Capes.class)
public class CapeRenderMixin {
    @Unique
    private static final Identifier CUSTOM_CAPE = Identifier.of("snail-plus", "textures/cape.png");
    @Unique
    private static List<UUID> players = Addon.users;
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void get(PlayerEntity player, CallbackInfoReturnable<Identifier> cir) {
        for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            if(players.contains(playerEntity.getUuid())) {
                cir.setReturnValue(CUSTOM_CAPE);
            }
        }
    }
}