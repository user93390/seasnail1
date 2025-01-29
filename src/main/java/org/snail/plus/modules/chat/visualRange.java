package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.snail.plus.Addon;
import org.snail.plus.utilities.WorldUtils;

import java.util.*;

public class visualRange extends Module {
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");

    private final Setting<Set<EntityType<?>>> entities = sgVisualRange.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to alert.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build());

    private final Setting<Boolean> checkUuid = sgVisualRange.add(new BoolSetting.Builder()
            .name("ignore invalid")
            .description("Ignores bots and other invalid entities.")
            .defaultValue(true)
            .visible(() -> entities.get().contains(EntityType.PLAYER))
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
            .sliderRange(0.0, 2.0)
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


    public static boolean isValid(Entity entity) {
        //ignore if uuid is invalid or name is invalid
        return validUuid(entity) && validName(entity);
    }

    /**
     usernames only contain letters, numbers, and underscores and a minimum of 3 characters
     @param entity  the entity to check
     @return true if the entity is a valid player
     */
    public static boolean validName(Entity entity) {

        for (FakePlayerEntity fakePlayerEntity : FakePlayerManager.getFakePlayers()) {
            if (entity == fakePlayerEntity) {
                return true;
            } else {
                return entity.getName().getString().matches("[a-zA-Z0-9_]{3,16}");
            }
        }
        return false;
    }

    public static boolean validUuid(Entity entity) {
        return entity.getUuid() != null;
    }

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        List<SoundEvent> soundList = sounds.get();
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;

            boolean inRenderDistance = EntityUtils.isInRenderDistance(entity);
            boolean isTrackedEntity = entities.get().contains(entity.getType());
            boolean isNewEntity = !entitiesList.contains(entity);
            boolean canAddMoreEntities = entitiesList.size() < maxAmount.get();
            boolean shouldCheckUuid = checkUuid.get() && isValid(entity);

            if (inRenderDistance && isTrackedEntity && isNewEntity && canAddMoreEntities && (shouldCheckUuid || !checkUuid.get())) {
                if (!soundList.isEmpty()) {
                    WorldUtils.playSound(soundList.get(random.nextInt(soundList.size())), pitch.get().floatValue());
                }
                if (entity instanceof PlayerEntity) {
                    warning("Entity entered %s", entity.getName().getString() + " at " + WorldUtils.getCoords((PlayerEntity) entity));
                }
                entitiesList.add(entity);
            } else if (!inRenderDistance && isTrackedEntity && !isNewEntity) {
                if (!soundList.isEmpty()) {
                    WorldUtils.playSound(soundList.get(random.nextInt(soundList.size())), pitch.get().floatValue());
                }
                if (entity instanceof PlayerEntity) {
                    warning("Entity left %s", entity.getName().getString() + " at " + WorldUtils.getCoords((PlayerEntity) entity));
                }
                entitiesList.remove(entity);
            }
        }
    }
}