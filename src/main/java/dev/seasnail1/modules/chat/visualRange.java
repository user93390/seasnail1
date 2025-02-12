package dev.seasnail1.modules.chat;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.WorldUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class visualRange extends Module {
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");

    private final Setting<Set<EntityType<?>>> entities = sgVisualRange.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to alert.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build());

    private final Setting<Boolean> checkUuid = sgVisualRange.add(new BoolSetting.Builder()
            .name("ignore invalid entities")
            .description("Ignores bots and other invalid entities.")
            .defaultValue(true)
            .build());

    private final Setting<Integer> maxAmount = sgVisualRange.add(new IntSetting.Builder()
            .name("max-amount")
            .description("The cap of how many players the visual range notifies.")
            .defaultValue(20)
            .sliderRange(1, 100)
            .build());

    private final Setting<List<SoundEvent>> sounds = sgVisualRange.add(new SoundEventListSetting.Builder()
            .name("sounds")
            .description("Sounds to play when a player is spotted")
            .build());

    private final Setting<Double> pitch = sgVisualRange.add(new DoubleSetting.Builder()
            .name("pitch")
            .description("The pitch of the sound.")
            .defaultValue(1.0)
            .sliderRange(0.0, 10.0)
            .build());

    public visualRange() {
        super(Addon.CATEGORY, "visual-Range", "warns you when certain entities are within render distance");
    }

    private final Set<Entity> entitiesList = new HashSet<>();
    private final Random random = new Random();
    public double x, y, z;

    Runnable reset = () -> mc.execute(() -> {
        entitiesList.clear();
        x = 0;
        y = 0;
        z = 0;
        random.setSeed(System.currentTimeMillis());
    });

    public boolean isValid(Entity entity) {
        if (entity instanceof PlayerEntity) {
            if (entity instanceof FakePlayerEntity) return true;

            return correctName(entity) && tabCheck(entity);
        } else {
            return true;
        }
    }

    public boolean correctName(Entity entity) {
        if (entity.getName().getString().isEmpty()) return false;
        //if the username starts with a dot, it's usually a bedrock player
        if (entity.getName().getString().charAt(0) == '.') {
            return true;
        }
        return entity.getName().getString().matches("[a-zA-Z0-9_]{3,16}");
    }

    public boolean tabCheck(Entity entity) {
        //check if the entity is in the tab list.
        return mc.getNetworkHandler().getPlayerList().stream()
                .anyMatch(player -> player.getProfile().getName().equals(entity.getName().getString()));
    }

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        List<SoundEvent> soundList = sounds.get();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity playerEntity) {
                if (entity == mc.player || Friends.get().isFriend(playerEntity)) continue;
            }

            boolean inRenderDistance = EntityUtils.isInRenderDistance(entity);
            boolean isTrackedEntity = entities.get().contains(entity.getType());
            boolean isNewEntity = !entitiesList.contains(entity);
            boolean canAddMoreEntities = entitiesList.size() < maxAmount.get();
            boolean isValidEntity = isValid(entity);

            if (checkUuid.get() && !isValidEntity) {
                remover(entity).run();
                continue;
            }

            if (inRenderDistance && isTrackedEntity && isNewEntity && canAddMoreEntities && (isValidEntity || !checkUuid.get())) {
                playSound(soundList);
                logEntityAction(l.Entered, entity);
                entitiesList.add(entity);
            } else if (!inRenderDistance && isTrackedEntity && !isNewEntity) {
                playSound(soundList);
                logEntityAction(l.Left, entity);
                entitiesList.remove(entity);
            }
        }
    }

    private void playSound(List<SoundEvent> soundList) {
        if (!soundList.isEmpty()) {
            WorldUtils.playSound(soundList.get(random.nextInt(soundList.size())), pitch.get().floatValue());
        }
    }

    private void logEntityAction(l mode, Entity entity) {
        String message = switch (mode) {
            case Entered ->
                    String.format("%s spotted at (%s)", entity.getName().getString(), WorldUtils.getCoords(entity.getBlockPos()));
            case Left ->
                    String.format("%s left at (%s)", entity.getName().getString(), WorldUtils.getCoords(entity.getBlockPos()));

            case null -> "NaN";
        };
        warning(message);
    }

    private Runnable remover(Entity entity) {
        return () -> mc.execute(() -> {
            if (!correctName(entity) || !tabCheck(entity)) {
                entitiesList.remove(entity);
            }
        });
    }

    enum l {
        Entered,
        Left
    }
}