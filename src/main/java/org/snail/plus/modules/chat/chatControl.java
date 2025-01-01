package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.snail.plus.Addon;
import java.util.List;

public class chatControl extends Module {

    private final SettingGroup sgChat = settings.createGroup("Chat");
    private final SettingGroup sgClient = settings.createGroup("Client");

    public final Setting<Boolean> improveClientMessage = sgClient.add(new BoolSetting.Builder()
           .name("improved client messages")
            .description("Improves the look of chat messages.")
            .defaultValue(true)
            .build());

    public final Setting<SettingColor> color = sgClient.add(new ColorSetting.Builder()
            .name("prefix color")
            .description("The color of the prefix.")
            .build());

    private final Setting<Boolean> coordsProtection = sgChat.add(new BoolSetting.Builder()
            .name("coords-protection")
            .description("Prevents you from sending messages in chat that may contain coordinates.")
            .defaultValue(true)
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

    private final Setting<Boolean> filter = sgChat.add(new BoolSetting.Builder()
            .name("filter")
            .description("Filters out messages that contain certain words.")
            .defaultValue(true)
            .build());

    private final Setting<List<String>> filterWords = sgChat.add(new StringListSetting.Builder()
            .name("filter-words")
            .description("Words to filter out of chat.")
            .visible(filter::get)
            .build());

    private final Setting<List<String>> playerList = sgChat.add(new StringListSetting.Builder()
            .name("blocked players")
            .description("Players to filter out of chat.")
            .visible(filter::get)
            .build());

    public chatControl() {
        super(Addon.Snail, "Chat Control", "allows you to have more control over client messages and server messages\n");
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        try {
            if (coordsProtection.get() && containsCoords(event.message)) {
                event.cancel();
                ChatUtils.sendMsg(Text.of(Formatting.RED + "Your message contains coordinates and was not sent."));
                return;
            }

            event.message = setPrefix(event.message);
            event.message = setGreen(event.message);
        } catch (Exception e) {
            error("Error in onMessageReceive method: %s", e.getMessage());
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        Text message = event.getMessage();
        for (String word : filterWords.get()) {
            if (message.getString().contains(word)) {
                event.cancel();
                return;
            }
        }

        for (String player : playerList.get()) {
            if (message.getString().contains(player)) {
                event.cancel();
                return;
            }
        }
    }

    private boolean containsCoords(String message) {
        return message.matches(".*-?\\d{1,6}, -?\\d{1,6}, -?\\d{1,6}.*");
    }

    private String setGreen(String message) {
        return green.get() ? "> " + message : message;
    }

    private String setPrefix(String message) {
        return prefix.get() ? message + " " + prefixText.get() : message;
    }
}
