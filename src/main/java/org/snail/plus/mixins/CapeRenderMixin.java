package org.snail.plus.mixins;

import meteordevelopment.meteorclient.utils.network.Capes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Capes.class)
public class CapeRenderMixin {
    @Unique
    private static final Identifier CUSTOM_CAPE = Identifier.of("snail-plus", "textures/cape.png");

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void get(PlayerEntity player, CallbackInfoReturnable<Identifier> cir) {
        if (player == mc.player) {
            cir.setReturnValue(CUSTOM_CAPE);
        }
    }
}