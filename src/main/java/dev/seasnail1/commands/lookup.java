package dev.seasnail1.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.seasnail1.utilities.WebsiteUtility;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class lookup extends Command {

    public lookup() {
        super("uuid", "Lookup a player's UUID.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("username", StringArgumentType.string())
                .executes(context -> {
                    String username = context.getArgument("username", String.class);
                    URI mojangAPI = createURI("https://api.mojang.com/users/profiles/minecraft/" + username);
                    String response = getUUID(mojangAPI);
                    info("UUID of " + username + ": " + response);
                    return SINGLE_SUCCESS;
                }));
    }

    private URI createURI(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUUID(URI mojangAPI) {
        WebsiteUtility websiteUtil = new WebsiteUtility();
        try {
            return websiteUtil.getString(mojangAPI, "id");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}