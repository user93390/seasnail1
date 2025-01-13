package org.snail.plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.tutorial.TutorialStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snail.plus.commands.swapCommand;
import org.snail.plus.hud.Watermark;
import org.snail.plus.modules.chat.armorWarning;
import org.snail.plus.modules.chat.chatControl;
import org.snail.plus.modules.chat.killMessages;
import org.snail.plus.modules.chat.visualRange;
import org.snail.plus.modules.combat.*;
import org.snail.plus.modules.misc.*;
import org.snail.plus.modules.render.FOV;
import org.snail.plus.modules.render.burrowEsp;
import org.snail.plus.modules.render.spawnerExploit;

public class Addon extends MeteorAddon {
    public static final Logger LOGGER = LoggerFactory.getLogger("Snail++");
    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    Runnable Initialize = () -> {
        synchronized (this) {
            try {
                loadModules();
                LOGGER.debug("Loading configuration");
                Config.get().load();
                mc.getTutorialManager().setStep(TutorialStep.NONE);
                mc.options.skipMultiplayerWarning = true;
                mc.options.advancedItemTooltips = true;
                mc.options.getAutoJump().setValue(false);
            } catch (Exception e) {
                LOGGER.error("Critical error while loading: {}", e.getMessage());
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onInitialize() {
        LOGGER.debug("Initializing Addon");
        Systems.addPreLoadTask(Initialize);
    }

    @Override
    public void onRegisterCategories() {
        LOGGER.debug("Registering categories");
        Modules.registerCategory(Snail);
    }

    // Load modules
    public void loadModules() {
        LOGGER.debug("Loading modules");
        Modules.get().add(new visualRange());
        Modules.get().add(new burrowEsp());
        Modules.get().add(new FOV());
        Modules.get().add(new discordRPC());
        Modules.get().add(new autoAnchor());
        Modules.get().add(new autoXP());
        Modules.get().add(new webAura());
        Modules.get().add(new autoFarmer());
        Modules.get().add(new selfAnvil());
        Modules.get().add(new chatControl());
        Modules.get().add(new killMessages());
        Modules.get().add(new autoWither());
        Modules.get().add(new armorWarning());
        Modules.get().add(new antiBurrow());
        Modules.get().add(new obsidianFarmer());
        Modules.get().add(new minecartAura());
        Modules.get().add(new spawnerExploit());
        Modules.get().add(new packetMine());
        Hud.get().register(Watermark.INFO);

        Commands.add(new swapCommand());
        LOGGER.debug("Modules loaded");
    }

    @Override
    public String getPackage() {
        return "org.snail.plus";
    }
}