package org.Snail.Plus.modules.misc;

import org.Snail.Plus.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

public class AutoKit extends Module {
    
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> kitName = sgGeneral.add(new StringSetting.Builder()
        .name("Kit name")
        .description("Auto kit name")
        .defaultValue("1")
        .build());

    public AutoKit() {
        super(Addon.Snail, "Auto-kit", "Rekits when you die.");
    }
}
