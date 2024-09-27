package org.snail.plus.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import org.snail.plus.Addon;

public class Watermark extends HudElement {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    public static final HudElementInfo<Watermark> INFO = new HudElementInfo<>(Addon.HUD_GROUP, "Watermark", "Cool Watermark", Watermark::new);
    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
            .name("shadow")
            .description("Shows shadow")
            .defaultValue(false)
            .build());
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("What color should the text be")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build());
    private final Setting<Double> size = sgGeneral.add(new DoubleSetting.Builder()
            .name("size")
            .description("How big the Watermark should be")
            .defaultValue(2.0)
            .min(0.0)
            .sliderMax(100.0)
            .build());
    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
            .name("name")
            .description("Name of the element")
            .defaultValue("snail++")
            .build());
    public Watermark() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth(name.get(), shadow.get()), renderer.textHeight(true));
        renderer.quad(x, y, size.get(), size.get(), color.get());
        renderer.text(name.get(), x, y, color.get(), shadow.get());
    }
}
