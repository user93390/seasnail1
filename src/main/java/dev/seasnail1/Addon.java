package dev.seasnail1;

import dev.seasnail1.commands.Lookup;
import dev.seasnail1.commands.SwapCommand;
import dev.seasnail1.hud.Watermark;
import dev.seasnail1.modules.chat.ArmorWarn;
import dev.seasnail1.modules.chat.ChatControl;
import dev.seasnail1.modules.chat.KillMessages;
import dev.seasnail1.modules.chat.VisualRange;
import dev.seasnail1.modules.combat.AutoAnchor;
import dev.seasnail1.modules.combat.AutoCrystal;
import dev.seasnail1.modules.combat.SelfAnvil;
import dev.seasnail1.modules.combat.WebAura;
import dev.seasnail1.modules.misc.AntiBot;
import dev.seasnail1.modules.misc.AutoExp;
import dev.seasnail1.modules.misc.PacketMine;
import dev.seasnail1.modules.misc.WitherAura;
import dev.seasnail1.modules.render.BurrowEsp;
import dev.seasnail1.modules.render.Fov;
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
import java.util.List;

public class Addon extends MeteorAddon {

    public static final URI API_URL = URI.create("https://api.github.com/repos/user93390/seasnail1/releases/latest");
    public static final Category CATEGORY = new Category("Snail++");
    public static final HudGroup HUD_GROUP = new HudGroup("Snail++");
    public static final Logger Logger = LoggerFactory.getLogger("Snail++");
    public static String CLIENT_VERSION = FabricLoader.getInstance().getModContainer("seasnail1").get().getMetadata().getVersion().getFriendlyString();

    public static boolean needsUpdate;

    Runnable checkForUpdates = () -> {
        try {
            String latestVersion = getVersion(API_URL);
            needsUpdate = !CLIENT_VERSION.equals(latestVersion);
        } catch (Exception e) {
            Logger.error("Error while checking for updates", e);
            throw new RuntimeException(e);
        }
    };

    private static String getVersion(URI uri) throws IOException {
        WebsiteUtility websiteUtil = new WebsiteUtility();

        return websiteUtil.getString(uri, "tag_name");
    }

    @Override
    public void onInitialize() {
        checkForUpdates.run();
        if (!needsUpdate) {
            loadModules();
        } else {
            Logger.error("You are using an outdated version of Snail++ {}", CLIENT_VERSION);
        }
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    public void loadModules() {
        if (Config.get() == null) {
            Config.get().load();
        }

        List<Module> moduleList = List.of(
                new VisualRange(),
                new BurrowEsp(),
                new Fov(),
                new AutoAnchor(),
                new AutoExp(),
                new WebAura(),
                new SelfAnvil(),
                new ChatControl(),
                new KillMessages(),
                new ArmorWarn(),
                new PacketMine(),
                new AutoCrystal(),
                new AntiBot(),
                new WitherAura()
        );

        List<Command> commandList = List.of(new SwapCommand(), new Lookup());

        moduleList.forEach(Modules.get()::add);
        commandList.forEach(Commands::add);

        Hud.get().register(Watermark.INFO);

        Logger.info("Modules and config loaded");
    }

    @Override
    public String getPackage() {
        return "dev.seasnail1";
    }
}
