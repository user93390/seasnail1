package org.seasnail1;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import org.json.JSONObject;
import org.seasnail1.commands.swapCommand;
import org.seasnail1.hud.Watermark;
import org.seasnail1.modules.chat.armorWarning;
import org.seasnail1.modules.chat.chatControl;
import org.seasnail1.modules.chat.killMessages;
import org.seasnail1.modules.chat.visualRange;
import org.seasnail1.modules.combat.*;
import org.seasnail1.modules.misc.autoFarmer;
import org.seasnail1.modules.misc.autoWither;
import org.seasnail1.modules.misc.autoXP;
import org.seasnail1.modules.misc.obsidianFarmer;
import org.seasnail1.modules.render.FOV;
import org.seasnail1.modules.render.burrowEsp;
import org.seasnail1.modules.render.spawnerExploit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class Addon extends MeteorAddon {
    public static final Category CATEGORY = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");
    public static final Logger Logger = LoggerFactory.getLogger("Snail++");
    private static final RichPresence RPC = new RichPresence();
    public static String CLIENT_VERSION = "1.2.7";
    public static boolean needsUpdate;

    Runnable Initialize = () -> {
        try {
            loadModules();
        } catch (Exception e) {
            Logger.error("Critical error while loading: {}", Arrays.toString(e.getStackTrace()));
        }
    };

    Runnable checkForUpdates = () -> {
        Logger.info("Checking for updates");
        try {
            URI uri = URI.create("https://api.github.com/repos/user93390/seasnail1/releases/latest");
            String latestVersion = getString(uri);
            needsUpdate = !CLIENT_VERSION.equals(latestVersion);
            if (needsUpdate) {
                String message = String.format("Please update your client to the latest version (%s) found at https://github.com/user93390/seasnail1/releases", latestVersion);
                Logger.error(message);
                throw new CrashException(new CrashReport(message, new Throwable("Client is out of date")));
            } else {
                Logger.info("Client is up to date {}", latestVersion);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    @Override
    public void onInitialize() {
        try {
            synchronized (this) {
                DiscordIPC.start(1289704022231617659L, null);
                checkForUpdates.run();
                if (!needsUpdate) {
                    Initialize.run();
                }

                RPC.setDetails("Playing snail++ " + CLIENT_VERSION);
                DiscordIPC.setActivity(RPC);
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

        for (Module module : moduleList) {
            Modules.get().add(module);
        }

        Hud.get().register(Watermark.INFO);
        Commands.add(new swapCommand());
        Config.get().load();
        Logger.warn("Modules and config loaded");
    }

    private static String getString(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        JSONObject json = new JSONObject(content.toString());
        return json.getString("tag_name");
    }

    @Override
    public String getPackage() {
        return "org.seasnail1";
    }
}