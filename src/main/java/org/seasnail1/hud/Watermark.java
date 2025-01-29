package org.seasnail1.hud;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import org.seasnail1.Addon;


public class Watermark extends HudElement {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    public static final HudElementInfo<Watermark> INFO = new HudElementInfo<>(Addon.HUD_GROUP, "Watermark", "Cool Watermark", Watermark::new);

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("What color should the text be")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build());

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
            .name("shadow")
            .description("Shows shadow")
            .defaultValue(false)
            .build());

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("The scale of the text.")
            .defaultValue(2.0)
            .min(0.0)
            .sliderMax(100.0)
            .build());

    public Watermark() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        String text = "Snail++";

        setSize(renderer.textWidth(text, shadow.get(), scale.get()), renderer.textHeight(shadow.get(), scale.get()));

        renderer.text(text, x, y, color.get(), shadow.get(), scale.get());
    }
}
