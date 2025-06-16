package dev.seasnail1.modules.chat;

import dev.seasnail1.Addon;
import dev.seasnail1.modules.misc.AntiBot;
import dev.seasnail1.modules.misc.AntiBot.BotUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;

import java.util.*;

public class VisualRange extends Module {
    private final SettingGroup General = settings.getDefaultGroup();

    private final Setting<Set<EntityType<?>>> entities = General.add(new EntityTypeListSetting.Builder().name("entities").description("Entities to alert.").onlyAttackable().defaultValue(EntityType.PLAYER).build());
    private final Setting<Integer> maxAmount = General.add(new IntSetting.Builder().name("max-amount").description("The cap of how many players the visual range notifies.").defaultValue(20).sliderRange(1, 100).build());
    private final Setting<List<SoundEvent>> sounds = General.add(new SoundEventListSetting.Builder().name("sounds").description("Sounds to play when a player is spotted").build());
    private final Setting<Double> pitch = General.add(new DoubleSetting.Builder().name("pitch").description("The pitch of the sound.").defaultValue(1.0).sliderRange(0.0, 2.0).build());


    private final Random random = new Random();
    Entity[] entitiesList = new Entity[maxAmount.get()];
    public VisualRange() {
        super(Addon.CATEGORY, "visual-Range", "warns you when certain entities are within render distance");
    }

    @Override
    public void onActivate() {
        entitiesList = new Entity[maxAmount.get()];
    }

    @Override
    public void onDeactivate() {
        entitiesList = new Entity[maxAmount.get()];
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        try {
            if (mc.world == null || mc.player == null) return;

            Map<Integer, Entity> currentEntities = new HashMap<>();

            for (Entity entity : mc.world.getEntities()) {
                if (entity == mc.player) continue;
                if (!(entity instanceof PlayerEntity) && !entities.get().contains(entity.getType())) continue;

                if (entity instanceof PlayerEntity player) {
                    AntiBot antiBot = Modules.get().get(AntiBot.class);

                    if (antiBot != null && antiBot.isActive()) {
                        BotUtils util = antiBot.new BotUtils(player);
                        if (!util.correctName() || !util.tabCheck()) continue;
                    }
                }

                currentEntities.put(entity.getId(), entity);

                if (!isEntityInList(entity.getId())) {
                    if (countEntitiesInList() < maxAmount.get()) {
                        playSoundAndWarn(entity, "entered visual range");
                    }
                }
            }

            for (int i = 0; i < entitiesList.length; i++) {
                Entity entity = entitiesList[i];
                if (entity != null && !currentEntities.containsKey(entity.getId())) {
                    playSoundAndWarn(entity, "left visual range");
                    entitiesList[i] = null;
                }
            }

            for (int i = 0; i < entitiesList.length; i++) {
                if (entitiesList[i] != null) {
                    if (currentEntities.containsKey(entitiesList[i].getId())) {
                        entitiesList[i] = currentEntities.get(entitiesList[i].getId());
                        currentEntities.remove(entitiesList[i].getId());
                    }
                }
            }

            if (!currentEntities.isEmpty()) {
                int added = 0;
                for (Entity entity : currentEntities.values()) {
                    for (int i = 0; i < entitiesList.length; i++) {
                        if (entitiesList[i] == null) {
                            entitiesList[i] = entity;
                            added++;
                            break;
                        }

                        if (added >= maxAmount.get()) break;
                    }
                }
            }

        } catch (Exception e) {
            error("An error occurred in VisualRange: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isEntityInList(int entityId) {
        for (Entity entity : entitiesList) {
            if (entity != null && entity.getId() == entityId) {
                return true;
            }
        }
        return false;
    }

    private int countEntitiesInList() {
        int count = 0;
        for (Entity entity : entitiesList) {
            if (entity != null) count++;
        }
        return count;
    }

    private void playSoundAndWarn(Entity entity, String action) {
        if (!sounds.get().isEmpty()) {
            SoundEvent sound = sounds.get().get(random.nextInt(sounds.get().size()));
            mc.player.playSound(sound, 1.0F, pitch.get().floatValue());
        }

        warning("%s", entity.getName().getString() + " " + action + " at " + entity.getBlockPos().toShortString());
    }
}