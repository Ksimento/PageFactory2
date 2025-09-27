package ru.sbt.edu_power.e2e_core.layout;

import lombok.Getter;
import lombok.Setter;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetElement;

@Getter
@Setter
public class ElementPair {
    private final DataSetElement expected;
    private final DataSetElement actual;
    // признак для идентичных элементов (тип измерения, название, параметры, размер и позиция)
    private final boolean isEquals;
    // признак для элементов находящихся в одном контексте (тип измерения и название элементов равны)
    private final boolean isRelated;
    // признак для элементов с идентичным контентом
    private final boolean isEqualContent;
    // признак для элементов со схожими DataSetPositionRepository но с разным контентом
    private boolean isDifferentParameters;
    // признак для элементов с идентичным контентом, но разными DataSetPositionRepository
    private boolean isDifferentSizeOrPosition;
    // признак для элементов не сопостовимых друг с другом
    private boolean isFullDifferent;

    public ElementPair(final DataSetElement expected, final DataSetElement actual) {
        this.expected = expected;
        this.actual = actual;
        isRelated = contextCompare();
        isEqualContent = isRelated && expected.equalsByParams(actual);
        isEquals = isEqualContent && expected.getPosition().equals(actual.getPosition());
        if (isRelated) {
            // для элементов из одного контекста устанавливаем контейнер в expected из actual
            expected.setContainer(actual.getContainer());
        }

    }

    public boolean comparePosition(final long precision) {
        return expected.getPosition().equals(actual.getPosition(), precision);
    }

    public boolean compareContent() {
        return expected.getParams().equals(actual.getParams());
    }

    public boolean contains(final DataSetElement element) {
        return expected.equalsByParams(element) || actual.equals(element);
    }

    public void setDifferentParameters() {
        isDifferentParameters = true;
    }

    public void setFullDifferent() {
        isFullDifferent = true;
    }

    public void setDifferentSizeOrPosition() {
        isDifferentSizeOrPosition = true;
    }

    private boolean contextCompare() {
        return expected.getElementName().equals(actual.getElementName()) &&
               expected.getMeasuringType() == actual.getMeasuringType();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ElementPair pair = (ElementPair) o;

        return hashCode() == pair.hashCode();
    }

    @Override
    public int hashCode() {
        int result = expected.hashCode();
        result = 31 * result + actual.hashCode();
        return result;
    }
}
