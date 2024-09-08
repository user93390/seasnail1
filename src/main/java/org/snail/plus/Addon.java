package org.snail.plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoCity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.client.util.math.MatrixStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snail.plus.hud.Watermark;
import org.snail.plus.modules.combat.*;
import org.snail.plus.modules.misc.*;

import java.util.Objects;


public class Addon extends MeteorAddon {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public static final Logger LOG = LoggerFactory.getLogger("Snail++");
    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");

    @Override
    public void onInitialize() {

        loadModules();
        Config.get().load();
        LOG.info("Loaded config");

        mc.getTutorialManager().setStep(TutorialStep.NONE);
        mc.options.skipMultiplayerWarning = true;
        mc.options.advancedItemTooltips = true;
        LOG.info("Snail++ loaded! join the discord at https://discord.gg/nh9pjVhsVb");
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Snail);
    }

    // Load modules
    public void loadModules() {
        Modules.get().add(new AutoEZ());
        Modules.get().add(new autoCity());
        Modules.get().add(new XPautomation());
        Modules.get().add(new ChatControl());
        Modules.get().add(new stealthMine());
        Modules.get().add(new AutoAnchor());
        Modules.get().add(new FOV());
        Modules.get().add(new BurrowEsp());
        Modules.get().add(new AutoSand());
        Modules.get().add(new SelfAnvil());
        Hud.get().register(Watermark.INFO);
    }

    @Override
    public String getPackage() {
        return "org.snail.plus";
    }
}