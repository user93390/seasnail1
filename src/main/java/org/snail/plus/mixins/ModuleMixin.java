package org.snail.plus.mixins;

import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.snail.plus.Addon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Module.class)
public class ModuleMixin {
    @Unique
    private Module module = (Module) (Object) this;

    /**
     * Injects code at the beginning of the sendToggledMsg method in the Module class.
     * If chat feedback is enabled in the configuration, it sends a message indicating
     * whether the module is enabled or disabled.
     *
     * @param ci CallbackInfo object used to cancel the original method execution.
     */
    @Inject(method = "sendToggledMsg", at = @At("HEAD"), cancellable = true, remap = false)
    private void sendToggledMsg(CallbackInfo ci) {
        try {
            if (Config.get().chatFeedback.get()) {
                String enabledMsg =  "[+] " + module.title;
                String disableMsg =  "[-] " + module.title;

                String message = module.isActive() ? Formatting.GREEN + enabledMsg : Formatting.RED + disableMsg;
                ChatUtils.sendMsg(Text.of(Formatting.BOLD + message));
                ci.cancel();
            }
        } catch (Exception e) {
            Addon.LOG.error("Error in sendToggledMsg method: " + e.getMessage());
        }
    }

    @Inject(method = "warning", at = @At("HEAD"), cancellable = true, remap = false)
    private void warning(String message, Object[] args, CallbackInfo ci) {
        try {
            ChatUtils.sendMsg(Text.of(Formatting.YELLOW + " [!] " + String.format(message, args)));
            ci.cancel();
        } catch (Exception e) {
            Addon.LOG.error("Error in warning method: " + e.getMessage());
        }

    }

    @Inject(method = "error", at = @At("HEAD"), cancellable = true, remap = false)
    private void error(String message, Object[] args, CallbackInfo ci) {
        try {
            ChatUtils.sendMsg(Text.of(Formatting.RED + " [!] " + String.format(message, args)));
            ci.cancel();
        } catch (Exception e) {
            Addon.LOG.error("Error in error method: " + e.getMessage());
        }

    }

    @Inject(method = "info*", at = @At("HEAD"), cancellable = true, remap = false)
    private void info(String message, Object[] args, CallbackInfo ci) {
        try {
            ChatUtils.sendMsg(Text.of(Formatting.GRAY + " [i] " + String.format(message)));
            ci.cancel();
        } catch (Exception e) {
            Addon.LOG.error("Error in info method: " + e.getMessage());
        }
    }
}