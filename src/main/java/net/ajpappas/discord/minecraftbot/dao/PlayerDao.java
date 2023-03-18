package net.ajpappas.discord.minecraftbot.dao;


import net.ajpappas.discord.minecraftbot.model.entity.Player;
import org.springframework.data.repository.CrudRepository;

public interface PlayerDao extends CrudRepository<Player, Long> {
}
