package org.snail.plus.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.BlockLocating;
import org.snail.plus.Addon;

public class stats extends HudElement {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    public static final HudElementInfo<stats> INFO = new HudElementInfo<>(Addon.HUD_GROUP, "stats+", "Shows pvp stats", stats::new);

    private final Setting<StatsType> Mode = sgGeneral.add(new EnumSetting.Builder<StatsType>()
            .name("render mode")
            .description("the render mode")
            .defaultValue(StatsType.venomHack)
            .build());
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("What color should the text be")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build());
    private final Setting<Double> size = sgGeneral.add(new DoubleSetting.Builder()
            .name("size")
            .description("the size of the text")
            .defaultValue(1)
            .build());
    private final Setting<Boolean> roundKDR = sgGeneral.add(new BoolSetting.Builder()
            .name("round KDR")
            .description("rounds the KDR value")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
            .name("shadow")
            .description("shows a shadow")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> resetDeath = sgGeneral.add(new BoolSetting.Builder()
            .name("reset on death")
            .description("resets the stats on player death")
            .defaultValue(true)
            .build());

    public stats() {
        super(INFO);
    }
    @Override
    public void render(HudRenderer renderer) {
        float deaths = 2;
        float kills = 5;
        float kdr = kills / deaths;
        setSize(renderer.textWidth("example", shadow.get()), renderer.textHeight(shadow.get()));
        renderer.text( " " + "KDR: " + kdr , x ,y, color.get(), shadow.get(), size.get());
        renderer.text(" "  + "Kills: " + kills, x, y, color.get(), shadow.get(), size.get());
        renderer.text(" "  + "Deaths: " + deaths, x, y, color.get(), shadow.get(), size.get());
    }

    enum StatsType {
        venomHack,
        blackOut,
        snailPlus,
        bananaPlus,
    }
}
