package dev.seasnail1.modules.combat;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.CombatUtils;
import dev.seasnail1.utilities.MathHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class CrystalAura extends Module {

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
            .name("place")
            .description("Places crystals.")
            .defaultValue(true)
            .build());

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The range in which to place crystals.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(10)
            .build());

    private final Setting<Boolean> break_ = sgBreak.add(new BoolSetting.Builder()
            .name("break")
            .description("Breaks crystals.")
            .defaultValue(true)
            .build());

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
            .name("break-range")
            .description("The range in which to break crystals.")
            .defaultValue(4.5)
            .min(0)
            .sliderMax(10)
            .build());

    private final Setting<Boolean> sync = sgBreak.add(new BoolSetting.Builder()
            .name("sync")
            .description("only breaks crystals every tick.")
            .defaultValue(false)
            .build());

    private final Setting<CombatUtils.filterMode> filterMode = sgGeneral.add(new EnumSetting.Builder<CombatUtils.filterMode>()
            .name("filter-mode")
            .description("The filter mode to use.")
            .defaultValue(CombatUtils.filterMode.Closet)
            .build());

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The range in which to target players.")
            .defaultValue(12.5)
            .min(0)
            .sliderMax(16)
            .build());

    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage to deal to the target.")
            .defaultValue(6.5)
            .min(0)
            .sliderMax(36)
            .build());

    private final Setting<Double> maxSelfDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("max-self-damage")
            .description("The maximum damage you can take from a crystal.")
            .defaultValue(12.5)
            .min(0)
            .sliderMax(36)
            .build());

    public CrystalAura() {
        super(Addon.CATEGORY, "CrystalAura-plus", "Automatically places and explodes crystals around your opps.");
    }

    Set<BlockPos> validPositions = new HashSet<>();
    PlayerEntity bestTarget = null;
    BlockPos crystalPos = null;
    int crystalID = -1;

    Runnable reset = () -> {
        validPositions.clear();
        bestTarget = null;
        crystalPos = null;
        crystalID = -1;
    };

    @Override
    public void onActivate() {
        reset.run();
    }

    @Override
    public void onDeactivate() {
        reset.run();
    }

    void findValidPositions(Vec3d start) {
        int radius = (int) Math.sqrt(placeRange.get());

        List<BlockPos> sphere = MathHelper.radius(BlockPos.ofFloored(start), radius);

        sphere.removeIf(pos -> {
            return mc.world.getBlockState(pos.down(1)).getBlock() != Blocks.OBSIDIAN;
        });

        sphere.forEach(crystal -> {
            double targetDamage = DamageUtils.crystalDamage(bestTarget, crystal.toCenterPos());
            double selfDamage = DamageUtils.crystalDamage(mc.player, crystal.toCenterPos());

            if (targetDamage >= minDamage.get() && selfDamage <= maxSelfDamage.get()) {
                validPositions.add(crystal);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void init(TickEvent.Pre event) {
        if (mc.world != null) {
            bestTarget = CombatUtils.filter(mc.world.getPlayers(), filterMode.get(), targetRange.get());
            if (bestTarget == null) {
                return;
            }
        }

        findValidPositions(mc.player.getPos());

        this.crystalPos = validPositions.stream()
                .min(Comparator.comparingDouble(pos
                        -> mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()))).orElse(null);

        if (place.get()) {
            placeCrystal(this.crystalPos);
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal) {
                crystalID = crystal.getId();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void postTick(TickEvent.Post event) {
        if (bestTarget == null) {
            return;
        }

        if (break_.get() && crystalID != -1) {
            breakCrystal(crystalID);
        }
    }

    private void placeCrystal(BlockPos pos) {
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);

        if (crystal.found()) {
            InvUtils.swap(crystal.slot(), false);
            //interact with the item at the given position using BlockUtils.interact
            BlockHitResult hitResult = new BlockHitResult(Vec3d.of(pos), Direction.UP, pos, false);

            BlockUtils.interact(hitResult, crystal.getHand(), true);
            InvUtils.swapBack();
        }
    }

    private void breakCrystal(int id) {
        assert mc.interactionManager != null;

        Entity entity = mc.world.getEntityById(id);
        if (entity != null) {
            mc.interactionManager.attackEntity(mc.player, entity);
        }
    }
}
