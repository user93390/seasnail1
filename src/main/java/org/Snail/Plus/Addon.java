package org.Snail.Plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.Snail.Plus.hud.Watermark;
import org.Snail.Plus.modules.combat.*;
import org.Snail.Plus.modules.misc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Addon");

    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");
    @Override
    public void onInitialize() {


        // Modules
        Modules.get().add(new AutoEZ());
        Modules.get().add(new AutoSand());
        Modules.get().add(new AntiBurrow());
        Modules.get().add(new XPautomation());
        Modules.get().add(new ChatControl());
        Modules.get().add(new StealthMine());
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
        Modules.get().add(new RPC());
        Modules.get().add(new Autoweb());
        Modules.get().add(new FOV());
        Modules.get().add(new BurrowEsp());

        LOG.info("snail++ installed!");
        // HUD
        Hud.get().register(Watermark.INFO);
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
