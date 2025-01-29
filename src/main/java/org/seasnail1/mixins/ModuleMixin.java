package org.seasnail1.mixins;

import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.seasnail1.modules.chat.chatControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Module.class)
public class ModuleMixin {

    @Unique
    private Module seasnail1$module = (Module) (Object) this;

    @Inject(method = "sendToggledMsg", at = @At("HEAD"), cancellable = true, remap = false)
    private void sendToggledMsg(CallbackInfo ci) {
        if (Config.get().chatFeedback.get() && Modules.get().get(chatControl.class).improveClientMessage.get()) {
            String message = seasnail1$module.isActive() ? Formatting.GREEN + "[+] " + seasnail1$module.title : Formatting.RED + "[-] " + seasnail1$module.title;
            ChatUtils.sendMsg(Text.of(Formatting.BOLD + message));
            ci.cancel();
        }
    }

    @Inject(method = "warning", at = @At("HEAD"), cancellable = true, remap = false)
    private void warning(String message, Object[] args, CallbackInfo ci) {
        if (Modules.get().get(chatControl.class).improveClientMessage.get()) {
            ChatUtils.sendMsg(Text.of(Formatting.YELLOW + " [!] " + String.format(message, args)));
            ci.cancel();
        }
    }

    @Inject(method = "error", at = @At("HEAD"), cancellable = true, remap = false)
    private void error(String message, Object[] args, CallbackInfo ci) {
        if (Modules.get().get(chatControl.class).improveClientMessage.get()) {
            ChatUtils.sendMsg(Text.of(Formatting.RED + " [!] " + String.format(message, args)));
            ci.cancel();
        }
    }

    @Inject(method = "info*", at = @At("HEAD"), cancellable = true, remap = false)
    private void info(String message, Object[] args, CallbackInfo ci) {
        if (Modules.get().get(chatControl.class).improveClientMessage.get()) {
            ChatUtils.sendMsg(Text.of(Formatting.GRAY + " [i] " + String.format(message, args)));
            ci.cancel();
        }
    }
}