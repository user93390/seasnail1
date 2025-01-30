package org.seasnail1.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.jaqobb.namemc.NameMC;
import dev.jaqobb.namemc.cache.CacheSettings;
import dev.jaqobb.namemc.profile.Friend;
import dev.jaqobb.namemc.profile.Profile;
import dev.jaqobb.namemc.profile.ProfileRepository;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class lookup extends Command {

    public lookup() {
        super("lookup", "shows info about a player");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("username", StringArgumentType.string()).executes(context -> {
            try {
                ProfileRepository profileRepository = new ProfileRepository(new CacheSettings(Duration.of(10L, TimeUnit.MINUTES.toChronoUnit())));

                String username = context.getArgument("username", String.class);
                URI uri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + username);
                HttpURLConnection httpURLConnection;

                httpURLConnection = (HttpURLConnection) uri.toURL().openConnection();

                httpURLConnection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();

                JSONObject json = new JSONObject(content.toString());
                String uuid = json.getString("id");

                String formattedUuid = uuid.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                        "$1-$2-$3-$4-$5"
                );

                UUID uid = UUID.fromString(formattedUuid);
                Set<Friend> friends = Collections.singleton(new Friend(uid, username));

                Profile profile = new Profile(uid, friends);

                profileRepository.put(uid, profile);

                Profile prof = profileRepository.get(uid);

                if (prof == null) {
                    error("Player not found for UUID: " + formattedUuid);
                    return 0;
                } else {
                    StringBuilder message = new StringBuilder();

                    message.append("Friends:\n");
                    for (Friend friend : prof.getFriends()) {
                        message.append("  - ").append(friend.getName()).append(" (UUID: ").append(friend.getUUID().toString()).append(")\n");
                    }
                    info(message.toString());
                }

                return 1;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
