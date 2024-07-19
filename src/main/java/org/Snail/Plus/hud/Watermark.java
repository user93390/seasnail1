package org.Snail.Plus.hud;

import org.Snail.Plus.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class Watermark extends HudElement {
    public static final HudElementInfo<Watermark> INFO = new HudElementInfo<>(Addon.HUD_GROUP, "Watermark", "Cool Watermark", Watermark::new);

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

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
    /* don't ask why... */
    double version = 2.0;
    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
            .name("name")
            .description("Name of the element")
            .defaultValue("snail++ " + version)
            .build());

    public Watermark() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {

        double textSize = size.get();

        // Adjust the size based on text size
        double height = renderer.textHeight(true) * textSize;

        // Render the text
        renderer.text(String.valueOf(name), getX(), getY(), color.get(), shadow.get(), textSize);
    }
}
