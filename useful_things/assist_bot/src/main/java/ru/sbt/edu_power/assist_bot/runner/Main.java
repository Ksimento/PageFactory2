package ru.sbt.edu_power.assist_bot.runner;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.assist_bot.roles.roles.UserRolesRepository;
import ru.sbt.edu_power.assist_bot.services.jenkins.DockerTagSearch;
import ru.sbt.edu_power.assist_bot.slack.SlackDispatcher;
import ru.sbt.edu_power.assist_bot.task_flow.QueueExecutor;

@Slf4j
public class Main {
    public static final SlackDispatcher SLACK_DISPATCHER = new SlackDispatcher();
    public static final QueueExecutor QUEUE_EXECUTOR = new QueueExecutor(true);
    public static final String BOT_TOKEN = System.getProperty("botToken");
    public static final UserRolesRepository USER_ROLES_REPOSITORY = new UserRolesRepository();

    public static void main(final String[] args) {

        // сканирование мультибранч джоб для получения всех последних докер-тегов
        new Thread(() -> DockerTagSearch.getInstance().scanRepo(), "Jenkins multibranch job scan").start();

        // регистрация кнопок управления
        final ServiceRegistration serviceRegistration = new ServiceRegistration();
        serviceRegistration.register();
        serviceRegistration.registerInitCommand();
        serviceRegistration.registerEmptyMessageChangeEvent();

        // конфигурация доступа к сервисам Atlassian
        JiraConnect.configureConnection();

        // запуск slack бота
        SLACK_DISPATCHER.execute();

        // запуск основной очереди исполнения заданий
        new Thread(QUEUE_EXECUTOR::runQueue, "Main task queue").start();
    }
}
