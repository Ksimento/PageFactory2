package ru.sbt.edu_power.e2e_core.smoke_layout;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.layout.enums.MeasuringTypes;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.DataVolume;

import java.util.List;

@Getter
@AllArgsConstructor
public class LayoutElement implements Comparable<LayoutElement> {
    private final String name;
    private final DimensionEnum dimensionEnum;
    private final MeasuringTypes measuringTypes;
    private final List<Integer> blocks;
    private final DataVolume dataVolume;
    private final boolean filtered;
    private final List<String> ignoreElementText;
    private final boolean isBlockElement;

    @Override
    public int compareTo(final LayoutElement o) {
        if (measuringTypes == o.getMeasuringTypes()) {
            return filtered ? -1 : 1;
        }
        if (measuringTypes == MeasuringTypes.POSITION) {
            return -1;
        }
        if (o.measuringTypes == MeasuringTypes.POSITION) {
            return 1;
        }
        if (filtered == o.filtered) {
            return 0;
        }
        return filtered ? -1 : 1;
    }

    public boolean isEqualExcludeDimension(final LayoutElement other) {
        return name.equals(other.name) &&
               measuringTypes == other.measuringTypes &&
               blocks.equals(other.blocks) &&
               dataVolume == other.dataVolume &&
               filtered == other.filtered &&
               ignoreElementText.equals(other.ignoreElementText);
    }

    @Override
    public String toString() {
        return "LayoutElement{" +
               "name='" + name + '\'' +
               ", dimensionEnum=" + dimensionEnum +
               ", measuringTypes=" + measuringTypes +
               ", blocks=" + blocks +
               ", dataVolume=" + dataVolume +
               ", filtered=" + filtered +
               ", ignoreElementText=" + ignoreElementText +
               ", isBlockElement=" + isBlockElement +
               '}';
    }
}
