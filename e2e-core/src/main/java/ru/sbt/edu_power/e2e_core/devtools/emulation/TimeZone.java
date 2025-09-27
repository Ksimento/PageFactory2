package ru.sbt.edu_power.e2e_core.devtools.emulation;

import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.devtools.DevTools;

import java.util.HashMap;
import java.util.Map;

/**
 * Класс реализует переключение часового пояса в браузере "на лету"
 */
public class TimeZone {
    private static final Map<Integer, String> TIME_ZONE_MAP = getTimeZoneMap();

    // Установка часового пояса по его смещению (от -12 до +12)
    public static void set(final int timeZoneOffset) {
        Assert.assertTrue(
                "Выбранная таймзона сейчас не поддерживается, необходимо её добавить в фреймворк",
                TIME_ZONE_MAP.containsKey(timeZoneOffset)
        );
        DevTools.getEmulation().setTimezoneOverride(TIME_ZONE_MAP.get(timeZoneOffset));
    }

    private static Map<Integer, String> getTimeZoneMap() {
        final Map<Integer, String> timeZoneMap = new HashMap<>();
        timeZoneMap.put(-2, "America/Noronha");
        timeZoneMap.put(-1, "Atlantic/Cape_Verde");
        timeZoneMap.put(0, "UTC");
        timeZoneMap.put(1, "Europe/Stockholm");
        timeZoneMap.put(2, "Europe/Kaliningrad");
        timeZoneMap.put(3, "Europe/Moscow");
        timeZoneMap.put(4, "Europe/Astrakhan");
        timeZoneMap.put(5, "Asia/Yekaterinburg");
        timeZoneMap.put(6, "Asia/Almaty");
        timeZoneMap.put(7, "Asia/Novosibirsk");
        timeZoneMap.put(8, "Asia/Irkutsk");
        timeZoneMap.put(9, "Asia/Chita");
        timeZoneMap.put(10, "Asia/Vladivostok");
        return timeZoneMap;
    }
}
