package org.snail.plus.modules.chat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.snail.plus.Addon;
import org.snail.plus.utilities.WorldUtils;

import java.util.ArrayList;
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

    public double x, y, z;
    private final List<Entity> entitiesList = new ArrayList<>();
    private final Random random = new Random();

    Runnable reset = () -> mc.execute(() -> {
        entitiesList.clear();
        x = 0;
        y = 0;
        z = 0;
    });

    public visualRange() {
        super(Addon.Snail, "Visual Range", "warns you when certain entities are within render distance");
    }

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    private boolean isValid(Entity entity) {
        //ignore if uuid is invalid or name is invalid
        return validUuid(entity) && validName(entity);
    }

    private boolean validName(Entity entity) {
        //usernames only contain letters, numbers, and underscores and a minimum of 3 characters
        return entity.getName().getString().matches("[a-zA-Z0-9_]{3,16}");
    }

    private boolean validUuid(Entity entity) {
        return entity.getUuid() != null;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        mc.execute(() -> {
            if (mc.player == null || mc.world == null) return;
            List<SoundEvent> soundList = sounds.get();
            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player) continue;
                if (EntityUtils.isInRenderDistance(entity)) {
                    if (entities.get().contains(entity.getType()) && !entitiesList.contains(entity)) {
                        if (checkUuid.get() && isValid(entity)) {
                            if (entitiesList.size() < maxAmount.get()) {
                                if (!soundList.isEmpty()) WorldUtils.playSound(soundList.get(random.nextInt(soundList.size())), 1.0f);
                                warning("Entity entered %s", entity.getName().getString() + " at " + WorldUtils.getCoords((PlayerEntity) entity));
                                entitiesList.add(entity);
                            }
                        } else if (!checkUuid.get()) {
                            if (entitiesList.size() < maxAmount.get()) {
                                if (!soundList.isEmpty()) WorldUtils.playSound(soundList.get(random.nextInt(soundList.size())), 1.0f);
                                warning("Entity entered %s", entity.getName().getString() + " at " + WorldUtils.getCoords((PlayerEntity) entity));
                                entitiesList.add(entity);
                            }
                        }
                    }
                } else {
                    if (entities.get().contains(entity.getType()) && entitiesList.contains(entity)) {
                        if (!soundList.isEmpty()) WorldUtils.playSound(soundList.get(random.nextInt(soundList.size())), 1.0f);

                        warning("Entity left %s", entity.getName().getString() + " at " + WorldUtils.getCoords((PlayerEntity) entity));
                        entitiesList.remove(entity);
                    }
                }
            }
        });
    }
}