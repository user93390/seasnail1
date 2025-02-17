package dev.seasnail1.mixins;

import dev.seasnail1.modules.chat.chatControl;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ChatUtils.class)
public class ChatUtilsMixin {
    @Inject(method = "getPrefix", at = @At("HEAD"), cancellable = true, remap = false)
    private static void getPrefix(CallbackInfoReturnable<Text> cir) {
        try {
            Color color = Modules.get().get(chatControl.class).color.get();
            MutableText logo = Text.literal("Snail++");
            MutableText prefix = Text.literal("");

            logo.setStyle(logo.getStyle().withColor(color.getPacked()));
            prefix.setStyle(prefix.getStyle().withColor(color.getPacked()));
            prefix.append("[");
            prefix.append(logo);
            prefix.append("] ");
            cir.setReturnValue(prefix);
        } catch (Exception e) {
            ChatUtils.error("Error in getPrefix method: %s", e.getMessage());
        }
    }
}