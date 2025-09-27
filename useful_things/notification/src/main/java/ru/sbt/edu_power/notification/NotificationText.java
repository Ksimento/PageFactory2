package ru.sbt.edu_power.notification;

public class NotificationText {

    public static String getNotificationText(final DalyProjectValue dalyProjectValue) {
        if ("".equals(dalyProjectValue.smokeUrlStand)) {
            final String URL_CONF_DALY_INFO_EDU = dalyProjectValue.getHttpViewpage() + dalyProjectValue.getDalyInfo();
            final String URL_CONF_DALY_TABLE_EDU = dalyProjectValue.getHttpViewpage() + dalyProjectValue.getDalyTable();
            return String.format(
                    "Привет, ты на этой неделе дежуришь:tada: , поздравляю тебя с этой интересной задачей!\n" +
                    "Возможно ты удивлен в том что твоя очередь пришла:man-shrugging:, можешь заглянуть на эту страницу и убедиться в этом -> [График дежурств](%s)\n" +
                    "Если забыл что входит в обязанности дежурного, то тебе сюда :books: -> [Инструкция по дежурству](%s)  \n" +
                    "Не забывай комментировать ночной прогон:night_with_stars: по smoke тестам\n" +
                    "Желаю тебе удачи:tada:! У тебя все получится:success:!!!Отчалил:mechanical_arm:",
                    URL_CONF_DALY_TABLE_EDU,
                    URL_CONF_DALY_INFO_EDU
            );
        } else {
            return String.format(":alert: Требуется проверить прохождение смок тестов для проекта \"%s\":alert: \n" +
                                 "На стенде \"%s\"\n" +
                                 "JobSmoke - \"%s\"\n",
                    dalyProjectValue.name(),
                    dalyProjectValue.smokeUrlStand,
                    dalyProjectValue.smokeJobUrl);
        }
    }
}
