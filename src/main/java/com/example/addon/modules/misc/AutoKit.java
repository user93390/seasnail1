package com.example.addon.modules.misc;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventPriority;

public class AutoKit extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> kitName = sgGeneral.add(new StringSetting.Builder()
        .name("Kit name")
        .description("Auto kit name")
        .defaultValue("1")
        .build()
    );

    public AutoKit() {
        super(Addon.Snail, "Auto-kit", "Rekits when you die.");
    }
}
