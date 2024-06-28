package org.Snail.Plus.utils;

import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Objects;
import java.util.stream.StreamSupport;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class FriendUtils {
    public static Boolean HasLowArmor(PlayerEntity entity, float durability) {

        if (entity.isAlive() && Friends.get().isFriend(entity)) {
            StreamSupport
                    .stream(
                            Objects.requireNonNull(mc.player).getArmorItems().spliterator(), false
                    )
                    .allMatch(itemStack ->
                            itemStack.getDamage() <= durability);
        }
        return true;
    }
}