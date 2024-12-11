package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import org.snail.plus.Addon;
import org.snail.plus.modules.misc.autoXP;
import org.snail.plus.utils.WorldUtils;

import java.util.List;

public class armorWarning extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("friends")
            .description("Warns your friends when they have low armor.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> directMessage = sgGeneral.add(new BoolSetting.Builder()
            .name("direct message")
            .description("Sends the message directly to the player.")
            .defaultValue(true)
            .visible(friends::get)
            .build());

    private final Setting<String> chatMessage = sgGeneral.add(new StringSetting.Builder()
            .name("chat-message")
            .description("The message to send in chat when your armor is low.")
            .defaultValue("Your armor is is low {name}! ({value})")
            .visible(friends::get)
            .build());

    private final Setting<Double> friendThreshold = sgGeneral.add(new DoubleSetting.Builder()
            .name("friend-threshold")
            .description("The armor value at which to warn your friends.")
            .defaultValue(10)
            .sliderRange(0, 100)
            .build());

    private final Setting<Double> maxFriendRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("max-friend-range")
            .description("The maximum range to warn your friends.")
            .defaultValue(7)
            .sliderRange(0, 100)
            .build());

    private final Setting<Double> threshold = sgGeneral.add(new DoubleSetting.Builder()
            .name("threshold")
            .description("The armor value at which to warn you.")
            .defaultValue(10)
            .sliderRange(0, 100)
            .build());

    private final Setting<Boolean> enableXP = sgGeneral.add(new BoolSetting.Builder()
            .name("enable auto-XP")
            .description("Automatically uses XP to repair armor.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> playSound = sgGeneral.add(new BoolSetting.Builder()
            .name("sound")
            .description("Plays a sound when your armor is low.")
            .defaultValue(true)
            .build());

    private final Setting<List<SoundEvent>> sounds = sgGeneral.add(new SoundEventListSetting.Builder()
            .name("sounds")
            .description("Sounds to play when a player is spotted")
            .visible(playSound::get)
            .build());

    private final Setting<Integer> remind = sgGeneral.add(new IntSetting.Builder()
            .name("reminder time")
            .description("how long to remind you of the armor value before warning you again. (In seconds)")
            .defaultValue(10)
            .build());

    boolean sent = false;
    Integer armorDurability;
    private long lastAlertTime = 0;
    private final long alertIntervalMillis = remind.get() * 1000;
    private Module module = Modules.get().get(autoXP.class);

    Runnable reset = () -> mc.execute(() -> {
        sent = false;
        armorDurability = 0;
        lastAlertTime = 0;
    });

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    public armorWarning() {
        super(Addon.Snail, "armor-warning", "Warns you when your armor is low.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            synchronized (this) {
                mc.executeSync(() -> {
                    if (mc.player != null) {
                        armorDurability = getDurability(mc.player);

                        long currentTime = System.currentTimeMillis();
                        boolean shouldAlert = currentTime - lastAlertTime >= alertIntervalMillis;

                        if (armorDurability < threshold.get() && shouldAlert) {
                            if (playSound.get() && !sounds.get().isEmpty()) {
                                for (SoundEvent sound : sounds.get()) {
                                    mc.player.playSound(sound, 1, 1);
                                }
                                if(enableXP.get() && !module.isActive()) {
                                    module.toggle();
                                }
                            }
                            warning("Your armor is low! (%s)", armorDurability.toString());
                            lastAlertTime = currentTime;
                        }

                        if (friends.get()) {
                            for (PlayerEntity friend : WorldUtils.getAllFriends()) {
                                if (friend != null) {
                                    Integer friendDurability = getDurability(friend);

                                    if (friendDurability < friendThreshold.get() && mc.player.distanceTo(friend) < maxFriendRange.get() && shouldAlert) {
                                        String message = chatMessage.get()
                                                .replace("{value}", armorDurability.toString())
                                                .replace("{name}", friend.getName().getString());
                                        if (directMessage.get()) {
                                            ChatUtils.sendPlayerMsg("/msg " + friend.getName().getString() + " " + message);
                                        } else {
                                            ChatUtils.sendPlayerMsg(message);
                                        }
                                        lastAlertTime = currentTime;
                                    }
                                }
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            error("Error in armorWarning", e);
        }
    }

    private Integer getDurability(PlayerEntity entity) {
        for (ItemStack stack : entity.getArmorItems()) {
            if (stack != null && !stack.isEmpty() && stack.isDamageable()) {
                return 100 * (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage();
            }
        }
        return 0;
    }
}
