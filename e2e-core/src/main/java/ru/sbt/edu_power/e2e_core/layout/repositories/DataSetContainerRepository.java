package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;

import java.util.Objects;

/**
 * Модель данных для сохранения расположения контейнера
 */
@Getter
public class DataSetContainerRepository extends DataSetRepository {
    private final int top;
    private final int left;

    public DataSetContainerRepository(
            final int top,
            final int left
    ) {
        this.top = top;
        this.left = left;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DataSetContainerRepository) {
            return matchField(top, ((DataSetContainerRepository) obj).getTop())
                    && matchField(left, ((DataSetContainerRepository) obj).getLeft());
        }
        return false;
    }

    public boolean equalsByPosition(final Object obj) {
        if (obj instanceof DataSetContainerRepository) {
            return matchField(top, ((DataSetContainerRepository) obj).getTop())
                   && matchField(left, ((DataSetContainerRepository) obj).getLeft());
        }
        return false;
    }

    private boolean matchField(final int actual, final int expected) {
        final int error = 4;
        return Math.pow((actual - expected), 2) <= error;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), top, left);
    }
}
