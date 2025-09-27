package ru.sbt.edu_power.e2e_core.layout.repositories;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.allure.AllureUtils;
import ru.sbtqa.tag.pagefactory.allure.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель данных для измерений типа COMPOSITION
 */
@Getter
public class DataSetCompositionRepository extends DataSetRepository {
    private final int vectorCompositionHash;
    private final byte[] image;

    public DataSetCompositionRepository(
            final int vectorCompositionHash,
            final byte[] image
    ) {
        this.vectorCompositionHash = vectorCompositionHash;
        this.image = image;
    }

    @Override
    List<String> match(final DataSetRepository dataSet) {
        final List<String> allErrorsList = new ArrayList<>();
        if (vectorCompositionHash != ((DataSetCompositionRepository) dataSet).getVectorCompositionHash()) {
            allErrorsList.add("Векторная композици отличается от оригинала");
        }
        AllureUtils.attachCompressedScreenShotToAllure("Ожидаемая SVG композиция", image, Type.JPEG);
        AllureUtils.attachCompressedScreenShotToAllure(
                "Фактическая SVG композиция",
                ((DataSetCompositionRepository) dataSet).getImage(),
                Type.JPEG
        );
        return allErrorsList;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DataSetCompositionRepository) {
            return vectorCompositionHash == ((DataSetCompositionRepository) obj).getVectorCompositionHash();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return vectorCompositionHash;
    }
}
