package org.snail.plus.mixins;

import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
        if (Config.get().chatFeedback.get()) {
            String enabledMsg =  "[+] " + module.title;
            String disableMsg =  "[-] " + module.title;
            // If the module is enabled, send a green message, otherwise send a red message.
            String message = module.isActive() ? Formatting.GREEN + enabledMsg : Formatting.RED + disableMsg;
            ChatUtils.sendMsg(Text.of(Formatting.BOLD + message));
            ci.cancel();
        }
    }
}