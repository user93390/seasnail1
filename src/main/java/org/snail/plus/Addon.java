package org.snail.plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
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
import org.snail.plus.hud.*;
import org.snail.plus.modules.chat.*;
import org.snail.plus.modules.combat.*;
import org.snail.plus.modules.misc.*;
import org.snail.plus.modules.render.*;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Snail++");
    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public void onInitialize() {
        synchronized (this) {
            mc.execute(() -> {
                try {
                    loadModules();
                    Config.get().load();

                    mc.getTutorialManager().setStep(TutorialStep.NONE);
                    mc.options.skipMultiplayerWarning = true;
                    mc.options.advancedItemTooltips = true;
                    mc.options.getAutoJump().setValue(false);
                } catch (Exception e) {
                    LOG.error("Critical error while loading: {}", e.getMessage());
                }
            });
        }
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Snail);
    }

    // Load modules
    public void loadModules() {
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
        Modules.get().add(new autoReply());
        Modules.get().add(new armorWarning());
        Modules.get().add(new antiBurrow());
        Hud.get().register(Watermark.INFO);
        Commands.add(new swapCommand());
    }

    @Override
    public String getPackage() {
        return "org.snail.plus";
    }
}