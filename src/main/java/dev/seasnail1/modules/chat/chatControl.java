package dev.seasnail1.modules.chat;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.Translator;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import java.io.IOException;
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

    private final Setting<String> suffixText = sgChat.add(new StringSetting.Builder()
            .name("suffix-text")
            .description("The text to add as your suffix when you type in chat.")
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

    private final Setting<Boolean> translate = sgClient.add(new BoolSetting.Builder()
            .name("translate")
            .description("Translates messages.")
            .defaultValue(false)
            .build());

    private final Setting<String> receivedLanguage = sgClient.add(new StringSetting.Builder()
            .name("Received language")
            .description("Translates other people's messages to this.")
            .defaultValue("English")
            .build());

    private final Setting<String> targetLanguage = sgClient.add(new StringSetting.Builder()
            .name("Sending language")
            .description("The language you want to translate your messages to.")
            .defaultValue("Spanish")
            .visible(translate::get)
            .build());

    public chatControl() {
        super(Addon.CATEGORY, "Chat-control+", "allows you to have more control over client messages and server messages");
    }

    Translator translator = new Translator(receivedLanguage.get(), targetLanguage.get());

    @EventHandler
    private void onMessageSend(SendMessageEvent event) throws IOException {
        if (coordsProtection.get() && containsCoordinates(event.message)) {
            event.cancel();
            warning("You cannot send messages with coordinates. ", event.message);
            return;
        }
        event.message = addSuffix(greenText(event.message));
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        String messageString = event.getMessage().getString();

        if (filterWords.get().stream().anyMatch(messageString::contains) || playerList.get().stream().anyMatch(messageString::contains)) {
            event.cancel();
        }
    }

    private boolean containsCoordinates(String message) {
        return message.matches(".*-?\\d{1,6}, -?\\d{1,6}, -?\\d{1,6}.*");
    }

    private String greenText(String message) {
        return green.get() ? "> " + message : message;
    }

    private String addSuffix(String message) {
        return prefix.get() ? message + " " + suffixText.get() : message;
    }
}
