package org.Snail.Plus.modules.combat;

import org.Snail.Plus.Addon;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class AntiBurrow extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius players can be in to be targeted.")
            .defaultValue(5)
            .range(0, 7)
            .sliderRange(0, 7)
            .build());

    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-disable")
            .description("Disables the module when you have placed the sand")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates towards the webs when placing.")
            .defaultValue(true)
            .build());

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to filter targets within range.")
            .defaultValue(SortPriority.LowestDistance)
            .build());

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each block placement.")
            .defaultValue(1.0)
            .min(0)
            .sliderMax(5)
            .build());

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
            .name("color")
            .description("Color for block placement.")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build());

    public AntiBurrow() {
        super(Addon.Snail, "Anti Burrow", "Disables a meta on most anarchy servers");
    }

    private long lastPlaceTime = 0;
    
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), priority.get());
        BlockPos targetPos = target.getBlockPos();

        if (target != null) {
        long time = System.currentTimeMillis();
        if ((time - lastPlaceTime) < delay.get() * 1000) return;
        lastPlaceTime = time;

        BlockUtils.place(targetPos, InvUtils.findInHotbar(Items.OAK_BUTTON, Items.BIRCH_BUTTON, Items.ACACIA_BUTTON,
        Items.DARK_OAK_BUTTON, Items.STONE_BUTTON, Items.SPRUCE_BUTTON), rotate.get(), 0, false);
        RenderUtils.renderTickingBlock(targetPos, color.get(), color.get(), ShapeMode.Both, 5, 5, true, false);

        if (autoDisable.get()) {
            this.toggle();
            ChatUtils.sendMsg(Formatting.RED, "[auto disable] Anti Burrow has been Disabled");
        }
    }
        }
    private  void render(Renderer3D event) {

    }
}
