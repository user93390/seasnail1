package org.snail.plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.mixininterface.IChatHud;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.Snail.Plus.hud.Watermark;
import org.snail.plus.modules.combat.*;
import org.snail.plus.modules.misc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Addon");

    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");
    @Override
    public void onInitialize() {
        mc.getTutorialManager().setStep(TutorialStep.NONE);
        Config.get().save();
        // Modules
        Modules.get().add(new AutoEZ());
        Modules.get().add(new XPautomation());
        Modules.get().add(new ChatControl());
        Modules.get().add(new StealthMine());
        Modules.get().add(new AutoKit());
        Modules.get().add(new AutoAnchor());
        Modules.get().add(new FOV());
        Modules.get().add(new BurrowEsp());
        Modules.get().add(new PvpInfo());
        Modules.get().add(new BedBomb());
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
