package ru.sbt.edu_power.assist_bot.services.slice;

import java.time.LocalDateTime;

public interface ISlice<T> extends Iterable<T> {
    // Загрузка или генерация данных для среза
    void load();
    // Обновление данных среза из локального источника
    void update(Iterable<T> dataForUpdate);
    // Оптимизация хранилища данных
    void optimize(ISlice<T> previous);
    // Получить дату и время создания среза
    LocalDateTime getTime();
}
