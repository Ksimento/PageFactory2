package ru.sbt.edu_power.e2e_core.smoke_layout;

import lombok.Getter;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.layout.enums.MeasuringTypes;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.DataVolume;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.Layout;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.LayoutDefault;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
public class ElementConfiguration {
    private final List<DimensionEnum> dimensions = new ArrayList<>();
    private final List<MeasuringTypes> measuringTypes = new ArrayList<>();
    private final List<Integer> blocks = new ArrayList<>();
    private final List<DataVolume> dataVolumes = new ArrayList<>();
    private boolean filtered;
    private final Field field;
    private final String name;
    private final List<String> ignoreElementText = new ArrayList<>();
    private final LayoutDefault layoutDefault;

    public ElementConfiguration(final Field field, final String name, final LayoutDefault layoutDefault) {
        this.field = field;
        this.name = name;
        this.layoutDefault = layoutDefault;
        collectDimensions();
        collectMeasuringType();
        collectBlockNumber();
        collectDataVolume();
        setFiltered();
    }

    public List<LayoutElement> getLayoutElements() {
        final List<LayoutElement> elements = new ArrayList<>();
        dimensions.forEach(dimension ->
                measuringTypes.forEach(measuring ->
                        dataVolumes.forEach(data ->
                                elements.add(
                                        new LayoutElement(
                                                name,
                                                dimension,
                                                measuring,
                                                blocks,
                                                data,
                                                filtered,
                                                ignoreElementText,
                                                isBlockElement()
                                        )
                                )
                        )
                )
        );
        return elements;
    }

    private boolean isBlockElement() {
        return List.class.isAssignableFrom(field.getType());
    }

    private void collectDimensions() {
        if (
                !field.getAnnotation(Layout.class).override() &&
                field.getAnnotation(Layout.class).dimension().length == 1 &&
                field.getAnnotation(Layout.class).dimension()[0] == DimensionEnum.DESKTOP
        ) {
            if (Objects.nonNull(layoutDefault) &&
                (layoutDefault.dimension().length > 1 || layoutDefault.dimension()[0] != DimensionEnum.DEFAULT)
            ) {
                dimensions.addAll(Arrays.asList(layoutDefault.dimension()));
                return;
            }
        }
        dimensions.addAll(Arrays.asList(field.getAnnotation(Layout.class).dimension()));
    }

    private void collectMeasuringType() {
        if (
                !field.getAnnotation(Layout.class).override() &&
                field.getAnnotation(Layout.class).type().length == 1 &&
                field.getAnnotation(Layout.class).type()[0] == MeasuringTypes.MULTITYPE
        ) {
            if (Objects.nonNull(layoutDefault) &&
                (layoutDefault.type().length > 1 || layoutDefault.type()[0] != MeasuringTypes.MULTITYPE)
            ) {
                measuringTypes.addAll(Arrays.asList(layoutDefault.type()));
                return;
            }
        }
        measuringTypes.addAll(Arrays.asList(field.getAnnotation(Layout.class).type()));
    }

    private void collectBlockNumber() {
        Arrays.stream(field.getAnnotation(Layout.class).blocks()).forEach(blocks::add);
    }

    private void collectDataVolume() {
        if (
                !field.getAnnotation(Layout.class).override() &&
                field.getAnnotation(Layout.class).data().length == 1 &&
                field.getAnnotation(Layout.class).data()[0] == DataVolume.NORMAL
        ) {
            if (Objects.nonNull(layoutDefault) &&
                (layoutDefault.data().length > 1 || layoutDefault.data()[0] != DataVolume.NORMAL)
            ) {
                dataVolumes.addAll(Arrays.asList(layoutDefault.data()));
                return;
            }
        }
        dataVolumes.addAll(Arrays.asList(field.getAnnotation(Layout.class).data()));
    }

    private void setFiltered() {
        if (
                !field.getAnnotation(Layout.class).override() &&
                !field.getAnnotation(Layout.class).filtered()
        ) {
            if (
                    Objects.nonNull(layoutDefault) &&
                    !field.getAnnotation(Layout.class).filtered()
            ) {
                filtered = layoutDefault.filtered();
                return;
            }
        }
        filtered = field.getAnnotation(Layout.class).filtered();
    }
}
