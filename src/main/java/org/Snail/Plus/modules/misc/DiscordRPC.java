package org.Snail.Plus.modules.misc;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.Snail.Plus.Addon;


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