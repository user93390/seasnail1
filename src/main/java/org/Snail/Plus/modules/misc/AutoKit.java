package org.Snail.Plus.modules.misc;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.Snail.Plus.Addon;

public class AutoKit extends Module {
    private long lastPlaceTime = 0;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> Name = sgGeneral.add(new StringSetting.Builder()
            .name("Kit name")
            .description("Auto kit name")
            .defaultValue("1")
            .build());

    private final Setting<Double> Delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("delay to place blocks")
            .defaultValue(3.0)
            .sliderMax(10.0)
            .sliderMin(0.0)
            .build());

    public AutoKit() {
        super(Addon.Snail, "Auto-kit", "Rekits when you die.");
    }
@EventHandler
    private void onDeath(OpenScreenEvent event) {
    long time = System.currentTimeMillis();
    if ((time - lastPlaceTime) < Delay.get() * 1000) return;
    lastPlaceTime = time;
    if (event.screen instanceof DeathScreen) {
        ChatUtils.sendPlayerMsg("/kit " + Name);
        }
    }
}