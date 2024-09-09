package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import org.snail.plus.Addon;
import org.snail.plus.modules.misc.XPautomation;
import org.snail.plus.utils.WorldUtils;

import java.util.List;
import java.util.Random;
import java.util.stream.StreamSupport;

public class ArmorMessage extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> durability = sgGeneral.add(new DoubleSetting.Builder()
            .name("durability")
            .description("The durability at which to send a message")
            .defaultValue(10)
            .min(0)
            .max(100)
            .sliderMax(100)
            .build());

    private final Setting<Boolean> autoXP = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-xp")
            .description("Automatically enables AutoXP+ when your armor is low.")
            .defaultValue(true)
            .build());

    private final Setting<String> message = sgGeneral.add(new StringSetting.Builder()
            .name("message")
            .description("The message to send")
            .defaultValue("Your armor is low! ({present})")
            .build());

    private final Setting<Boolean> sound = sgGeneral.add(new BoolSetting.Builder()
            .name("sound")
            .description("Plays a sound when your armor is low")
            .defaultValue(true)
            .build());

    private final Setting<List<SoundEvent>> sounds = sgGeneral.add(new SoundEventListSetting.Builder()
            .name("sounds")
            .description("Sounds to play when a player is spotted")
            .visible(sound::get)
            .build());

    private final Setting<Boolean> warnFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("warn-friends")
            .description("Warn friends")
            .defaultValue(true)
            .build());

    private final Setting<String> friendMessage = sgGeneral.add(new StringSetting.Builder()
            .name("friend-message")
            .description("The message to send to friends")
            .defaultValue("hey {name}! armor at {present}%! go repair!")
            .build());

    private final Setting<Boolean> autoDm = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-dm")
            .description("Sends the message to friends.")
            .defaultValue(true)
            .build());

    public ArmorMessage() {
        super(Addon.Snail, "Armor Message", "Sends a message when your armor is low");
    }

    private void sendWarning(String msg) {
        if (sound.get() && !sounds.get().isEmpty()) {
            WorldUtils.playSound(sounds.get().get(new Random().nextInt(sounds.get().size())), 1.0f);
        }
        info(msg);
    }

    private void sendFriendWarning(Friend friend, String msg) {
        String dmMsg = autoDm.get() ? msg : "/msg " + friend.name + " " + msg;
        ChatUtils.sendPlayerMsg(dmMsg);
    }

    private String isArmorLow(ItemStack itemStack, String armorPiece) {
        return itemStack.getMaxDamage() - itemStack.getDamage() < durability.get() ? armorPiece : null;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        ItemStack helmet = mc.player.getInventory().getArmorStack(3);
        ItemStack chestplate = mc.player.getInventory().getArmorStack(2);
        ItemStack leggings = mc.player.getInventory().getArmorStack(1);
        ItemStack boots = mc.player.getInventory().getArmorStack(0);

        if (helmet.isEmpty() || chestplate.isEmpty() || leggings.isEmpty() || boots.isEmpty()) return;

        String msg = message.get().replace("{present}", String.valueOf(durability.get().intValue()));
        StringBuilder lowArmorPieces = new StringBuilder();

        if (mc.player.getArmorItems() != null && mc.player.getArmorItems().iterator().hasNext()) {
            if (StreamSupport.stream(mc.player.getArmorItems().spliterator(), false)
                    .anyMatch(itemStack -> {
                        String lowPiece = isArmorLow(itemStack, itemStack.getName().getString());
                        if (lowPiece != null) {
                            lowArmorPieces.append(lowPiece).append(" ");
                            return true;
                        }
                        return false;
                    })) {
                sendWarning(msg + " Low armor pieces: " + lowArmorPieces.toString().trim());
                if(autoXP.get()) {
                    ChatUtils.info("Enabling AutoXP+");
                    if(!Modules.get().isActive(XPautomation.class)) {
                        Modules.get().get(XPautomation.class).toggle();
                    }
                }
                return;
            }
        }
        if (warnFriends.get()) {
            mc.world.getPlayers().stream()
                    .filter(player -> player != mc.player && Friends.get().isFriend(player))
                    .forEach(player -> {
                        StringBuilder friendLowArmorPieces = new StringBuilder();
                        if (StreamSupport.stream(player.getArmorItems().spliterator(), false)
                                .anyMatch(itemStack -> {
                                    String lowPiece = isArmorLow(itemStack, itemStack.getName().getString());
                                    if (lowPiece != null) {
                                        friendLowArmorPieces.append(lowPiece).append(" ");
                                        return true;
                                    }
                                    return false;
                                })) {
                            String friendMsg;
                        }
                    });
        }
    }
}