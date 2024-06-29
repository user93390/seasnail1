package org.Snail.Plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.Snail.Plus.hud.Watermark;
import org.Snail.Plus.modules.combat.*;
import org.Snail.Plus.modules.misc.*;
import org.Snail.Plus.utils.HWID;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Addon");

    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");

    @Override
    public void onInitialize() {

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
        if (HWID.CheckHWID()) {
            LOG.info("Welcome to snail++");
        } else {
            LOG.warn(trollMessages[randomIndex]);
            System.out.println(getHWID());
            System.exit(-805306369);
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
        Modules.get().add(new MiddleClick());
        Modules.get().add(new AutoPearl());
        // HUD
        Hud.get().register(Watermark.INFO);
    }

    private String getHWID() {
        return DigestUtils.sha3_256Hex(DigestUtils.md2Hex(DigestUtils.sha512Hex(DigestUtils.sha512Hex(System.getenv("os") + System.getProperty("os.name") + System.getProperty("os.arch") + System.getProperty("os.version") + System.getProperty("user.language") + System.getenv("SystemRoot") + System.getenv("HOMEDRIVE") + System.getenv("PROCESSOR_LEVEL") + System.getenv("PROCESSOR_REVISION") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_ARCHITECTURE") + System.getenv("PROCESSOR_ARCHITEW6432") + System.getenv("NUMBER_OF_PROCESSORS")))));
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
