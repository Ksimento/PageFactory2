package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.user_data;

import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Класс реализует подсчёт рабочего времени сотрудника с учётом перехода на следующий рабочий день
// За основу затраченного времени берётся время старта и завершения тест-кейсов.
// Пропуски менее 90 минут считаются так же рабочим временем
public class WorkTime {
    // дата и время начала самого первого ручного теста - оно же время начала ручного регресса сотрудником
    private final LocalDateTime startRegression;
    private final List<DateRange> dateRangeList = new ArrayList<>();
    // Промежуток времени который считается перерывом в работе
    public static final Duration IDLE_TIME = Duration.ofMinutes(90L);

    public WorkTime(final LocalDateTime startRegression) {
        this.startRegression = startRegression;
    }

    // присоединяем промежуток времени
    public void join(final Date start, final Date end) {
        final LocalDateTime localDateTimeStart = LocalDateTime.ofInstant(start.toInstant(), ZoneId.of("UTC"));
        final LocalDateTime localDateTimeEnd = LocalDateTime.ofInstant(end.toInstant(), ZoneId.of("UTC"));
        // если текущий список промежутков пуст или если разрыв в промежутках более 90 минут - используем входящее время начала
        if (dateRangeList.isEmpty() || getLastRange().getEnd().plus(IDLE_TIME).isBefore(localDateTimeStart)) {
            dateRangeList.add(new DateRange(localDateTimeStart, localDateTimeEnd));
        } else {
            // иначе в качестве начала промежутка используем время завершения последнего промежутка
            dateRangeList.add(new DateRange(getLastRange().end, localDateTimeEnd));
        }
    }

    private DateRange getLastRange() {
        return dateRangeList.get(dateRangeList.size() - 1);
    }

    // метод возвращает дату заврешения последнего промежутка
    public Date getLastDate() {
        return dateRangeList.isEmpty() ? null : Date.from(getLastRange().getEnd().toInstant(ZoneOffset.UTC));
    }

    // метод возвращает общую длительность рабочего времени
    public Duration getWorkTime() {
        return dateRangeList
                .stream()
                .map(DateRange::getDuration)
                .reduce(Duration::plus)
                .orElse(Duration.ofSeconds(0));
    }

    public LocalDateTime getStartRegression() {
        return startRegression;
    }

    // класс реализует временной промежуток, сохраняя при этом не саму длительность, а время начала и завершения
    @Getter
    public static class DateRange {
        private final LocalDateTime start;
        private final LocalDateTime end;

        public DateRange(final LocalDateTime start, final LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        public Duration getDuration() {
            return Duration.between(start, end);
        }
    }
}
