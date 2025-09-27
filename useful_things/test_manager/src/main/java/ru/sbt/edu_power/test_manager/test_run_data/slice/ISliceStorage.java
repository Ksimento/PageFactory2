package ru.sbt.edu_power.test_manager.test_run_data.slice;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;

public interface ISliceStorage<R, T extends ISlice<R>> extends Collection<T> {
    // Получить последний элемент среза
    T getLastSlice();

    // Выполнить новый срез
    void slice();

    // Получить время последнего обновления
    LocalDateTime getLastSliceTime();

    // Получить повременной массив данных
    LinkedHashMap<LocalDateTime, T> getTimeBasedData();
}
