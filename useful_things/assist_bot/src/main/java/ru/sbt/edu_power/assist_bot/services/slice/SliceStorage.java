package ru.sbt.edu_power.assist_bot.services.slice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public abstract class SliceStorage<R, T extends ISlice<R>> extends ArrayList<T> implements ISliceStorage<R, T> {
    private static final long serialVersionUID = 3086449884418284226L;

    @Override
    public T getLastSlice() {
        return get(size() - 1);
    }

    @Override
    public LocalDateTime getLastSliceTime() {
        return getLastSlice().getTime();
    }

    @Override
    public LinkedHashMap<LocalDateTime, T> getTimeBasedData() {
        return stream().collect(Collectors.toMap(ISlice::getTime, s -> s, (a, b) -> a, LinkedHashMap::new));
    }
}
