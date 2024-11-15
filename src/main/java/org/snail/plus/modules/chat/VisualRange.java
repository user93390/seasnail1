package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.snail.plus.Addon;
import org.snail.plus.utils.WorldUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class VisualRange extends Module {

    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");

    private final Setting<Set<EntityType<?>>> entities = sgVisualRange.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to alert.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build());

    private final Setting<Boolean> checkUuid = sgVisualRange.add(new BoolSetting.Builder()
            .name("check-uuid")
            .description("Toggle checking player UUIDs. (only works on players)")
            .defaultValue(true)
            .visible(() -> entities.get().contains(EntityType.PLAYER))
            .build());

    private final Setting<Boolean> ignoreInvalid = sgVisualRange.add(new BoolSetting.Builder()
            .name("ignore-invalid")
            .description("Ignores invalid players. (bots)")
            .defaultValue(true)
            .visible(() -> entities.get().contains(EntityType.PLAYER))
            .build());

    private final Setting<Integer> maxAmount = sgVisualRange.add(new IntSetting.Builder()
            .name("max-amount")
            .description("The cap of how many players the visual range notifies.")
            .defaultValue(3)
            .build());

    private final Setting<Boolean> ignoreFriends = sgVisualRange.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignores friends.")
            .defaultValue(true)
            .build());

    private final Setting<List<SoundEvent>> sounds = sgVisualRange.add(new SoundEventListSetting.Builder()
            .name("sounds")
            .description("Sounds to play when a player is spotted")
            .build());

    private List<Entity> players;
    private int viewDistance;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Random random = new Random();
    public double x, y, z;
    public VisualRange() {
        super(Addon.Snail, "Visual Range", "Notifies you when a player is in your visual range.");
    }

    @Override
    public void onActivate() {
        players = new ArrayList<>();
        viewDistance = mc.options.getViewDistance().getValue();
        if (executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newSingleThreadExecutor();
        }
    }

    @Override
    public void onDeactivate() {
        players.clear();
        if (executor != null) {
            executor.shutdown();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        List<SoundEvent> soundList = sounds.get();
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (EntityUtils.isInRenderDistance(entity)) {
                if (entities.get().contains(entity.getType()) && !players.contains(entity)) {
                    if (checkUuid.get() && entity.getUuid() != null) {
                        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
                        if (!ignoreInvalid.get() && (playerListEntry == null || playerListEntry.getLatency() < 1)) continue;
                        if(players.size() < maxAmount.get() && !ignoreFriends.get() || !ignoreFriends.get() && !Friends.get().isFriend((PlayerEntity) entity)) {
                            players.add(entity);
                             x = Math.round(entity.getX());
                             y = Math.round(entity.getY());
                             z = Math.round(entity.getZ());
                            if (!soundList.isEmpty()) {
                                WorldUtils.playSound(soundList.get(random.nextInt(soundList.size())), 1.0f);
                            }
                            warning("Entity spotted %s", entity.getName().getString() + " at " + x + " " + y + " " + z);
                        }
                    }
                }
            } else {
                if (checkUuid.get() && entity.getUuid() != null) {
                    PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
                    if (!ignoreInvalid.get() && (playerListEntry == null || playerListEntry.getLatency() < 1)) continue;
                    double x = Math.round(entity.getX());
                    double y = Math.round(entity.getY());
                    double z = Math.round(entity.getZ());
                    warning("Entity left %s", entity.getName().getString() + " last known position was " + x + " " + y + " " + z);
                    if (!soundList.isEmpty()) {
                        WorldUtils.playSound(soundList.get(random.nextInt(soundList.size())), 1.0f);
                    }
                    players.remove(entity);
                }
            }
        }
    }
}
