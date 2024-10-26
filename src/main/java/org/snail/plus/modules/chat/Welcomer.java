package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.snail.plus.Addon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Welcomer extends Module {

    private final SettingGroup sgJoin = settings.createGroup("Join");
    private final SettingGroup sgLeave = settings.createGroup("Leave");

    private final Setting<Boolean> debug = sgJoin.add(new BoolSetting.Builder()
            .name("debug")
            .description("Debug mode.")
            .defaultValue(false)
            .build());

   private final Setting<Boolean> join = sgJoin.add(new BoolSetting.Builder()
            .name("join")
            .description("Toggles join messages.")
            .defaultValue(true)
            .build());

    private final Setting<List<String>> joinMessages = sgJoin.add(new StringListSetting.Builder()
            .name("join-messages")
            .description("Messages to send when a player joins.")
            .defaultValue(List.of("Welcome"))
            .visible(join::get)
            .build());
            
    private final Setting<Boolean> leave = sgLeave.add(new BoolSetting.Builder()
            .name("leave")
            .description("Toggles leave messages.")
            .defaultValue(true)
            .build());

    private final Setting<List<String>> leaveMessages = sgLeave.add(new StringListSetting.Builder()
            .name("leave-messages")
            .description("Messages to send when a player leaves.")
            .defaultValue(List.of("Goodbye"))
            .visible(leave::get)
            .build());
            
    public Welcomer() {
        super(Addon.Snail, "welcomer", "Welcomes players to the server.");
    }

   private int players = 0;
   private List<ServerPlayerEntity> playerList = new ArrayList<>();
   private Random random = new Random();

    @Override
    public void onActivate() {
        if (mc.getServer() != null && mc.getServer().getPlayerManager() != null) {
            players = mc.getServer().getPlayerManager().getPlayerList().size();
            playerList = mc.getServer().getPlayerManager().getPlayerList();

            if (debug.get()) {
                Addon.LOG.info("Players: " + players);
                for (PlayerEntity player : playerList) {
                    Addon.LOG.info(player.getName().getString());
                }
            }
        }
    }

    /**
     * Handles the Tickevent.Post event to manage player join and leave messages.
     * <p>
     * This method checks the current number of players on the server and compares it 
     * with the previously recorded number of players. If there are more players than 
     * previously recorded, it iterates through the current player list and sends a 
     * welcome message to new players. If there are fewer players than previously 
     * recorded, it iterates through the previously recorded player list and sends a 
     * leave message to players who have left.
     * 
     * @param event The Tickevent.Post event that triggers this method.
     */
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if(mc.getServer() != null) {
        if (mc.getServer().getPlayerManager().getPlayerList().size() > players) {
            for (ServerPlayerEntity player : mc.getServer().getPlayerManager().getPlayerList()) {
                if (!playerList.contains(player)) {
                    playerList.add(player);
                    if (join.get()) {
                        String message = joinMessages.get().get(random.nextInt(joinMessages.get().size()));
                        ChatUtils.sendPlayerMsg(String.format(message, player.getName().getString()));
                    }
                }
            }
        } else if (mc.getServer().getPlayerManager().getPlayerList().size() < players) {
            for (PlayerEntity player : playerList) {
                if (!mc.getServer().getPlayerManager().getPlayerList().contains(player)) {
                    playerList.remove(player);
                    if (leave.get()) {
                        String message = leaveMessages.get().get(random.nextInt(leaveMessages.get().size()));
                        ChatUtils.sendPlayerMsg(String.format(message, player.getName().getString()));
                    }
                }
            }
        }
        players = mc.getServer().getPlayerManager().getPlayerList().size();
        }
    }
}