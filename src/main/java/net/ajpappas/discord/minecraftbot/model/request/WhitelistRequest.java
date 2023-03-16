package net.ajpappas.discord.minecraftbot.model.request;

public record WhitelistRequest(Action action, Object... arguments) {
    public enum Action {
        ADD, LIST, OFF, ON, RELOAD, REMOVE
    }
}
