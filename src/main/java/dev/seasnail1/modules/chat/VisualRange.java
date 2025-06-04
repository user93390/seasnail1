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

public class VisualRange extends Module {
    private final SettingGroup General = settings.getDefaultGroup();

    private final Setting<Set<EntityType<?>>> entities = General.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to alert.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build());

    private final Setting<Boolean> checkUuid = General.add(new BoolSetting.Builder()
            .name("ignore invalid")
            .description("Ignores bots and other invalid entities.")
            .defaultValue(true)
            .visible(() -> entities.get().contains(EntityType.PLAYER))
            .build());

    private final Setting<Integer> maxAmount = General.add(new IntSetting.Builder()
            .name("max-amount")
            .description("The cap of how many players the visual range notifies.")
            .defaultValue(20)
            .sliderRange(1, 100)
            .build());

    private final Setting<List<SoundEvent>> sounds = General.add(new SoundEventListSetting.Builder()
            .name("sounds")
            .description("Sounds to play when a player is spotted")
            .build());

    private final Setting<Double> pitch = General.add(new DoubleSetting.Builder()
            .name("pitch")
            .description("The pitch of the sound.")
            .defaultValue(1.0)
            .sliderRange(0.0, 2.0)
            .build());

    public VisualRange() {
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
        if (entity instanceof FakePlayerEntity) return true;
        if(entity instanceof PlayerEntity) {
            return correctName(entity) && tabCheck(entity);
        }
        return true;
    }

    public boolean correctName(Entity entity) {
        if (entity.getName().getString().isEmpty()) return false;
        if (entity.getName().getString().charAt(0) == '.') {
            return true;
        }
        return entity.getName().getString().matches("[a-zA-Z0-9_]{3,16}");
    }

    public boolean tabCheck(Entity entity) {
        return mc.getNetworkHandler().getPlayerList().stream()
                .anyMatch(player -> player.getDisplayName().getString().equals(entity.getName().getString()));
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
            if (entity == mc.player || Friends.get().isFriend((PlayerEntity) entity)) continue;

            boolean inRenderDistance = EntityUtils.isInRenderDistance(entity);
            boolean isTrackedEntity = entities.get().contains(entity.getType());
            boolean isNewEntity = !entitiesList.contains(entity);
            boolean canAddMoreEntities = entitiesList.size() < maxAmount.get();
            boolean isValidEntity = isValid(entity);

            if (checkUuid.get() && !isValidEntity) {
                Remover(entity).run();
                continue;
            }

            if (inRenderDistance && isTrackedEntity && isNewEntity && canAddMoreEntities && (isValidEntity || !checkUuid.get())) {
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

    Runnable Remover(Entity entity) {
        return () -> mc.execute(() -> {
            if (!correctName(entity) || !tabCheck(entity)) {
                entitiesList.remove(entity);
            }
        });
    }
}