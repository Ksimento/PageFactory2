package ru.sbt.edu_power.e2e_core.test_runner;

import lombok.extern.slf4j.Slf4j;
import ru.sbtqa.tag.pagefactory.environment.Environment;

/**
 * Класс реализует запуск тестов на двух стендах по принципу round-robin, то-есть, последовательно предоставляя
 * очередному тесту то первый то второй стенд без всякого учёта загрузки
 * Что бы функционал работал, тест нужно запускать с параметром -DsecondStandUrl=https://devXX.pcbltools.ru - в качестве
 * второго стенда
 */
@Slf4j
public class StandSplitter {
    private static boolean switcher;
    private static final boolean ONLY_ONE_STAND = isOnlyOneStand();
    private static final String FIRST_STAND = System.getProperty("webdriver.starting.url");
    private static final String SECOND_STAND = System.getProperty("webdriver.starting.url.second");

    public static synchronized void getStand() {
        if (ONLY_ONE_STAND) {
            return;
        }
        switcher = !switcher;
        Environment.getDriverService().getDriver().get(switcher ? FIRST_STAND : SECOND_STAND);
    }

    private static boolean isOnlyOneStand() {
        return "".equals(System.getProperty("webdriver.starting.url.second"));
    }
}
