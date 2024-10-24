package org.snail.plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.tutorial.TutorialStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.snail.plus.hud.*;
import org.snail.plus.modules.chat.*;
import org.snail.plus.modules.combat.*;
import org.snail.plus.modules.misc.*;
import org.snail.plus.modules.render.*;


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
        LOG.info("Snail++ loaded! join the discord at " + "https://discord.gg/nh9pjVhsVb");
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Snail);
    }

    // Load modules
    public void loadModules() {
        for(Module module : Modules.get().getAll()) {
            Modules.get().add(module);
            LOG.info("Module " + module.name.toLowerCase() + " loaded");
        }
        Hud.get().register(Watermark.INFO);
    }


    @Override
    public String getPackage() {
        return "org.snail.plus";
    }
}