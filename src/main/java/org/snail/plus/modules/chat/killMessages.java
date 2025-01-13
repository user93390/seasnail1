package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.snail.plus.Addon;
import org.snail.plus.utilities.WorldUtils;
import org.snail.plus.utilities.events.PlayerDeathEvent;
import org.snail.plus.utilities.events.TotemPopEvent;
import org.snail.plus.utilities.screens.Placeholders;

import java.util.List;
import java.util.Random;

public class killMessages extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> chatDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("The delay between messages")
            .defaultValue(0.0)
            .sliderRange(0.0, 10.0)
            .build());

    private final Setting<Boolean> totemPop = sgGeneral.add(new BoolSetting.Builder()
            .name("totem pop")
            .description("sends a custom message when a player pops a totem")
            .defaultValue(false)
            .build());

    private final Setting<List<String>> totemMessage = sgGeneral.add(new StringListSetting.Builder()
            .name("totem message")
            .description("Custom message to send. placeholders: %Entity%")
            .defaultValue("")
            .visible(totemPop::get)
            .build());

    private final Setting<Boolean> killMessage = sgGeneral.add(new BoolSetting.Builder()
            .name("kill message")
            .description("sends a custom message when a player dies")
            .defaultValue(true)
            .build());

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
            .name("message")
            .description("Custom message to send. placeholders: %Entity%, %Coords%")
            .defaultValue("")
            .visible(killMessage::get)
            .build());

    private final Setting<Boolean> directMessage = sgGeneral.add(new BoolSetting.Builder()
            .name("direct message")
            .description("sends the message directly to the player")
            .defaultValue(false)
            .build());

    Random random = new Random();
    boolean sentMessage = false;
    PlayerEntity victim;
    Integer pops = 0;
    private long lastMessageTime = 0;
    Runnable tickReset = () -> mc.execute(() -> {
        sentMessage = false;
        random = new Random();
        victim = null;
        lastMessageTime = 0;
    });

    Runnable reset = () -> {
        random = new Random();
        sentMessage = false;
        pops = 0;
        lastMessageTime = 0;
    };

    Runnable showScreen = Placeholders::showScreen;
    public killMessages() {
        super(Addon.Snail, "Auto EZ+", "sends a custom message when a player dies");
    }

    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WButton placeholders = list.add(theme.button("Placeholders")).expandX().widget();
        placeholders.action = () -> {
            getContent();
            showScreen.run();
        };
        return list;
    }

    public void getContent() {
        Placeholders.items = List.of("{Entity} - shows the player's name", "{Coords} - shows the player's coordinates",
                "{totems} - shows the amount of totem pops");
        Placeholders.title = "Kill Messages Placeholders";
    }

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    @EventHandler
    private void onTotemPop(TotemPopEvent event) {
        if (totemPop.get()) {
            if(event.player != mc.player) {
                pops = event.totems;
                sendMessages(event.player, totemMessage.get());
            }
        }
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        if (killMessage.get() && !sentMessage) {
            if(event.player != mc.player) {
                if(event.selfKilled) {
                    sendMessages(event.player, messages.get());
                } else {
                    info("didn't kill " + event.player.getName().getString() + "sending message anyway...");
                    sendMessages(event.player, messages.get());
                }
                sentMessage = true;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (sentMessage) {
            tickReset.run();
        }
    }

    private void sendMessages(PlayerEntity entity, List<String> messages) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMessageTime >= chatDelay.get() * 20) {
            String message = messages.get(random.nextInt(messages.size()));
            message = message.replace("{Entity}", entity.getName().getString())
                    .replace("{Coords}", WorldUtils.getCoords(entity.getBlockPos()))
                    .replace("{totems}", pops.toString());

            ChatUtils.sendPlayerMsg(directMessage.get() ? "/msg " + victim + " " + message : message);
            lastMessageTime = currentTime;
        }
    }
}