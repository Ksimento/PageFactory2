package ru.sbt.edu_power.assist_bot.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.socket_mode.SocketModeClient;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.runner.Main;

import java.io.IOException;

@Slf4j
public class SlackDispatcher {
    private final App app;
    private static final String APP_TOKEN = System.getProperty("appToken");

    public SlackDispatcher() {
        final AppConfig appConfig = AppConfig.builder()
                                             .singleTeamBotToken(Main.BOT_TOKEN)
                                             .build();
        app = new App(appConfig);
    }

    public App getApp() {
        return app;
    }

    public void execute() {
        log.info("APP_TOKEN {}......", APP_TOKEN.substring(0, APP_TOKEN.length() - 6));
        log.info("BOT_TOKEN {}......", Main.BOT_TOKEN.substring(0, Main.BOT_TOKEN.length() - 6));
        final SocketModeApp socketModeApp;
        try {
            socketModeApp = new SocketModeApp(
                    APP_TOKEN,
                    SocketModeClient.Backend.Tyrus,
                    app
            );
        } catch (final IOException e) {
            throw new AssistBotException(e);
        }
        final Thread slackBolt = new Thread(() -> {
            try {
                socketModeApp.start();
            } catch (final Exception e) {
                throw new AssistBotException(e);
            }
        }, "Slack Bolt");
        slackBolt.start();
    }
}
