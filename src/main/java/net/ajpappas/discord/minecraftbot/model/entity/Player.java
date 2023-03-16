package net.ajpappas.discord.minecraftbot.model.entity;

import java.util.UUID;

public record Player(String username, UUID uuid) {

    @Override
    public String toString() {
        if (uuid != null) {
            return uuid.toString();
        } else {
            return username;
        }
    }
}
