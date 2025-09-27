package ru.sbt.edu_power.allure_comparator.timeline;

import java.util.*;

public class ErrorScale extends AbstractScale {
    private final WorkloadScale workloadScale = new WorkloadScale();
    private final Map<Long, Integer> errorScale = new LinkedHashMap<>();

    @Override
    public void generateScale(long start, long end, int steps) {
        super.generateScale(start, end, steps);
        workloadScale.generateScale(start, end, steps);
    }

    @Override
    public void insert(final TimelineElement element) {
        if (Objects.isNull(element.getFailedTime())) {
            workloadScale.insert(element);
        }
        insert(position -> Objects.nonNull(element.getFailedTime())
                && position <= element.getFailedTime()
                && (position + getScaleStep()) > element.getFailedTime());
    }

    @Override
    public List<Integer> getScaleData() {
        if (errorScale.isEmpty()) {
            calculateErrorScale();
        }
        return new ArrayList<>(errorScale.values());
    }

    @Override
    public int getMax() {
        return 100;
    }

    @Override
    public int getMin() {
        return 0;
    }

    private void calculateErrorScale() {
        final int maxWorkload = workloadScale.getMax();
        final int lowWorkload = Math.max(maxWorkload * 7 / 100, 3);
        workloadScale.getMap().forEach((k, v) -> {
            final int sum = v + getValue(k);
            if (sum <= lowWorkload) {
                errorScale.put(k, 0);
                return;
            }
            final int errorPercent = sum == 0 ? 0 : getValue(k) * 100 / sum;
            errorScale.put(k, errorPercent);
        });
    }
}
