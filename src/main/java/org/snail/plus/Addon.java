package org.snail.plus;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.tutorial.TutorialStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snail.plus.hud.Watermark;
import org.snail.plus.modules.combat.AutoAnchor;
import org.snail.plus.modules.combat.AutoSand;
import org.snail.plus.modules.combat.BurrowEsp;
import org.snail.plus.modules.combat.SelfAnvil;
import org.snail.plus.modules.misc.*;

public class Addon extends MeteorAddon {

    public static final Logger LOG = LoggerFactory.getLogger("Snail++");
    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");

    @Override
    public void onInitialize() {
        loadModules();
        Config.get().load();
        LOG.info("Loaded config");
        LOG.info("Snail++ loaded! join the discord at https://discord.gg/nh9pjVhsVb");

        //useful settings
        MinecraftClient.getInstance().getTutorialManager().setStep(TutorialStep.NONE);
        MinecraftClient.getInstance().options.skipMultiplayerWarning = true;
        MinecraftClient.getInstance().options.advancedItemTooltips = true;
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Snail);
    }

    // Load modules
    public void loadModules() {
        Modules.get().add(new AutoEZ());
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