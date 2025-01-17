package org.snail.plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import org.json.JSONObject;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Addon extends MeteorAddon {
    public static String CLIENT_VERSION = "1.2.6";
    public static final Logger LOGGER = LoggerFactory.getLogger("Snail++");
    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");
    public static boolean needsUpdate = false;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Set<String> playerNames = new HashSet<>();

    @Override
    public void onInitialize() {
        try {
            String sql = "INSERT INTO players(name) VALUES(?)";
            try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "examplePlayerName"); // Provide a valid player name
                pstmt.executeUpdate();

                LOGGER.info("Database initialized");
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            synchronized (this) {
                checkForUpdates.run();
                if (!needsUpdate) {
                    Initialize.run();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Critical error while initializing: {}", Arrays.toString(e.getStackTrace()));
            mc.close();
            System.exit(1);
        }
    }

    private Connection connect() throws SQLException {
        // Example connection string, adjust as needed
        String url = "https://github.com/user93390/seasnail1.git";
        return DriverManager.getConnection(url);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Snail);
    }

    // Load modules
    public void loadModules() {
        LOGGER.warn("Loading modules");
        List<Module> moduleList = List.of(
                new visualRange(),
                new burrowEsp(),
                new FOV(),
                new discordRPC(),
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
        LOGGER.warn("Modules and config loaded");
    }

    Runnable Initialize = () -> {
        try {
            loadModules();
            mc.getTutorialManager().setStep(TutorialStep.NONE);
            mc.options.skipMultiplayerWarning = true;
            mc.options.advancedItemTooltips = true;
            mc.options.getAutoJump().setValue(false);
        } catch (Exception e) {
            LOGGER.error("Critical error while loading: {}", Arrays.toString(e.getStackTrace()));
        }
    };

    Runnable checkForUpdates = () -> {
        LOGGER.info("Checking for updates");
        try {
            URI uri = URI.create("https://api.github.com/repos/user93390/seasnail1/releases/latest");
            String latestVersion = getString(uri);
            needsUpdate = !CLIENT_VERSION.equals(latestVersion);
            if (needsUpdate) {
                String message = String.format("Please update your client to the latest version (%s) found at https://github.com/user93390/seasnail1/releases", uri);
                LOGGER.error(message);
                throw new CrashException(new CrashReport(message, new Throwable("Client is out of date")));
            } else {
                LOGGER.info("Client is up to date");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

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
        return "org.snail.plus";
    }
}