package dev.seasnail1.modules.misc;

import dev.seasnail1.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

public class AntiBot extends Module {

    private static final long CACHE_DURATION_MS = 1000;
    SettingGroup sgGeneral = settings.getDefaultGroup();
    public final Setting<Boolean> checkTab = sgGeneral.add(new meteordevelopment.meteorclient.settings.BoolSetting.Builder().name("check tab").description("Uses minecraft tab list to check for bots. Usually bots are not in the tab list.").defaultValue(true).build());
    public final Setting<Boolean> UUIDCheck = sgGeneral.add(new meteordevelopment.meteorclient.settings.BoolSetting.Builder().name("check uuid").description("Uses UUID to check for bots.").defaultValue(false).build());
    public final Setting<Boolean> ignoreBedrock = sgGeneral.add(new meteordevelopment.meteorclient.settings.BoolSetting.Builder().name("ignore bedrock").description("Ignores bedrock players.").defaultValue(true).build());
    PlayerEntity[] bots = new PlayerEntity[0];
    private PlayerEntity[] cachedPlayers = new PlayerEntity[0];
    private long lastCacheTime = 0;

    public AntiBot() {
        super(Addon.CATEGORY, "Anti-Bot", "Remove bots from the server. (Client sided)");
    }

    @Override
    public void onActivate() {
        bots = new PlayerEntity[0];
    }

    @Override
    public void onDeactivate() {
        bots = new PlayerEntity[0];
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;

        bots = validPlayers();
        for (PlayerEntity bot : bots) {
            mc.world.removeEntity(bot.getId(), PlayerEntity.RemovalReason.DISCARDED);
        }
    }

    public PlayerEntity[] validPlayers() {
        long now = System.currentTimeMillis();
        if (now - lastCacheTime < CACHE_DURATION_MS) {
            return cachedPlayers;
        }
        PlayerEntity[] valid = mc.world.getPlayers().stream().filter(player -> player != mc.player).filter(player -> {
            BotUtils botUtils = new BotUtils(player);
            return botUtils.correctName() && !botUtils.invalidUUID() && (!checkTab.get() || botUtils.tabCheck());
        }).toArray(PlayerEntity[]::new);
        cachedPlayers = valid;
        lastCacheTime = now;
        return valid;
    }

    public class BotUtils {
        private final PlayerEntity entity;

        public BotUtils(PlayerEntity entity) {
            this.entity = entity;
        }

        public boolean correctName() {
            if (entity.getName().getString().isEmpty()) return false;
            if (ignoreBedrock.get() && entity.getName().getString().charAt(0) == '.') return true;

            return entity.getName().getString().matches("[a-zA-Z0-9_]{3,16}");
        }

        public boolean invalidUUID() {
            return UUIDCheck.get() && entity.getUuid().toString().length() != 36;
        }

        public boolean tabCheck() {
            return mc.getNetworkHandler().getPlayerList().stream().noneMatch(player -> {
                if (player.getDisplayName() != null) {
                    return player.getDisplayName().getString().equals(entity.getName().getString());
                }
                return false;
            });
        }
    }
}