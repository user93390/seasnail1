package dev.seasnail1.modules.combat;

import dev.seasnail1.Addon;
import dev.seasnail1.utilities.CombatUtils;
import dev.seasnail1.utilities.MathHelper;
import dev.seasnail1.utilities.SwapUtils;
import dev.seasnail1.utilities.WorldUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IBox;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Author: seasnail1
 */
public class AutoAnchor extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacement = settings.createGroup("Placement");
    private final SettingGroup sgDamage = settings.createGroup("Damage");
    private final SettingGroup sgAntiCheat = settings.createGroup("Anti-Cheat");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    // General Settings
    private final Setting<Double> placeBreak = sgGeneral.add(new DoubleSetting.Builder().name("place-break range").description("The maximum distance to place and break anchors.").defaultValue(3.0).sliderRange(1.0, 10.0).build());

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target range").description("The maximum distance to target players.").defaultValue(3.0).sliderRange(1.0, 20).build());

    private final Setting<CombatUtils.filterMode> targetMode = sgGeneral.add(new EnumSetting.Builder<CombatUtils.filterMode>().name("filter mode").description("The mode used for targeting players.").defaultValue(CombatUtils.filterMode.LowestHealth).build());

    private final Setting<logic> logicMode = sgGeneral.add(new EnumSetting.Builder<logic>().name("logic").description("The logic used for placing anchors.").defaultValue(logic.damage).build());
    // Placement Settings
    private final Setting<Boolean> rotate = sgPlacement.add(new BoolSetting.Builder().name("rotation").description("Enables rotation towards the block when placing anchors.").defaultValue(false).build());

    private final Setting<Integer> rotationSteps = sgAntiCheat.add(new IntSetting.Builder().name("rotation steps").description("The amount of steps to rotate.").sliderRange(5, 25).visible(rotate::get).build());

    private final Setting<Double> explodeDelay = sgPlacement.add(new DoubleSetting.Builder().name("explode delay").description("How long to wait before exploding the anchor after placing it. In ticks").defaultValue(5).sliderRange(0.0, 10.0).build());

    private final Setting<SwapUtils.swapMode> swap = sgPlacement.add(new EnumSetting.Builder<SwapUtils.swapMode>().name("swap mode").description("The mode used for swapping items when placing anchors.").defaultValue(SwapUtils.swapMode.Move).build());

    private final Setting<Boolean> swing = sgPlacement.add(new BoolSetting.Builder().name("swing").description("Swings your hand.").defaultValue(true).build());

    private final Setting<Double> minDamage = sgDamage.add(new DoubleSetting.Builder().name("min damage").description("The minimum damage required to place an anchor.").defaultValue(0.5).sliderRange(0.0, 36.0).build());

    private final Setting<Double> maxDamage = sgDamage.add(new DoubleSetting.Builder().name("max damage").description("The maximum damage towards you").defaultValue(5.0).sliderRange(0.0, 36.0).build());

    // Render Settings
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side color").description("The color of the sides of the rendered anchor box.").defaultValue(new SettingColor(255, 0, 0, 75)).build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line color").description("The color of the lines of the rendered anchor box.").defaultValue(new SettingColor(255, 0, 0, 255)).build());

    private final Setting<RenderMode> renderMode = sgRender.add(new EnumSetting.Builder<RenderMode>().name("render mode").description("The mode used for rendering the anchor box.").defaultValue(RenderMode.smooth).build());

    private final Setting<Integer> duration = sgRender.add(new IntSetting.Builder().name("render time").description("The duration for which the anchor box is rendered, in ticks.").defaultValue(3).sliderRange(1, 100).visible(() -> renderMode.get() == RenderMode.fading).build());

    private final Setting<Integer> Smoothness = sgRender.add(new IntSetting.Builder().name("smoothness").description("The smoothness of the anchor box rendering in smooth mode.").defaultValue(3).sliderRange(1, 100).visible(() -> renderMode.get() == RenderMode.smooth).build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape mode").description("The shape mode used for rendering the anchor box.").defaultValue(ShapeMode.Both).build());

    private final Setting<Boolean> pauseUse = sgMisc.add(new BoolSetting.Builder().name("pause on use").description("Pauses the module when you are using an item.").defaultValue(false).build());

    PlayerEntity[] entities = new PlayerEntity[0];
    BlockPos[] AnchorPos = new BlockPos[0];
    BlockPos pos;
    Box renderBoxOne = null, renderBoxTwo = null;
    long lastExplode = 0;
    float selfDamage = 0, targetDamage = 0;
    boolean broken = false;

    Runnable reset = () -> {
        broken = false;
        pos = null;
        entities = new PlayerEntity[0];
        AnchorPos = new BlockPos[0];
        selfDamage = 0;
        targetDamage = 0;
    };

    public AutoAnchor() {
        super(Addon.CATEGORY, "Auto-Anchor", "Blows up respawn anchors to deal massive damage to targets");
    }

    @Override
    public void onActivate() {
        reset.run();
        lastExplode = 0;
    }

    @Override
    public void onDeactivate() {
        reset.run();
        lastExplode = 0;
    }

    private void calculateDamage(BlockPos pos) {
        for (PlayerEntity entity : entities) {
            this.selfDamage = DamageUtils.anchorDamage(mc.player, pos.toCenterPos());
            this.targetDamage = DamageUtils.anchorDamage(entity, pos.toCenterPos());
        }
    }


    void calculate(Vec3d start) {
        List<BlockPos> sphere = MathHelper.radius(BlockPos.ofFloored(start), Math.sqrt(placeBreak.get()));

        sphere.removeIf(pos -> !WorldUtils.replaceable(pos) || !WorldUtils.intersects(pos, true) ||
                mc.player.getBlockPos().getSquaredDistance(start) > placeBreak.get() * placeBreak.get());

        for (BlockPos pos : sphere) {
            calculateDamage(pos);

            if (selfDamage <= maxDamage.get() && targetDamage >= minDamage.get()) {
                AnchorPos = Arrays.copyOf(AnchorPos, AnchorPos.length + 1);
                AnchorPos[AnchorPos.length - 1] = pos;

                int index = 1;
                for (int i = 1; i < AnchorPos.length; i++) {
                    if (!AnchorPos[i].equals(AnchorPos[i - 1])) {
                        AnchorPos[index] = AnchorPos[i];
                        index++;
                    }
                }

                if (index < AnchorPos.length) {
                    AnchorPos = Arrays.copyOf(AnchorPos, index);
                }
            }
        }
    }



    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        if (broken) {
            reset.run();
        }

        if (mc.world.getDimension().respawnAnchorWorks()) {
            error("You are in the wrong dimension!");
            toggle();
            return;
        }

        PlayerEntity bestTarget = CombatUtils.bestTarget(mc.world.getPlayers(), targetMode.get(), targetRange.get());

        if (bestTarget == null) {
            return;
        }

        entities = new PlayerEntity[]{bestTarget};

        for (PlayerEntity entity : entities) {
            calculate(entity.getBlockPos().toCenterPos());

            /*
            damage -> Places anchors at the position with the highest damage to the target.
            range -> Places anchors at the position closest to the player.
            auto -> Places anchors at the position with the highest damage to the target minus the distance to the player.
            */

          this.pos = switch (logicMode.get()) {
                case damage ->  Arrays.stream(AnchorPos)
                        .max(Comparator.comparingDouble(p -> DamageUtils.anchorDamage(entity, p.toCenterPos())))
                        .orElse(null);

                case range -> Arrays.stream(AnchorPos)
                        .min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p.getX(), p.getY(), p.getZ())))
                        .orElse(null);

                case auto -> Arrays.stream(AnchorPos)
                        .max(Comparator.comparingDouble(pos -> {
                            double range = mc.player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
                            double damage = DamageUtils.anchorDamage(entity, pos.toCenterPos());

                            return damage - range;
                        }))
                        .orElse(null);
            };
        }

        // Break once per tick
        if (this.pos != null && !broken) {
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, () -> MathHelper.updateRotation(rotationSteps.get()));
            }
            breakAnchor();
        }
    }

    private void breakAnchor() {
        if (updateEat() || pos == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastExplode < (explodeDelay.get() * 50)) return;

        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);

        if (!anchor.found() || !glowstone.found()) {
            error("Missing " + (!anchor.found() ? "Respawn Anchor" : "Glowstone") + " in hotbar!");
            toggle();
            return;
        }

        BlockHitResult hitResult = new BlockHitResult(pos.toCenterPos(), mc.player.getHorizontalFacing(), pos, false);

        // Step 1: Place anchor
        performInteraction(() -> BlockUtils.place(pos, anchor, rotate.get(), 100, true), anchor);

        // Step 2: Charge anchor
        performInteraction(() -> mc.interactionManager.interactBlock(mc.player, glowstone.getHand(), hitResult), glowstone);

        // Step 3: Explode anchor
        performInteraction(() -> mc.interactionManager.interactBlock(mc.player, anchor.getHand(), hitResult), anchor);

        lastExplode = currentTime;
        broken = true;
    }

    private void performInteraction(Runnable action, FindItemResult item) {
        switch (swap.get()) {
            case silent -> {
                InvUtils.swap(item.slot(), true);
                action.run();
                InvUtils.swapBack();
            }
            case Inventory -> {
                SwapUtils.pickSwitch(item.slot());
                action.run();
                SwapUtils.pickSwapBack();
            }
            case normal -> {
                InvUtils.swap(item.slot(), true);
                action.run();
            }
            case Move -> {
                SwapUtils.moveSwitch(item.slot(), mc.player.getInventory().selectedSlot);
                action.run();
                SwapUtils.moveSwitch(mc.player.getInventory().selectedSlot, item.slot());
            }
        }
    }

    private boolean updateEat() {
        return pauseUse.get() && mc.player.isUsingItem();
    }

    @EventHandler
    public void render(Render3DEvent event) {
        for (PlayerEntity entity : entities) {
            if (mc.player.getBlockPos().getSquaredDistance(entity.getBlockPos()) > placeBreak.get() * placeBreak.get() || pos == null) continue;

            switch (renderMode.get()) {
                case normal -> event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case fading -> RenderUtils.renderTickingBlock(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0, duration.get(), true, false);
                case smooth -> {
                    if (renderBoxOne == null) {
                        renderBoxOne = new Box(pos);
                    }
                    if (renderBoxTwo == null) {
                        renderBoxTwo = new Box(pos);
                    }
                    if (renderBoxTwo instanceof IBox) {
                        ((IBox) renderBoxTwo).meteor$set(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                    }

                    double offsetX = (renderBoxTwo.minX - renderBoxOne.minX) / Smoothness.get();
                    double offsetY = (renderBoxTwo.minY - renderBoxOne.minY) / Smoothness.get();
                    double offsetZ = (renderBoxTwo.minZ - renderBoxOne.minZ) / Smoothness.get();
                    ((IBox) renderBoxOne).meteor$set(renderBoxOne.minX + offsetX, renderBoxOne.minY + offsetY, renderBoxOne.minZ + offsetZ, renderBoxOne.maxX + offsetX, renderBoxOne.maxY + offsetY, renderBoxOne.maxZ + offsetZ);
                    event.renderer.box(renderBoxOne, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        for (PlayerEntity entity1 : entities) {
            return entity1.getName().getString();
        }
        return null;
    }

    public enum RenderMode {
        fading, normal, smooth
    }

    public enum logic {
        damage, range, auto
    }
}
