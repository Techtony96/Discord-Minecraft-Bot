package net.ajpappas.discord.minecraftbot.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhitelistResponse(String userMessage) {
}
