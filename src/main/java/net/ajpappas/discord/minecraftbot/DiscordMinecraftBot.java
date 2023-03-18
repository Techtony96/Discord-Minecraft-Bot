package net.ajpappas.discord.minecraftbot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.rest.RestClient;
import jakarta.annotation.PostConstruct;
import net.ajpappas.discord.minecraftbot.dao.PlayerDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;


@SpringBootApplication(scanBasePackages = "net.ajpappas.discord")
public class DiscordMinecraftBot implements CommandLineRunner {

    private static final Logger log = LogManager.getLogger();

    @Value("${bot.token}")
    private String botToken;

    @Value("${git.commit.id.describe-short}")
    private String botVersion;

    public static void main(String[] args) {
        SpringApplication.run(DiscordMinecraftBot.class, args);
    }

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {
        return DiscordClientBuilder.create(botToken).build()
                .gateway()
                .login()
                .block();
    }

    @Bean
    public RestClient discordRestClient(GatewayDiscordClient client) {
        return client.getRestClient();
    }

    @PostConstruct
    public void startupApplication() {
        log.info("Running version {}", botVersion);
    }

    @Override
    public void run(String... args) throws Exception {
        // Prevent Spring from shutting down immediately after start up
        Thread.currentThread().join();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propsConfig = new PropertySourcesPlaceholderConfigurer();
        propsConfig.setLocation(new ClassPathResource("git.properties"));
        propsConfig.setIgnoreResourceNotFound(true);
        propsConfig.setIgnoreUnresolvablePlaceholders(true);
        return propsConfig;
    }
}
