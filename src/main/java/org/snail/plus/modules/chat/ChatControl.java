package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.snail.plus.Addon;

public class ChatControl extends Module {

    private final SettingGroup sgClient = settings.createGroup("Client");
    private final SettingGroup sgChat = settings.createGroup("Chat");
    
    public final Setting<Boolean> improvedMsgs = sgClient.add(new BoolSetting.Builder()
            .name("improved-msgs")
            .description("Improves the look of chat messages.")
            .defaultValue(true)
            .build());

    public final Setting<SettingColor> color = sgClient.add(new ColorSetting.Builder()
            .name("color")
            .description("The color of the prefix.")
            .build());

    private final Setting<Boolean> coordsProtection = sgChat.add(new BoolSetting.Builder()
            .name("coords-protection")
            .description("Prevents you from sending messages in chat that may contain coordinates.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> prefix = sgChat.add(new BoolSetting.Builder()
            .name("prefix")
            .description("Adds a prefix to your chat messages.")
            .defaultValue(true)
            .build());

    private final Setting<String> prefixText = sgChat.add(new StringSetting.Builder()
            .name("prefix-text")
            .description("The text to add as your prefix when you type in chat.")
            .defaultValue("| snail++")
            .visible(prefix::get)
            .build());

    private final Setting<Boolean> green = sgChat.add(new BoolSetting.Builder()
            .name("green-text")
            .description("Adds a '>' to your text to make it green")
            .defaultValue(true)
            .build());


    public ChatControl() {
        super(Addon.Snail, "Chat Control", "Allows you to have more control over your chat messages.");
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        if (coordsProtection.get() && containsCoords(event.message)) {
            event.cancel();
            ChatUtils.sendMsg(Text.of(Formatting.RED + "Your message contains coordinates and was not sent."));
            return;
        }

        if (prefix.get()) {
            event.message = event.message + " " + prefixText.get();
        }

        if (green.get()) {
            event.message = ">" + event.message;
        }
    }


    private boolean containsCoords(String message) {
        return message.matches(".*\\b-?\\d{1,5}\\s+-?\\d{1,5}\\s+-?\\d{1,5}\\b.*");
    }
}