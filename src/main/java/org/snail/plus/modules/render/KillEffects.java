package org.snail.plus.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.explosion.Explosion;
import org.snail.plus.Addon;

public class KillEffects extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<mode> Mode = sgGeneral.add(new EnumSetting.Builder<mode>()
            .name("mode")
            .description("The mode of the kill effect.")
            .defaultValue(mode.firework)
            .build());

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-friends")
            .description("Ignores friends.")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-self")
            .description("Ignores yourself.")
            .defaultValue(true)
            .build());

    public KillEffects() {
        super(Addon.Snail, "Kill Effects", "shows effects when a player dies");
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        for(PlayerEntity player : mc.world.getPlayers()) {
            if(player.isDead()) {
                if(player == mc.player && ignoreSelf.get()) continue;
                if(Friends.get().isFriend(player) && ignoreFriends.get()) continue;
                switch(Mode.get()) {
                    case firework:
                        mc.world.spawnEntity(new FireworkRocketEntity(mc.world, player.getX(), player.getY(), player.getZ(), null));
                        break;
                    case lightning:
                        mc.world.spawnEntity(new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world));
                        break;
                }
            }
        }
    }
    public enum mode {
        firework,
        lightning,
    }
}
