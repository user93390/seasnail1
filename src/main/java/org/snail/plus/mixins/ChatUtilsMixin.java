package org.snail.plus.mixins;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.snail.plus.Addon;
import org.snail.plus.modules.chat.ChatControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(ChatUtils.class)
public class ChatUtilsMixin {
    @Inject(method = "getPrefix", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getPrefix(CallbackInfoReturnable<MutableText> cir) {
        try {
            MutableText logo = Text.literal("Snail++");
            MutableText prefix = Text.literal("");
            SettingColor color = ChatControl.getColor();
            logo.setStyle(logo.getStyle().withColor(color.toTextColor()));
            prefix.setStyle(prefix.getStyle().withColor(color.toTextColor()));
            prefix.append("[");
            prefix.append(logo);
            prefix.append("] ");
            cir.setReturnValue(prefix);
        } catch (Exception e) {
            Addon.LOG.error("Error in getPrefix method: " + e.getMessage());
        }

    }
}