package ru.sbt.edu_power.assist_bot.services.slice;

import java.time.LocalDateTime;
import java.util.ArrayList;

public abstract class Slice<T> extends ArrayList<T> implements ISlice<T> {
    private static final long serialVersionUID = -5224641230394713069L;
    private final LocalDateTime time = LocalDateTime.now();

    @Override
    public void optimize(final ISlice<T> previous) {
        new ArrayList<>(this).forEach(e ->
                previous.forEach(pe -> {
                    if (e.equals(pe)) {
                        this.remove(e);
                        this.add(pe);
                    }
                })
        );
    }

    @Override
    public LocalDateTime getTime() {
        return time;
    }
}
