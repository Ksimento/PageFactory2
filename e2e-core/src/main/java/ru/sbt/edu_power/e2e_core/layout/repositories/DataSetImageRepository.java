package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;
import lombok.Setter;

/**
 * Модель данных для измерений типа IMAGE
 */
@Getter
@Setter
public class DataSetImageRepository extends DataSetRepository {
    private String src;
    private final String bgsrc;

    public DataSetImageRepository(
            final String src,
            final String bgsrc
    ) {
        this.src = getMinifyedString(src);
        this.bgsrc = getMinifyedString(bgsrc);
    }

    @Override
    public String toString() {
        return src.isEmpty() ? bgsrc : src;
    }
}
