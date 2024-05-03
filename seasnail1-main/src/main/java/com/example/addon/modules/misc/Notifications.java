package com.example.addon.modules.misc;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.settings.*;

import java.util.Objects;

public class Notifications extends Module {
    private final String prefix = Formatting.DARK_GREEN + "[AddonName]";
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-feedback")
            .description("Toggle chat feedback for this module.")
            .defaultValue(true)
            .build()
    );

    public Notifications() {
        super(Addon.MISC, "Notifications Snail", "Provides notifications for addon actions.");
    }

    public void sendToggledMsg() {
        if (chatFeedback.get() && mc.world != null) {
            String msg = prefix + " " + Formatting.WHITE + name
                    + (isActive() ? Formatting.GREEN + " ON" : Formatting.RED + " OFF");
            sendChatMessage(msg);
        }
    }

    public void sendToggledMsg(String message) {
        if (chatFeedback.get() && mc.world != null) {
            String msg = prefix + " " + Formatting.WHITE + name
                    + (isActive() ? Formatting.GREEN + " ON " : Formatting.RED + " OFF ") + Formatting.GRAY + message;
            sendChatMessage(msg);
        }
    }

    public void sendDisableMsg(String text) {
        if (mc.world != null) {
            String msg = prefix + " " + Formatting.WHITE + name + Formatting.RED + " OFF " + Formatting.GRAY + text;
            sendChatMessage(msg);
        }
    }

    public void sendInfo(String text) {
        if (mc.world != null) {
            String msg = prefix + " " + Formatting.WHITE + name + " " + text;
            sendChatMessage(msg);
        }
    }

    private void sendChatMessage(String message) {
        mc.player.sendMessage(Text.of(message), false);
    }
}

