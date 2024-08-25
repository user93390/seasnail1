package org.snail.plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.DiscordPresence;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.client.util.Icons;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snail.plus.hud.Watermark;
import org.snail.plus.modules.combat.*;
import org.snail.plus.modules.misc.*;

import java.io.IOException;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Snail++");
    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");

    @Override
    public void onInitialize() {

        LOG.info("Loaded config");
        Config.get().load();
        loadModules();
        // HUD
        Hud.get().register(Watermark.INFO);
        LOG.info("Snail++ loaded! join the discord at https://discord.gg/nh9pjVhsVb");

        mc.getTutorialManager().setStep(TutorialStep.NONE);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Snail);
    }

    public void loadModules() {
        //load modules
        Modules.get().add(new AutoEZ());
        Modules.get().add(new XPautomation());
        Modules.get().add(new ChatControl());
        Modules.get().add(new stealthMine());
        Modules.get().add(new AutoAnchor());
        Modules.get().add(new FOV());
        Modules.get().add(new BurrowEsp());
        Modules.get().add(new PvpInfo());
        Modules.get().add(new AutoSand());
        Modules.get().add(new SelfAnvil());
        Modules.get().add(new BedBomb());
    }

    @Override
    public String getPackage() {
        return "org.snail.plus";
    }
}