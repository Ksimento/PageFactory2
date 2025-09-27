package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель данных для измерений типа LIBRARY
 */
@Getter
public class DataSetLibraryRepository extends DataSetRepository {
    private final String content;

    public DataSetLibraryRepository(
            final String content
    ) {
        this.content = content;
    }

    @Override
    List<String> match(final DataSetRepository dataSet) {
        final List<String> errors = new ArrayList<>();
        if (!this.equals(dataSet)) {
            errors.add(String.format(
                    "\nОжидаемое значение:\n\t\"%s\"\nФактическое значение:\n\t\"%s\"",
                    this,
                    dataSet
            ));
        }
        return errors;
    }

    @Override
    public String toString() {
        return content;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DataSetLibraryRepository) {
            return hashCode() == obj.hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}