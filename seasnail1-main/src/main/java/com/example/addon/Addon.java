package com.example.addon;

import com.example.addon.modules.combat.*;
import com.example.addon.modules.misc.*;
import com.example.addon.modules.movement.*;



import com.example.addon.hud.*;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Addon");
    public static final Category COMBAT = new Category("Combat");
    public static final Category MISC = new Category("Misc");
    public static final Category MOVEMENT = new Category("Movement");
    public static final HudGroup HUD_GROUP = new HudGroup("HUD");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Addon Template");

        // Modules
        Modules.get().add(new AutoEZ());
        Modules.get().add(new AnchorAuraPlus());
        Modules.get().add(new AutoSand());
        Modules.get().add(new AntiBurrow());
        Modules.get().add(new XPautomation());
        Modules.get().add(new quiverPlus());
        Modules.get().add(new ChatControl());
        Modules.get().add(new VelocityPlus());
        Modules.get().add(new stealthMine());

        // HUD
        Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(COMBAT);
        Modules.registerCategory(MISC);
        Modules.registerCategory(MOVEMENT);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
