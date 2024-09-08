package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.snail.plus.Addon;

public class autoWelcomer extends Module {
    private final SettingGroup sgJoin = settings.createGroup("Join");
    private final SettingGroup sgLeave = settings.createGroup("Leave");

    private final Setting<String> message = sgJoin.add(new StringSetting.Builder()
            .name("message")
            .description("The message to send")
            .defaultValue("Welcome to the server! {player}")
            .build());

    private final Setting<Boolean> autoDm = sgJoin.add(new BoolSetting.Builder()
            .name("auto-dm")
            .description("Sends the message to the player.")
            .defaultValue(true)
            .build());

    private final  Setting<Boolean> ignoreFriends = sgJoin.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignores friends.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> leaveMsg = sgLeave.add(new BoolSetting.Builder()
            .name("leave-msg")
            .description("Sends a message when a player leaves.")
            .defaultValue(true)
            .build());

    private final Setting<String> leaveMessage = sgLeave.add(new StringSetting.Builder()
            .name("leave-message")
            .description("The message to send when a player leaves.")
            .defaultValue("Goodbye! {player}")
            .visible(leaveMsg::get)
            .build());


    public autoWelcomer() {
        super(Addon.Snail, "auto welcomer", "welcomes players to the server automatically");
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.world == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if(mc.world.getPlayers().contains(player)) {
                if (Friends.get().isFriend(player) && ignoreFriends.get()) return;
                String msg = (message.get().replace("{player}", player.getName().getString()));
                if (player.age == 1) {
                    ChatUtils.sendMsg(Text.of(msg));
                    if (autoDm.get()) {
                        ChatUtils.sendPlayerMsg("/msg " + player.getName().getString() + " " + msg);
                    }
                }
            }
            if(leaveMsg.get()) {
                if (!mc.world.getPlayers().contains(player)) {
                    String msg = (leaveMessage.get().replace("{player}", player.getName().getString()));
                    ChatUtils.sendMsg(Text.of(msg));
                }
            }
        }
    }
}
