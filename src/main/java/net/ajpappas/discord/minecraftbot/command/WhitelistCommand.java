package net.ajpappas.discord.minecraftbot.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import net.ajpappas.discord.common.command.SlashCommand;
import net.ajpappas.discord.common.exception.UserException;
import net.ajpappas.discord.minecraftbot.dao.PlayerDao;
import net.ajpappas.discord.minecraftbot.model.entity.Player;
import net.ajpappas.discord.minecraftbot.model.request.WhitelistRequest;
import net.ajpappas.discord.minecraftbot.model.response.WhitelistResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@Component
public class WhitelistCommand implements SlashCommand {

    private static final Logger log = LoggerFactory.getLogger(WhitelistCommand.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PlayerDao playerDao;

    @Value("${api.host}/${api.base}/")
    private String apiUrl;


    @Override
    public String getName() {
        return "whitelist";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        WhitelistRequest.Action action;
        try {
            action = WhitelistRequest.Action.valueOf(event.getOptions().get(0).getName().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.error(new UserException("Requested action '" + event.getOptions().get(0).getName() + "' is not one of: " + Arrays.toString(WhitelistRequest.Action.values())));
        }

        switch (action) {
            case ADD -> { return add(event); }
            case LIST -> { return list(event); }
            default -> { return Mono.error(new UserException("Requested action '" + action + "' has not been implemented.")); }
        }
    }

    private Mono<WhitelistResponse> makeRequest(WhitelistRequest request) {
        try {
            return Mono.justOrEmpty(restTemplate.postForObject(apiUrl + "/whitelist", request, WhitelistResponse.class));
        } catch (HttpClientErrorException e) {
            log.error("Error while making request to API.", e);
            if (e.getStatusCode().is4xxClientError()) {
                return Mono.error(new UserException("Bad client request.", e));
            } else if (e.getStatusCode().is5xxServerError()) {
                return Mono.error(new UserException("Remote server error.", e));
            } else {
                return Mono.error(new UserException("Unknown error.", e));
            }
        }catch (ResourceAccessException e) {
            log.error("Error while connecting to API.", e);
            return Mono.error(new UserException("Unable to connect to API.", e));
        }
    }

    private Mono<Void> add(ChatInputInteractionEvent event) {
        if (event.getOptions().get(0).getOption("name").isEmpty() && event.getOptions().get(0).getOption("uuid").isEmpty())
            return Mono.error(new UserException("Argument 'name' or 'uuid' must be supplied."));

        String name = event.getOptions().get(0).getOption("name")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(s -> s.replaceAll("[^A-Za-z0-9_]", ""))
                .orElse(null);

        UUID uuid;
        try {
            uuid = event.getOptions().get(0).getOption("uuid")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asString)
                    .map(UUID::fromString)
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return Mono.error(new UserException("UUID in unexpected format. Format: 00000000-0000-0000-0000-000000000000"));
        }

        if (name != null && uuid != null)
            return Mono.error(new UserException("Only supply either name OR uuid."));

        Long userId = event.getInteraction().getUser().getId().asLong();

        // Ensure the discord user is registered to the given MC player
        Optional<Player> playerLookup = playerDao.findById(userId);
        if (playerLookup.isPresent()) {
            if (playerLookup.get().getUuid() != null) {
                if (uuid == null) {
                    return event.reply("You registered with your MC UUID, use that instead.").withEphemeral(true);
                } else if (uuid.equals(playerLookup.get().getUuid())) {
                    // Add to whitelist
                    WhitelistRequest request = new WhitelistRequest(WhitelistRequest.Action.ADD, uuid);
                    return makeRequest(request).flatMap(response -> event.reply(response.userMessage()).withEphemeral(true));
                } else {
                    return event.reply("Given UUID does not match registered UUID. Ask an admin for assistance.").withEphemeral(true);
                }
            } else if (playerLookup.get().getUsername() != null) {
                if (name == null) {
                    return event.reply("You registered with your MC username, use that instead.").withEphemeral(true);
                } else if (name.equalsIgnoreCase(playerLookup.get().getUsername())) {
                    // Add to whitelist
                    WhitelistRequest request = new WhitelistRequest(WhitelistRequest.Action.ADD, name);
                    return makeRequest(request).flatMap(response -> event.reply(response.userMessage()).withEphemeral(true));
                } else {
                    return event.reply("Given MC username does not match registered MC username. Ask an admin for assistance.").withEphemeral(true);
                }
            } else {
                // Somehow in DB without username or uuid, fall through to default register/whitelist
            }
        } else {
            // Never registered and saved in DB, fall through to default register/whitelist
        }

        playerDao.save(new Player(userId, name, uuid));
        WhitelistRequest request = new WhitelistRequest(WhitelistRequest.Action.ADD, uuid != null ? uuid : name);
        return makeRequest(request).flatMap(response -> event.reply(response.userMessage()).withEphemeral(true));
    }

    private Mono<Void> list(ChatInputInteractionEvent event) {
        WhitelistRequest request = new WhitelistRequest(WhitelistRequest.Action.LIST);
        return makeRequest(request).flatMap(response -> event.reply(response.userMessage()).withEphemeral(true));
    }
}
