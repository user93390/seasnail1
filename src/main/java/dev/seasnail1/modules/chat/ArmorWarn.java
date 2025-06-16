package dev.seasnail1.modules.chat;

import dev.seasnail1.Addon;
import dev.seasnail1.modules.misc.AutoExp;
import dev.seasnail1.utilities.WorldUtils;
import dev.seasnail1.utilities.screens.Placeholders;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;

import java.util.List;

public class ArmorWarn extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder().name("friends").description("Warns your friends when they have low armor.").defaultValue(true).build());

    private final Setting<Boolean> directMessage = sgGeneral.add(new BoolSetting.Builder().name("direct message").description("Sends the message directly to the player.").defaultValue(true).visible(friends::get).build());

    private final Setting<String> chatMessage = sgGeneral.add(new StringSetting.Builder().name("chat-message").description("The message to send in chat when your armor is low.").defaultValue("").visible(friends::get).build());

    private final Setting<Double> friendThreshold = sgGeneral.add(new DoubleSetting.Builder().name("friend-threshold").description("The armor value at which to warn your friends.").defaultValue(10).sliderRange(0, 100).visible(friends::get).build());

    private final Setting<Double> maxFriendRange = sgGeneral.add(new DoubleSetting.Builder().name("max-friend-range").description("The maximum range to warn your friends.").defaultValue(7).sliderRange(0, 100).visible(friends::get).build());

    private final Setting<Double> threshold = sgGeneral.add(new DoubleSetting.Builder().name("threshold").description("The armor value at which to warn you.").defaultValue(10).sliderRange(0, 100).build());

    private final Setting<Boolean> enableXP = sgGeneral.add(new BoolSetting.Builder().name("enable auto-XP").description("Automatically uses XP to repair armor.").defaultValue(true).build());

    private final Setting<Boolean> playSound = sgGeneral.add(new BoolSetting.Builder().name("sound").description("Plays a sound when your armor is low.").defaultValue(true).build());

    private final Setting<List<SoundEvent>> sounds = sgGeneral.add(new SoundEventListSetting.Builder().name("sounds").description("Sounds to play when a player is spotted").visible(playSound::get).build());

    private final Setting<Integer> reminderTime = sgGeneral.add(new IntSetting.Builder().name("reminder time").description("how long to remind you of the armor value before warning you again. (In seconds)").defaultValue(10).build());
    private final long alertIntervalMillis = reminderTime.get() * 1000;
    boolean sent = false;
    int armorDurability;
    Runnable showScreen = Placeholders::showScreen;
    private long lastAlertTime = 0;
    Runnable reset = () -> {
        sent = false;
        armorDurability = 0;
        lastAlertTime = 0;
    };
    private Module module;
    private String grammar;

    public ArmorWarn() {
        super(Addon.CATEGORY, "armor-warning", "Warns you when your armor is low.");
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
        Placeholders.title = "Armor Warning Placeholders";

        Placeholders.items = List.of("{name} - shows the player's name", "{piece} - shows the piece of armor", "{durability} - shows the durability of the armor", "{grammar} - shows the grammar for the piece of armor (is/are)");
    }

    @Override
    public void onActivate() {
        reset.run();
    }


    @Override
    public void onDeactivate() {
        reset.run();
    }

    private void handleArmor() {
        if (mc.player != null) {
            armorDurability = getDurability(mc.player);
        }

        long currentTime = System.currentTimeMillis();
        boolean shouldAlert = currentTime - lastAlertTime >= alertIntervalMillis;

        if (armorDurability < threshold.get() && shouldAlert) {
            playAlertSounds();
            if (enableXP.get() && !module.isActive()) {
                module.toggle();
                info("Enabling auto-XP+");
            }
            warning("Your armor is low! (%s)", armorDurability);
            lastAlertTime = currentTime;
        }

        if (friends.get()) {
            WorldUtils.getAllFriends().stream().filter(friend -> friend.getBlockPos().getSquaredDistance(mc.player.getBlockPos()) <= maxFriendRange.get() * maxFriendRange.get()).forEach(friend -> {
                Integer friendDurability = getDurability(friend);
                if (friendDurability < friendThreshold.get() && !sent) {
                    playAlertSounds();
                    sendMessage(friend, friendDurability);
                    sent = true;
                }
            });
        }
    }

    private void playAlertSounds() {
        if (playSound.get() && !sounds.get().isEmpty()) {
            for (SoundEvent sound : sounds.get()) {
                WorldUtils.playSound(sound, 1);
            }
        }
    }

    private void sendMessage(PlayerEntity entity, Integer friendDurability) {
        for (ItemStack stack : entity.getArmorItems()) {
            if (stack.getItem() instanceof ArmorItem armorItem) {
                if (armorItem.toString().endsWith("s")) {
                    grammar = "are";
                } else {
                    grammar = "is";
                }
            }
        }

        String message = chatMessage.get().replace("{durability}", friendDurability.toString()).replace("{name}", entity.getName().getString()).replace("{piece}", getArmorPiece(entity).getItem().getName().getString()).replace("{grammar}", grammar);
        if (directMessage.get()) {
            ChatUtils.sendPlayerMsg("/msg " + entity.getName().getString() + " " + message);
        } else {
            ChatUtils.sendPlayerMsg(message);
        }
    }

    private ItemStack getArmorPiece(PlayerEntity entity) {
        for (ItemStack stack : entity.getArmorItems()) {
            if (stack != null && !stack.isEmpty() && stack.isDamageable()) {
                double damage = stack.getMaxDamage() - (100 * (stack.getMaxDamage() - stack.getDamage())) / stack.getMaxDamage();
                if (damage == armorDurability && damage > 0) {
                    return stack;
                }
            }
        }
        return null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            if (module == null) module = Modules.get().get(AutoExp.class);

            if (mc.player != null) handleArmor();
        } catch (Exception e) {
            error("Error in armorWarning", e);
            Addon.Logger.error("Unexpected error in armorWarning: {}", String.valueOf(e));
            e.printStackTrace();
            throw e;
        }
    }

    private Integer getDurability(PlayerEntity entity) {
        for (ItemStack stack : entity.getArmorItems()) {
            if (stack != null && !stack.isEmpty() && stack.isDamageable()) {
                float damage = (float) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage();
                if (damage > 0 && damage * stack.getMaxDamage() < threshold.get()) {
                    return (int) (stack.getMaxDamage() * damage);
                }
            }
        }
        return -1;
    }
}
