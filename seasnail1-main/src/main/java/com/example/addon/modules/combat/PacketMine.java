
package com.example.addon.modules.combat;

import com.example.addon.Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;


public class stealthMine extends Addon {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum AutoSwitchMode {
        Silent,
        Off
    }

    private final Setting<Boolean> instant = sgGeneral.add(new BoolSetting.Builder()
            .name("instant-mine")
            .description("instantly remines the broken block if  replaced")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("delay")
            .description("Delay in seconds between each instant mine")
            .defaultValue(1.0)
            .min(0.0)
            .sliderMax(20.0)
            .visible(instant::get)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("rotates you to the block you are mining")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> mineSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("mine-speed")
            .description("mining speed (1.0 -> vanilla)")
            .defaultValue(1.0)
            .min(0.0)
            .sliderMax(1.0)
            .build()
    );

    private final Setting<AutoSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<AutoSwitchMode>()
            .name("swap-mode")
            .description("how to swap to pickaxe when mining")
            .defaultValue(AutoSwitchMode.Silent)
            .build()
    );

    private final Setting<Boolean> city = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-city")
            .description("turns on auto-city to act like a automine")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> multiTask = sgGeneral.add(new BoolSetting.Builder()
            .name("multi-task")
            .description("pauses automine when you are eating")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> eat = sgGeneral.add(new BoolSetting.Builder()
            .name("eat-pause")
            .description("stops auto-mine when eating")
            .visible(multiTask::get)
            .defaultValue(true)
            .build()
    );
    
    private final Setting<SettingColor> readySideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of the render")
        .defaultValue(new SettingColor(0, 204, 0, 10))
        .build()
    );
    
    

    private boolean breaking;
    private boolean swapped;
    private Double target;
    private Direction direction;
    private BlockPos blockPos;

    public stealthMine() {
        super(Addon.COMBAT, "Packetmine+", "better mining for pvp, can be used as automine");
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long time = System.currentTimeMillis();

        // Adjust itemSlot type
        FindItemResult itemSlot = InvUtils.findInHotbar(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE);
        
        if(autoSwitch.get() == AutoSwitchMode.Silent) { 
            if (itemSlot != null) {
                // Adjust Rotations class usage
                Rotations.rotate(mc.player.getYaw(), 90, () -> {
                    // Send mine packets
                    sendMinePackets();
                });
            }
        }
    }

    private void sendMinePackets() {
        if (breaking) {
            // Ensure mc is accessible
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, direction));
            breaking = false;
        }
    }
}