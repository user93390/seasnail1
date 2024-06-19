package org.Snail.Plus;

import org.Snail.Plus.modules.combat.*;
import org.Snail.Plus.modules.misc.*;
import org.Snail.Plus.hud.*;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.Snail.Plus.utils.HWID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Addon");
    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");

    @Override
    public void onInitialize() {
        LOG.info("Loading Snail++...");
        if (!HWID.CheckHWID()) {
            LOG.warn("Your HWID is : " + HWID.GetHWID());
            System.exit(0);
        }
        // Modules
        Modules.get().add(new AutoEZ());
        Modules.get().add(new AutoSand());
        Modules.get().add(new AntiBurrow());
        Modules.get().add(new XPautomation());
        Modules.get().add(new ChatControl());
        Modules.get().add(new PistonPush());
        Modules.get().add(new SelfAnvil());
        Modules.get().add(new AutoKit());
        Modules.get().add(new AutoAnchor());
        Modules.get().add(new SnailBomber());
        Modules.get().add(new AutoTrap());
        Modules.get().add(new Blocker());
        Modules.get().add(new autoCity());
        // HUD
        Hud.get().register(Watermark.INFO);
        LOG.info("Snail++ is loaded");
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Snail);
    }


    @Override
    public String getPackage() {
        return "org.Snail.Plus";
    }
}
