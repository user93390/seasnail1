package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;

import java.util.List;
import java.util.Random;

public class AutoEZ extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> messageSetting = sgGeneral.add(new StringListSetting.Builder()
            .name("message")
            .description("Custom message to send. placeholders: %Entity%, %Coords%")
            .defaultValue("ez %Entity%, coords: %Coords%", "gg %Entity%, coords: %Coords%")
            .build());

    private final Setting<Boolean> dm = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-dm")
            .description("Sends the message to the player.")
            .defaultValue(true)
            .build());

    Random random = new Random();
    boolean sentMessage = false;
    Runnable reset = () -> mc.execute(() -> {
        sentMessage = false;
        random = new Random();
    });

    public AutoEZ() {
        super(Addon.Snail, "Auto EZ+", "sends a custom message when a player you've killed dies");
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
    private void onTick(TickEvent.Post event) {
        mc.execute(() -> {
            if (mc.player == null || mc.world == null) return;
            for (PlayerEntity player : mc.world.getPlayers()) {
                if ((player.isDead() && !player.equals(mc.player) && !sentMessage)) {
                    sendMessages(List.of(player));
                    sentMessage = true;
                }
            }
        });
    }

    private void sendMessages(List<PlayerEntity> entity) {
        for (PlayerEntity player : entity) {
            String message = messageSetting.get().get(random.nextInt(messageSetting.get().size()));
            message = message.replace("%Entity%", player.getName().getString());
            message = message.replace("%Coords%", WorldUtils.getCoords(player));
            if (dm.get()) {
                ChatUtils.sendPlayerMsg("/msg" + message);
            } else {
                ChatUtils.sendPlayerMsg(message);
            }
        }
    }
}