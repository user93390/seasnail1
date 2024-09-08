package org.snail.plus.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.snail.plus.Addon;
import org.snail.plus.utils.CombatUtils;
import org.snail.plus.utils.WorldUtils;

import java.util.ArrayList;
import java.util.List;


public class EchestFarm extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLogic = settings.createGroup("Logic");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates you when mining enderchests.")
            .defaultValue(true)
            .build());

    private final Setting<Double> delay = sgLogic.add(new DoubleSetting.Builder()
            .name("delay")
            .description("the speed of mining enderchests. Lower is slower.")
            .defaultValue(0)
            .sliderRange(0, 10)
            .build());

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Automatically switches to a pickaxe when mining enderchests.")
            .defaultValue(true)
            .build());

    private final Setting<Integer>  maxObbi = sgGeneral.add(new IntSetting.Builder()
            .name("max-obsidian")
            .description("The maximum amount of obsidian to mine.")
            .defaultValue(0)
            .min(0)
            .sliderMax(10)
            .build());

    private final Setting<Boolean> center = sgLogic.add(new BoolSetting.Builder()
            .name("center")
            .description("Centers you when mining enderchests.")
            .defaultValue(true)
            .build());

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("disable range")
            .description("disables when a player is within this range. (0 to disable)")
            .defaultValue(4)
            .sliderRange(0, 10)
            .build());

    private final Setting<Double> healthThreshold = sgGeneral.add(new DoubleSetting.Builder()
            .name("health-threshold")
            .description("Stops mining if the player's health drops below this value.")
            .defaultValue(5.0)
            .sliderRange(0, 20)
            .build());

    private final Setting<Double> toolDurability = sgGeneral.add(new DoubleSetting.Builder()
            .name("tool-durability")
            .description("Stops mining if the tool's durability is below this percentage.")
            .defaultValue(10.0)
            .sliderRange(0, 750)
            .build());


    private final Setting<Boolean> instant = sgLogic.add(new BoolSetting.Builder()
            .name("instant")
            .description("Instantly mines enderchests.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> chatMsg = sgMisc.add(new BoolSetting.Builder()
            .name("chat-msg")
            .description("Sends a chat message when you mine an enderchest.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> pauseEat = sgMisc.add(new BoolSetting.Builder()
            .name("pause-eat")
            .description("Pauses when eating.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> pauseSword = sgMisc.add(new BoolSetting.Builder()
            .name("pause-sword")
            .description("Pauses when holding a sword.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> pauseCrystal = sgMisc.add(new BoolSetting.Builder()
            .name("pause-crystal")
            .description("Pauses when holding a crystal.")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> pauseGap = sgMisc.add(new BoolSetting.Builder()
            .name("pause-gap")
            .description("Pauses when holding a gap.")
            .defaultValue(true)
            .build());

    private int minedEchest = 0;
    private List<BlockPos> echestPositions = new ArrayList<>();
    private long LastMine = 0;
    private long currentTime = System.currentTimeMillis();
    private Boolean isMining = false;
    private Boolean isBroken = false;

    public EchestFarm() {
        super(Addon.Snail, "Echest Farm", "Automatically farms enderchests.");
    }

    @Override
    public void onActivate() {
        minedEchest = 0;
        echestPositions = new ArrayList<>();
        LastMine = 0;
        currentTime = System.currentTimeMillis();
        isMining = false;
        isBroken = false;
    }

    @Override
    public void onDeactivate() {
        minedEchest = 0;
        echestPositions.clear();
        LastMine = 0;
        currentTime = System.currentTimeMillis();
        isMining = false;
        isBroken = false;
    }

    public List<BlockPos> getEchestPositions(PlayerEntity entity) {
        for (int x = -5; x < 5; x++) {
            for (int y = -5; y < 5; y++) {
                for (int z = -5; z < 5; z++) {
                    BlockPos pos = entity.getBlockPos().add(x, y, z);
                   if(WorldUtils.isAir(pos)) {
                        echestPositions.add(pos);
                        return List.of(pos);
                    }
                }
            }
        }
        return echestPositions;
    }


    @EventHandler
    public void Ontick(TickEvent.Post event) {
        for(PlayerEntity player : mc.world.getPlayers()) {
            if(player.getPos().distanceTo(mc.player.getPos()) < targetRange.get() || player.getHealth() < healthThreshold.get()) {
                return;
            }
            for(int getObsidian = minedEchest; getObsidian * 8 < maxObbi.get(); getObsidian++) {

                if(center.get() && CombatUtils.isCentered(mc.player)) {
                    PlayerUtils.centerPlayer();
                }
                if(rotate.get()) {
                    Rotations.rotate(Rotations.getYaw(player), Rotations.getPitch(player), 100, () -> {
                        if(!isMining && !isBroken) {
                            mineEchest(getEchestPositions(player).getFirst());
                        }
                        minedEchest++;
                        if(chatMsg.get()) {
                            info("Mined x" + minedEchest);
                        }
                    });
                } else {
                    if(!isMining && !isBroken) {
                        mineEchest(getEchestPositions(player).getFirst());
                        minedEchest++;
                    }
                }
            }
            if(minedEchest >= maxObbi.get()) {
                info("over max obsidian... stopping");
                minedEchest = 0;
                echestPositions.clear();
                toggle();
                return;
            }

        }
    }
    public void swap() {
        if(autoSwitch.get()) {
            for(int i = 0; i < 9; i++) {
                ItemStack itemStack = mc.player.getInventory().getStack(i);
                Item item = itemStack.getItem();
                if(item == Blocks.OBSIDIAN.asItem()) {
                    mc.player.getInventory().selectedSlot = i;
                    break;
                }
            }
        }
    }

    public void mineEchest(BlockPos blockPos) {
        swap();
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));

        if(instant.get()) {
            swap();
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
        }
        isMining = true;

        if(WorldUtils.isAir(blockPos)) {
            isBroken = true;
        }
    }
    @Override
    public String getInfoString() {
        return minedEchest * 8 + "/" + maxObbi.get().toString();
    }
}
