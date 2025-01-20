package org.snail.plus;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;

public class Addon extends MeteorAddon {
    public static String CLIENT_VERSION = "1.2.6";
    public static final Logger LOGGER = LoggerFactory.getLogger("Snail++");
    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");
    public static boolean needsUpdate = false;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static List<UUID> users = new ArrayList<>();
    public static File file = new File("external.json").getAbsoluteFile();

    @Override
    public void onInitialize() {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream(file)))
                    .build();
            FirebaseApp.initializeApp(options);

            Firestore db = FirestoreClient.getFirestore();

            // Create a document reference for the player
            DocumentReference docRef = db.collection("Minecraft").document("Players");

            // Create a map to store the player's data
            Map<String, Object> playerData = new HashMap<>();
            playerData.put(mc.getSession().getUsername(), " " + mc.getSession().getUuidOrNull().toString());
            docRef.set(playerData);

            Map<String, Object> data = docRef.get().get().getData();
            if (data != null) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    users.add(UUID.fromString(entry.getValue().toString().trim()));
                }
            }

            synchronized (this) {
                checkForUpdates.run();
                if (!needsUpdate) {
                    Initialize.run();
                    UUID uuid = mc.getSession().getUuidOrNull();
                    if (uuid != null) {
                        users.add(uuid);
                    }

                    LOGGER.warn(String.valueOf(users));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Critical error while initializing", e);
            e.printStackTrace();
            System.exit(1);
        }
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