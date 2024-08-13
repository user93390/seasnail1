package org.Snail.Plus.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import org.Snail.Plus.Addon;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.starscript.Script;

import java.util.ArrayList;
import java.util.List;


public class RPC extends Module{

    private static final RichPresence presence = new RichPresence();


    public RPC() {
        super(Addon.Snail, "RPC", "Discord RPC");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    @Override
    public void onActivate() {
        DiscordIPC.start(1259952133025959976L, null);
        presence.setState("Playing Snail++ Client!");
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
    }
}
