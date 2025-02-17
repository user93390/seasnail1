package dev.seasnail1.modules.chat;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.WorldUtils;
import dev.seasnail1.utilities.events.PlayerDeathEvent;
import dev.seasnail1.utilities.events.TotemPopEvent;
import dev.seasnail1.utilities.screens.Placeholders;
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
            .description("Custom message to send.")
            .defaultValue("", "")
            .visible(totemPop::get)
            .build());

    private final Setting<Boolean> killMessage = sgGeneral.add(new BoolSetting.Builder()
            .name("kill message")
            .description("sends a custom message when a player dies")
            .defaultValue(true)
            .build());

    private final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
            .name("message")
            .description("Custom message to send.")
            .defaultValue("", "")
            .visible(killMessage::get)
            .build());

    private final Setting<Boolean> directMessage = sgGeneral.add(new BoolSetting.Builder()
            .name("direct message")
            .description("sends the message directly to the player")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> sendAnyway = sgGeneral.add(new BoolSetting.Builder()
            .name("send anyway")
            .description("sends the message even if the player didn't kill the victim")
            .defaultValue(false)
            .build());

    long currentTime = System.currentTimeMillis();
    Integer pops = 0;
    Random random = new Random();
    boolean sentMessage = false;
    PlayerEntity victim;
    private long lastMessageTime = 0;
    Runnable tickReset = () -> mc.execute(() -> {
        sentMessage = false;
        random = new Random();
        victim = null;
        lastMessageTime = 0;
    });

    Runnable reset = () -> {
        currentTime = System.currentTimeMillis();
        random = new Random();
        sentMessage = false;
        pops = 0;
        lastMessageTime = 0;
    };

    Runnable showScreen = Placeholders::showScreen;

    public killMessages() {
        super(Addon.CATEGORY, "kill-Messages", "sends a custom message when a player dies");
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
        Placeholders.title = "Kill Messages Placeholders";

        Placeholders.items = List.of(
                "{name} - shows the player's name",
                "{coordinates} - shows where the player died",
                "{totems} - shows the amount of totem pops", "{world} - shows the world the player died in",
                "{weapon} - shows the weapon used to kill the player"
        );
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
    private void onActivateTotem(TotemPopEvent event) {
        if (totemPop.get()) {
            if (event.player != mc.player) {
                pops = event.totems;
                sendMessages(event.player, totemMessage.get());
            }
        }
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        if (killMessage.get() && !sentMessage) {
            if (event.player != mc.player) {
                if (event.selfKilled) {
                    sendMessages(event.player, messages.get());
                } else {
                    if (sendAnyway.get()) {
                        info("didn't kill " + event.player.getName().getString() + "sending message anyway...");
                        sendMessages(event.player, messages.get());
                    }
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

        if (currentTime - lastMessageTime >= chatDelay.get() * 50) {
            String message = messages.get(random.nextInt(messages.size()));
            message = message.replace("{name}", entity.getName().getString())
                    .replace("{coordinates}", WorldUtils.getCoords(entity.getBlockPos()))
                    .replace("{totems}", pops.toString())
                    .replace("{world}", mc.player.getWorld().asString())
                    .replace("{weapon}", mc.player.getMainHandStack().getName().getString());


            ChatUtils.sendPlayerMsg(directMessage.get() ? "/msg " + victim + " " + message : message);
        }
        lastMessageTime = currentTime;
    }
}