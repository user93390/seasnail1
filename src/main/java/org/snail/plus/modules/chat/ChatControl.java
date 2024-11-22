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
    private final SettingGroup sgChat = settings.createGroup("Chat");
    private final SettingGroup sgClient = settings.createGroup("Client");
    public final Setting<Boolean> improvedMsgs = sgClient.add(new BoolSetting.Builder()
            .name("improved-msgs")
            .description("Improves the look of chat messages.")
            .defaultValue(true)
            .build());


    private final Setting<Boolean> coordsProtection = sgChat.add(new BoolSetting.Builder()
            .name("coords-protection")
            .description("Prevents you from sending messages in chat that may contain coordinates.")
            .defaultValue(true)
            .build());

    public final Setting<SettingColor> color = sgClient.add(new ColorSetting.Builder()
            .name("prefix color")
            .description("The color of the prefix.")
            .build());

    private final Setting<Boolean> prefix = sgChat.add(new BoolSetting.Builder()
            .name("chat prefix")
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
        super(Addon.Snail, "Chat Control", "allows you to have more control over client messages and server messages\n");
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        try {
            if (coordsProtection.get() && containsCoords(event.message)) {
                event.cancel();
                ChatUtils.sendMsg(Text.of(Formatting.RED + "Your message contains coordinates and was not sent."));
            }

            if (prefix.get()) {
                event.message = event.message + " " + prefixText.get();
            }

            if (green.get()) {
                event.message = ">" + event.message;
            }
        } catch (Exception e) {
            ChatUtils.error("Error in onMessageReceive method: %s", e.getMessage());
        }
    }


    private boolean containsCoords(String message) {
        return message.matches(".*\\b-?\\d{1,5}\\s+-?\\d{1,5}\\s+-?\\d{1,5}\\b.*");
    }
}