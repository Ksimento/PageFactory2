package ru.sbt.edu_power.external_services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class ESUtils {
    /**
     * Метод вызывает остановку потока на указанное количество миллисекунд
     *
     * @param millis длительность остановки в миллисекундах
     */
    public static void freeze(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            throw new ExternalServicesException("Слип сломался", e);
        }
    }

    //    Метод выполняет фильтрацию списка, удаляя из него повторяющиеся значения
    public static <T> Predicate<T> distinctByKey(
            final Function<? super T, ?> keyExtractor
    ) {
        final Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
