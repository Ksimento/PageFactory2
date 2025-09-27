package ru.sbt.edu_power.e2e_core.smoke_layout;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbt.edu_power.e2e_core.smoke.EndPoints;
import ru.sbt.edu_power.e2e_core.smoke.Roles;
import ru.sbt.edu_power.e2e_core.smoke.RoutEvaluator;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.DataVolume;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.Layout;
import ru.sbt.edu_power.e2e_core.smoke_layout.misc.LayoutDefault;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.annotations.PageEntry;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class PageConfiguration {
    private final List<Roles> roles = new ArrayList<>();
    private final String title;
    private String functional;
    private String pageEntry;
    private final List<ElementConfiguration> elementConfigurationList = new ArrayList<>();
    private final Class<? extends Page> page;
    private final Map<Field, String> fields;
    private List<String> featurePath;
    private final String uuid;
    private final LayoutDefault layoutDefault;

    public PageConfiguration(final Class<? extends Page> page, final Map<Field, String> fields, final String uuid) {
        this.page = page;
        this.fields = fields;
        this.title = page.getAnnotation(PageEntry.class).title();
        layoutDefault = page.isAnnotationPresent(LayoutDefault.class) ? page.getAnnotation(LayoutDefault.class) : null;
        construct();
        collectElementConfigurations();
        this.uuid = uuid;
    }


    // Метод генерирует мапу из вариаций для сценариев (по размеру экрана и полноте наполнения)
    // на список элементов для проверки. Список элементов отсортирован в порядке, требуемом для сценария
    public Map<Variant, List<LayoutElement>> getVariants() {
        final Map<Variant, List<LayoutElement>> map = new HashMap<>();

        elementConfigurationList
                .stream()
                .map(ElementConfiguration::getLayoutElements)
                .flatMap(List::stream)
                .forEach(le -> {
                    roles.forEach(role -> {
                        final Variant variant = new Variant(role, le.getDimensionEnum(), le.getDataVolume());
                        if (!map.containsKey(variant)) {
                            map.put(variant, new ArrayList<>());
                        }
                        map.get(variant).add(le);
                    });

                });
        map.forEach((v, list) -> {
            list.sort(Comparator.naturalOrder());
        });
        return map;
    }

    private void construct() {
        if (!page.isAnnotationPresent(EndPoints.class)) {
            throw new AutotestError("Отсутствует аннотация @EndPoints для страницы " + page.getName());
        }
        if (page.getAnnotation(EndPoints.class).ignored()) {
            throw new AutotestError(
                    "Страница не может быть использована для тестирования верстки, так как @EndPoints в состоянии ignored: " +
                    page.getName());
        }
        roles.addAll(RoutEvaluator.getRoleToRouteMap(page).keySet());
        setFunctional();
        setFeaturePath();
        setPageEntry();
    }

    private void setFunctional() {
        functional = String.join(". ", getPathFromPackage());
    }

    private String upperFirstChar(final String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void collectElementConfigurations() {
        fields.entrySet()
              .stream()
              .filter(f -> f.getKey().isAnnotationPresent(Layout.class))
              .map(e -> new ElementConfiguration(e.getKey(), e.getValue(), layoutDefault))
              .forEach(elementConfigurationList::add);
    }

    private List<String> getPathFromPackage() {
        final List<String> pathFromPackage = Arrays.asList(page.getName().split("pages.")[1].split("\\."));
        return pathFromPackage.subList(0, pathFromPackage.size() - 1)
                              .stream()
                              .map(this::upperFirstChar)
                              .collect(Collectors.toList());
    }

    private void setFeaturePath() {
        featurePath = getPathFromPackage();
    }

    private void setPageEntry() {
        pageEntry = page.getAnnotation(PageEntry.class).title();
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static class Variant implements Comparable<Variant> {
        private final Roles role;
        private final DimensionEnum dimensionEnum;
        private final DataVolume dataVolume;

        @Override
        public int compareTo(final Variant o) {
            if (dimensionEnum == o.dimensionEnum) {
                if (dataVolume == o.dataVolume) {
                    return role.compareTo(o.role);
                } else {
                    return dataVolume.compareTo(o.dataVolume);
                }
            } else {
                return dimensionEnum.compareTo(o.dimensionEnum);
            }
        }
    }
}
