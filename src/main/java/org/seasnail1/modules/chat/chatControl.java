package org.seasnail1.modules.chat;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import org.seasnail1.Addon;

import java.util.List;
import java.util.Random;

public class chatControl extends Module {

    private final SettingGroup sgChat = settings.createGroup("Chat");
    private final SettingGroup sgClient = settings.createGroup("Client");
    private final SettingGroup sgReply = settings.createGroup("Reply");

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

    private final Setting<Boolean> autoReply = sgReply.add(new BoolSetting.Builder()
            .name("auto-reply")
            .description("Automatically reply to certain messages.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> requireMcName = sgReply.add(new BoolSetting.Builder()
            .name("require-mc-name")
            .description("Only reply to messages that contain your Minecraft name.")
            .defaultValue(true)
            .visible(autoReply::get)
            .build());

    private final Setting<Boolean> directMessage = sgReply.add(new BoolSetting.Builder()
            .name("direct message")
            .description("sends the message directly to the player")
            .defaultValue(false)
            .visible(autoReply::get)
            .build());

    private final Setting<Boolean> whiteList = sgReply.add(new BoolSetting.Builder()
            .name("whitelist")
            .description("Only reply to certain players.")
            .defaultValue(false)
            .visible(autoReply::get)
            .build());

    private final Setting<List<String>> players = sgReply.add(new StringListSetting.Builder()
            .name("players")
            .description("Players to reply to.")
            .defaultValue(List.of())
            .visible(whiteList::get)
            .visible(autoReply::get)
            .build());

    private final Setting<List<String>> replyMessages = sgReply.add(new StringListSetting.Builder()
            .name("reply-messages")
            .description("Messages to reply with.")
            .defaultValue(List.of("I am currently afk.", "I am currently busy."))
            .visible(autoReply::get)
            .build());

    private final Setting<List<String>> triggerKeywords = sgReply.add(new StringListSetting.Builder()
            .name("trigger-keywords")
            .description("Keywords that trigger an auto-reply. Leave empty to reply to all messages.")
            .defaultValue(List.of("help", "question"))
            .visible(autoReply::get)
            .build());

    public chatControl() {
        super(Addon.CATEGORY, "Chat-control+", "allows you to have more control over client messages and server messages\n");
    }

    Random random = new Random();
    boolean sentMessage = false;

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        try {
            if (coordsProtection.get() && containsCoordinates(event.message)) {
                event.cancel();
                warning("You cannot send messages with coordinates. ", event.message);
                return;
            }

            event.message = addSuffix(event.message);
            event.message = greenText(event.message);
        } catch (Exception e) {
            error("Error in onMessageReceive method: %s", e.getMessage());
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        Text message = event.getMessage();
        String messageString = message.getString();

        for (String word : filterWords.get()) {
            if (messageString.contains(word)) {
                event.cancel();
                return;
            }
        }

        for (String player : playerList.get()) {
            if (messageString.contains(player)) {
                event.cancel();
                return;
            }
        }

        if (autoReply.get() && requireMcName.get() && messageString.contains(mc.player.getName().getString())) {
            for (String keyword : triggerKeywords.get()) {
                if (messageString.contains(keyword)) {
                    if (whiteList.get()) {
                        for (String player : players.get()) {
                            if (messageString.contains(player)) {
                                sendReply();
                                return;
                            }
                        }
                    } else {
                        sendReply();
                        return;
                    }
                }
            }
        }
    }

    private void sendReply() {
        String replyMessage = replyMessages.get().get(random.nextInt(replyMessages.get().size()));

        if (!sentMessage && replyMessage != null && !replyMessage.isEmpty()) {
            if (directMessage.get()) {
                ChatUtils.sendPlayerMsg("/msg " + replyMessage);
            } else {
                ChatUtils.sendPlayerMsg(replyMessage);
            }
        }
        sentMessage = true;
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
