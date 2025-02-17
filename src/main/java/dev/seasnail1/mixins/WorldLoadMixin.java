package dev.seasnail1.mixins;

import dev.seasnail1.Addon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class WorldLoadMixin {
    @Inject(method = "setWorld", at = @At("TAIL"))
    private void onWorldLoad(ClientWorld world, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (world != null) {
            if (Addon.needsUpdate) {
                MutableText text = Text.literal("[Snail++] An update is available! Click here to download it.");
                Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/user93390/seasnail1/releases/latest"))
                        .withFormatting(Formatting.UNDERLINE)
                        .withFormatting(Formatting.RED);
                text = text.setStyle(style);
                //add the text to the chat
                mc.inGameHud.getChatHud().addMessage(text);
            }
        }
    }
}