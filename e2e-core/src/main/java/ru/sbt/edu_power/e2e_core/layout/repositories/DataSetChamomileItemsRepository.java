package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;

/**
 * Модель данных для виджета Ромашки по предметам
 */
@Getter
public class DataSetChamomileItemsRepository extends DataSetRepository{
    private final String petals;

    public DataSetChamomileItemsRepository(
            final String petals
    ) {
        this.petals = petals;
    }
}
