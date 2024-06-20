package org.Snail.Plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.Snail.Plus.hud.Watermark;
import org.Snail.Plus.modules.combat.*;
import org.Snail.Plus.modules.misc.AutoEZ;
import org.Snail.Plus.modules.misc.AutoKit;
import org.Snail.Plus.modules.misc.ChatControl;
import org.Snail.Plus.modules.misc.XPautomation;
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

        String[] List = {
                "be340168edd172fce1ac35a8ab717c137d189a92031a212350905ea2c28a189p"
        };

        String[] trollMessages = {
                "Out of memory",
                "io.netty.handler.timeout.ReadTimeoutException",
                "Error 500: Java.Lang.NullPointerException",
                "Incompatible mods found!",
                "Disk write error",
                "Unexpected end of file",
                "Resource temporarily unavailable"
        };

        Random random = new Random();
        int randomIndex = random.nextInt(trollMessages.length);

        LOG.warn(trollMessages[randomIndex]);

        if (HWID.GetHWID().equals(List)) {
            System.out.println("Welcome to Snail++!");

        } else {
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
