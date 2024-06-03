package com.example.addon.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import com.example.addon.Addon;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.utils.StarscriptError;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.EditGameRulesScreen;
import net.minecraft.client.gui.screen.world.EditWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsScreen;
import net.minecraft.util.Util;


public class DiscordRPC extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> custom = sgGeneral.add(new BoolSetting.Builder()
       .name("custom")
       .description("allows you to make a custom text")
       .defaultValue(true)
       .build());
    private final Setting<String> lineOne = sgGeneral.add(new StringSetting.Builder()
       .name("line one")
       .description("the text you want to display")
       .defaultValue("Snail++")
       .build());

       private final Setting<String> linetwo = sgGeneral.add(new StringSetting.Builder()
       .name("line two")
       .description("the text you want to display")
       .defaultValue("Owning {server}")
       .build());

       private final Setting<String> linethree = sgGeneral.add(new StringSetting.Builder()
       .name("line three")
       .description("the text you want to display")
       .defaultValue("In the {world}")
       .build());

    private final Setting<Boolean> placeholder = sgGeneral.add(new BoolSetting.Builder()
       .name("custom placeholders")
       .description("examples: {server}, {player}, {coords}, {world}")
       .defaultValue(true)
       .build());
       public DiscordRPC() {
        super(Addon.Snail, "DiscordRPC+", "Snail++ rpc");
    }


}