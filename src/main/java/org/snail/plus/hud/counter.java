package org.snail.plus.hud;


import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.snail.plus.Addon;

public class counter extends HudElement {
    public static final HudElementInfo<counter> INFO = new HudElementInfo<>(Addon.HUD_GROUP, "counter+", "shows kills, deaths, kdr", counter::new);
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
            .description("how big the text should be")
            .defaultValue(2.0)
            .min(0.0)
            .sliderMax(100.0)
            .build());

    private final Setting<Boolean> reset = sgGeneral.add(new BoolSetting.Builder()
            .name("reset")
            .description("reset the counter")
            .defaultValue(false)
            .build());

    private int kills = 0;
    private int deaths = 0;
    private long lastDeath = 0;
    private double kdr = 0;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public counter() {
        super(INFO);
    }

    public int getKills() {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.isDead() && player.getLastAttacker() == mc.player) {
                kills++;
            }
        }
        return kills;
    }
    public Long getLastDeath() {
        //update last death every second using for loop
        long currentTime = System.currentTimeMillis();
        long lastPlacedTime = 0;
        if (currentTime - lastPlacedTime < (1000)) return lastDeath;
        for(int deathTime = 0; !mc.player.isDead(); deathTime++) {
            lastDeath = deathTime;
        }
        return lastDeath;
    }

    public int getDeaths() {
        if (mc.player.isDead()) {
            deaths++;
        }
        return deaths;
    }

    public double getKDR() {
        kdr = (double) kills / deaths;
        return kdr;
    }

    @EventHandler
    public void Ontick(TickEvent.Post event) {
        if (mc.world.getPlayers().contains(mc.player.getName().getString())) {
            if (reset.get()) {
                kills = 0;
                deaths = 0;
                kdr = 0;
            }
        }
    }

    @EventHandler
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("Kills: " + getKills() + " Deaths: " + getDeaths() + " KDR: " + getKDR(), shadow.get()), renderer.textHeight(true));
        renderer.text("Kills: " + getKills() + " Deaths: " + getDeaths() + " KDR: " + getKDR() + "last Death: " + getLastDeath(), x, y, color.get(), shadow.get());
    }
}