package net.ajpappas.discord.minecraftbot.command;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import net.ajpappas.discord.common.command.SlashCommand;
import net.ajpappas.discord.common.exception.UserException;
import net.ajpappas.discord.minecraftbot.dao.PlayerDao;
import net.ajpappas.discord.minecraftbot.model.entity.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class DatabaseCommand implements SlashCommand {

    @Autowired
    private PlayerDao playerDao;

    @Autowired
    private GatewayDiscordClient client;

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public PermissionSet requiredPermissions() {
        return PermissionSet.of(Permission.ADMINISTRATOR);
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        switch (event.getOptions().get(0).getName().toLowerCase()) {
            case "delete" -> { return delete(event); }
            case "list" -> { return list(event); }
            default -> { return Mono.error(new UserException("Requested action '" + event.getOptions().get(0).getName().toLowerCase() + "' has not been implemented.")); }
        }
    }



    private Mono<Void> delete(ChatInputInteractionEvent event) {
        Mono<Long> deleteId = event.getOptions().get(0).getOption("user").get().getValue().get().asUser()
                .map(User::getId)
                .map(Snowflake::asLong);

        Mono<Player> databaseLookup = deleteId.map(playerDao::findById).flatMap(Mono::justOrEmpty);

        return databaseLookup.flatMap(player -> {
                    playerDao.delete(player);
                    return event.reply("Deleted user from database!").withEphemeral(true);
                })
                .switchIfEmpty(Mono.error(new UserException("This user does not exist in the database!")));
    }

    private Mono<Void> list(ChatInputInteractionEvent event) {
        String list = StreamSupport.stream(playerDao.findAll().spliterator(), false)
                .map(player -> {
                    StringBuilder res = new StringBuilder(getUser(player.getDiscordUserId()) + " ->");
                    if (player.getUsername() != null)
                        res.append(" Name: ").append(player.getUsername());
                    if (player.getUuid() != null)
                        res.append(" UUID: ").append(player.getUuid());
                    return res.toString();
                })
                .collect(Collectors.joining("\n"));

        if (list.isBlank())
            return event.reply("Database is empty.").withEphemeral(true);
        else return event.reply("```" + list + "```").withEphemeral(true);
    }

    private String getUser(Long id) {
        return client.getUserById(Snowflake.of(id)).map(User::getTag).block();
    }
}
