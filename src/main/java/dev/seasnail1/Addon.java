package dev.seasnail1;

import dev.seasnail1.commands.lookup;
import dev.seasnail1.commands.swapCommand;
import dev.seasnail1.hud.Watermark;
import dev.seasnail1.modules.chat.armorWarning;
import dev.seasnail1.modules.chat.chatControl;
import dev.seasnail1.modules.chat.killMessages;
import dev.seasnail1.modules.chat.visualRange;
import dev.seasnail1.modules.combat.*;
import dev.seasnail1.modules.misc.*;
import dev.seasnail1.modules.render.FOV;
import dev.seasnail1.modules.render.burrowEsp;
import dev.seasnail1.modules.render.spawnerExploit;
import dev.seasnail1.utilities.WebsiteUtility;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class Addon extends MeteorAddon {
    public static final Category CATEGORY = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");
    public static final Logger Logger = LoggerFactory.getLogger("Snail++");
    public static String CLIENT_VERSION = FabricLoader.getInstance().getModContainer("seasnail1").get()
            .getMetadata()
            .getVersion()
            .getFriendlyString();

    public static boolean needsUpdate;

    Runnable Initialize = () -> {
        try {
            loadModules();
        } catch (Exception e) {
            Logger.error("Critical error while loading: {}", Arrays.toString(e.getStackTrace()));
        }
    };

    Runnable checkForUpdates = () -> {
        try {
            URI uri = URI.create("https://api.github.com/repos/user93390/seasnail1/releases/latest");
            String latestVersion = getVersion(uri);

            needsUpdate = !CLIENT_VERSION.equals(latestVersion);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    @Override
    public void onInitialize() {
        try {
            synchronized (this) {
                checkForUpdates.run();
                if (!needsUpdate) {
                    Initialize.run();
                }
            }
        } catch (Exception e) {
            Logger.error("Critical error while initializing", e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    // Load modules
    public void loadModules() {
        List<Module> moduleList = List.of(
                new visualRange(),
                new burrowEsp(),
                new FOV(),
                new autoAnchor(),
                new autoXP(),
                new webAura(),
                new autoFarmer(),
                new selfAnvil(),
                new chatControl(),
                new killMessages(),
                new autoWither(),
                new armorWarning(),
                new antiBurrow(),
                new obsidianFarmer(),
                new minecartAura(),
                new spawnerExploit(),
                new packetMine()
        );

        List<Command> commandList = List.of(
                new swapCommand(),
                new lookup()
        );

        moduleList.forEach(Modules.get()::add);
        commandList.forEach(Commands::add);

        Hud.get().register(Watermark.INFO);

        Config.get().load();
        Logger.info("Modules and config loaded");
    }

    private static String getVersion(URI uri) throws IOException {
        WebsiteUtility websiteUtil = new WebsiteUtility();

        return websiteUtil.getString(uri, "tag_name");
    }

    @Override
    public String getPackage() {
        return "dev.seasnail1";
    }
}