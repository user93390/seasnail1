package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import org.snail.plus.Addon;

import java.util.List;
import java.util.Random;

public class autoReply extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> requireMcName = sgGeneral.add(new BoolSetting.Builder()
            .name("require-mc-name")
            .description("Only reply to messages that contain your Minecraft name.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> whiteList = sgGeneral.add(new BoolSetting.Builder()
            .name("whitelist")
            .description("Only reply to certain players.")
            .defaultValue(false)
            .build());

    private final Setting<List<String>> players = sgGeneral.add(new StringListSetting.Builder()
            .name("players")
            .description("Players to reply to.")
            .defaultValue(List.of())
            .visible(whiteList::get)
            .build());

    private final Setting<List<String>> replyMessages = sgGeneral.add(new StringListSetting.Builder()
            .name("reply-messages")
            .description("Messages to reply with.")
            .defaultValue(List.of("I am currently afk.", "I am currently busy."))
            .build());

    private final Setting<List<String>> triggerKeywords = sgGeneral.add(new StringListSetting.Builder()
            .name("trigger-keywords")
            .description("Keywords that trigger an auto-reply. Leave empty to reply to all messages.")
            .defaultValue(List.of("help", "question"))
            .build());

    private Random random = new Random();
    private boolean sentMessage = false;

    public autoReply() {
        super(Addon.Snail, "auto Reply", "Replies to messages automatically.");
    }

    @Override
    public void onActivate() {
        random = new Random();
        sentMessage = false;
    }

    @Override
    public void onDeactivate() {
        random = new Random();
        sentMessage = false;
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        synchronized (this) {
            mc.executeSync(() -> {
                Text message = event.getMessage();
                String messageString = message.getString();
                for (String keyword : triggerKeywords.get()) {
                    if (messageString.contains(keyword) && requireMcName.get() && messageString.contains(mc.player.getName().getString())) {
                        if (whiteList.get()) {
                            for (String player : players.get()) {
                                if (messageString.contains(player)) {
                                    sendReply();
                                }
                            }
                        } else {
                            sendReply();
                        }
                    }
                }
            });
        }
    }

    private void sendReply() {
        String replyMessage = replyMessages.get().get(random.nextInt(replyMessages.get().size()));

        if (!sentMessage && replyMessage != null && !replyMessage.isEmpty()) {
            ChatUtils.sendPlayerMsg(replyMessage);
        }
        sentMessage = true;
    }
}