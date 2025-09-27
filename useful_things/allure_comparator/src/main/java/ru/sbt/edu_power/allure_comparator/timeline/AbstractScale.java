package ru.sbt.edu_power.allure_comparator.timeline;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractScale implements Scale {
    private int scaleStepsNumber;
    private long scaleStep;
    private final Map<Long, AtomicInteger> scale = new LinkedHashMap<>();

    @Override
    public void generateScale(final long start, final long end, final int steps) {
        scaleStepsNumber = steps;;
        final long length = end - start;
        scaleStep = length / steps;
        for (long position = start; position < end; position += scaleStep) {
            scale.put(position, new AtomicInteger(0));
        }
    }

    public int getValue(final long position) {
        return scale.get(position).get();
    }

    @Override
    public Map<Long, Integer> getMap() {
        return scale.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(), (a, b) -> b, LinkedHashMap::new));
    }

    protected void insert(final Function<Long, Boolean> insertionFunction) {
        scale.forEach((position, counter) -> {
            if (insertionFunction.apply(position)) {
                counter.incrementAndGet();
            }
        });
    }

    @Override
    public int getScaleStepsNumber() {
        return scaleStepsNumber;
    }

    @Override
    public int getMax() {
        return scale.values()
                .stream()
                .mapToInt(AtomicInteger::get)
                .max()
                .orElse(0);
    }

    @Override
    public int getMin() {
        return scale.values()
                .stream()
                .mapToInt(AtomicInteger::get)
                .min()
                .orElse(0);
    }

    @Override
    public List<Integer> getScaleData() {
        return scale.values()
                .stream()
                .map(AtomicInteger::get)
                .collect(Collectors.toList());
    }

    @Override
    public long getScaleStep() {
        return scaleStep;
    }
}
