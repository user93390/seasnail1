package com.example.addon.hud;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class HudExample extends HudElement {
    public static final HudElementInfo<HudExample> INFO = new HudElementInfo<>(Addon.HUD_GROUP, "Watermark", "Cool Watermark", HudExample::new);

    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> version = sgGeneral.add(new BoolSetting.Builder()
            .name("version")
            .description("Shows the version")
            .defaultValue(true)
            .build());

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("Color")
            .description("What color should the text be")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build());

    private final Setting<Double> size = sgGeneral.add(new DoubleSetting.Builder()
            .name("size")
            .description("How big the Watermark should be")
            .defaultValue(5.0)
            .min(0.0)
            .sliderMax(100.0)
            .build());

    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
            .name("name")
            .description("Name of the element")
            .defaultValue("snail++")
            .build());

    public HudExample() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        String text = name.get();
        double textSize = size.get();
        setSize(renderer.textWidth(text, true) * textSize, renderer.textHeight(true) * textSize);
        
        renderer.text(text, getX(), getY(), color.get(), true, textSize);
    }
}
