package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Модель данных для измерений типа POSITION
 */
@Getter
public class DataSetPositionRepository extends DataSetRepository {
    private final int width;
    private final int height;
    private final int top;
    private final int left;

    public DataSetPositionRepository(
            final int width,
            final int height,
            final int top,
            final int left
    ) {
        this.width = width;
        this.height = height;
        this.top = top;
        this.left = left;
    }

    @Override
    public boolean equals(final Object obj) {
        return equals(obj, 0);
    }

    public boolean equals(final Object obj, final long pixelPrecision) {
        if (obj instanceof DataSetPositionRepository) {
            final boolean size = matchField(width, ((DataSetPositionRepository) obj).getWidth(), pixelPrecision)
                                 && matchField(height, ((DataSetPositionRepository) obj).getHeight(), pixelPrecision);
            return size && matchField(top, ((DataSetPositionRepository) obj).getTop(), pixelPrecision)
                                     && matchField(left, ((DataSetPositionRepository) obj).getLeft(), pixelPrecision);
        }
        return false;
    }

    @Override
    List<String> match(final DataSetRepository dataSet) {
        final List<String> allErrorsList = new ArrayList<>();
        final Map<String, Object> matching = dataSet.getDataMap();
        getDataMap().forEach((k, v) -> {
            if (!matchField((Integer) v, (Integer) matching.get(k), 0)) {
                allErrorsList.add(k + ": " + v + " != " + matching.get(k));
            }
        });
        return allErrorsList;
    }

    public boolean equalsByPosition(final Object obj, final long pixelPrecision) {
        if (obj instanceof DataSetPositionRepository) {
            return matchField(top, ((DataSetPositionRepository) obj).getTop(), pixelPrecision)
                   && matchField(left, ((DataSetPositionRepository) obj).getLeft(), pixelPrecision);
        }
        return false;
    }

    private boolean matchField(final int actual, final int expected, final long pixelPrecision) {
        final long max = Math.max(actual, expected);
        if (max == 0) {
            return true;
        }
        final long min = Math.min(actual, expected);
        return 100L - (min * 100L) / max <= pixelPrecision || (max - min) <= pixelPrecision;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), width, height, top, left);
    }
}
