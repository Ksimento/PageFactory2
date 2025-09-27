package ru.sbt.edu_power.e2e_core.smoke_layout;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Getter
public class VariantGroup {
    private final List<PageConfiguration.Variant> variants = new ArrayList<>();
    private final List<LayoutElement> elements;

    public VariantGroup(final List<LayoutElement> elements) {
        this.elements = Collections.unmodifiableList(elements);
    }

    public void addVariant(final PageConfiguration.Variant variant) {
        variants.add(variant);
    }

    public static void updateCollection(
            final List<VariantGroup> variantGroups,
            final PageConfiguration.Variant variant,
            final List<LayoutElement> elements
    ) {
        if (!variantGroups.isEmpty()) {
            final Optional<VariantGroup> variantGroup = variantGroups
                    .stream()
                    .filter(vg -> isEqualVariants(vg.getVariants().get(0), vg.getElements(), variant, elements))
                    .findAny();
            if (variantGroup.isPresent()) {
                variantGroup.get().addVariant(variant);
                return;
            }
        }
        final VariantGroup variantGroup = new VariantGroup(elements);

        variantGroup.addVariant(variant);
        variantGroups.add(variantGroup);
    }

    private static boolean isEqualVariants(
            final PageConfiguration.Variant variantA,
            final List<LayoutElement> elementsA,
            final PageConfiguration.Variant variantB,
            final List<LayoutElement> elementsB
    ) {
        if (!isElementListEquals(elementsA, elementsB)) {
            return false;
        }
        return variantA.getRole() == variantB.getRole() && variantA.getDataVolume() == variantB.getDataVolume();
    }

    private static boolean isElementListEquals(final List<LayoutElement> elementsA, final List<LayoutElement> elementsB) {
        if (elementsA.size() != elementsB.size()) {
            return false;
        }
        return IntStream
                .range(0, elementsA.size())
                .allMatch(i -> elementsA.get(i).isEqualExcludeDimension(elementsB.get(i)));
    }
}
