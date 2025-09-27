package ru.sbt.edu_power.e2e_core.smoke_layout;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import ru.sbt.edu_power.e2e_core.page_tags.ParentPage;
import ru.sbt.edu_power.e2e_core.page_tags.RootPage;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.tc_verifier.NewJiraTCCollector;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFilter;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCase;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseCreation;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.annotations.PageEntry;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class TestCaseGenerator {
    private final PageConfiguration pageConfiguration;
    private final List<PageConfiguration.Variant> variants;
    private final NewJiraTCCollector collector;

    public TestCaseGenerator(
            final PageConfiguration pageConfiguration,
            final List<PageConfiguration.Variant> variants,
            final NewJiraTCCollector collector
    ) {
        this.pageConfiguration = pageConfiguration;
        this.variants = variants;
        this.collector = collector;
    }

    public TestCaseModel getTestCase() {
        final Map<String, TestCaseModel> tcMap = TCFilter
                .of(collector.asMap())
                .filter(t -> Objects.nonNull(t.getObjective()))
                .filter(t -> t.getObjective().contains(pageConfiguration.getUuid()))
                .filter(t -> t.getObjective().contains(variants.get(0).getDataVolume().name()))
                .filter(t -> t.getObjective().contains(variants.get(0).getRole().getRoleName()))
                .toMap();
        if (tcMap.size() > 1) {
            throw new AutotestError(String.format(
                    "Найдено больше одного тест-кейса с параметрами:\nUUID:%s\n%s",
                    pageConfiguration.getUuid(),
                    variants
            ));
        }
        if (tcMap.size() == 1) {
            final TestCaseModel model = tcMap.values().iterator().next();
            updateTestCase(model);
            return model;
        }
        return createTestCase();
    }

    private void updateTestCase(final TestCaseModel model) {
        final TestCase testCase = new TestCase();
        final AtomicBoolean isParamsDifferent = new AtomicBoolean(false);
        final EnumMap<TCFields, TCFields.Layout> dimensions = getDimensionMap();
        dimensions.forEach((field, value) -> {
            if (model.getLayout(field) != value) {
                isParamsDifferent.set(true);
                testCase.addCustomFieldParam(field, value.getValue());
                model.putCustomField(field, value);
            }
        });
        if (isParamsDifferent.get()) {
            JiraConnect.testCaseUpdate(model.getKey(), testCase);
        }
    }

    private TestCaseModel createTestCase() {
        final TestCaseCreation model = new TestCaseCreation();
        model.setProjectKey(variants.get(0).getRole().getProjectName());
        model.setFolder(getTestCasePath());
        final String name = String.format(
                "%s / %s / %s",
                pageConfiguration.getPageEntry(),
                variants.get(0).getRole().getRoleName(),
                variants.get(0).getDataVolume().name().toLowerCase()
        );
        model.setName(name);
        final String objective = String.format(
                "<b>!Данные ниже нужны для синхронизации с автотестами, их удалять нельзя!</b><br>%s<br>%s<br>%s",
                pageConfiguration.getUuid(),
                variants.get(0).getRole().getRoleName(),
                variants.get(0).getDataVolume().name()
        );
        model.setObjective(objective);
        model.setPriority(TCFields.Priority.LOW.value);
        model.setStatus(TCFields.Status.DRAFT.value);
        model.addCustomFieldParam(TCFields.AUTOMATED_STATUS, TCFields.AutomatedStatus.YES.value);
        model.addCustomFieldParam(TCFields.TEST_TYPE, TCFields.TestType.REGRESS.value);
        model.addCustomFieldParam(TCFields.TEST_VIEW, TCFields.TestView.LAYOUT.value);
        getDimensionMap().forEach((field, value) ->
                model.addCustomFieldParam(field, value.getValue())
        );

        final HttpResponse<JsonNode> response = JiraConnect.testCaseCreate(model);
        final String key = response.getBody().getObject().getString("key");
        return collector.getById(key);
    }

    private EnumMap<TCFields, TCFields.Layout> getDimensionMap() {
        final EnumMap<TCFields, TCFields.Layout> dimensions = new EnumMap<>(TCFields.class);
        Stream.of(
                TCFields.LAYOUT_DESKTOP,
                TCFields.LAYOUT_MOBILE_PORTRAIT,
                TCFields.LAYOUT_TABLET_PORTRAIT,
                TCFields.LAYOUT_TABLET_LANDSCAPE
        ).forEach(d -> dimensions.put(d, TCFields.Layout.NOT));
        for (final PageConfiguration.Variant variant : variants) {
            switch (variant.getDimensionEnum()) {
                case DESKTOP:
                case DEFAULT:
                    dimensions.put(TCFields.LAYOUT_DESKTOP, TCFields.Layout.YES);
                    break;
                case MOBILE_PORTRAIT:
                    dimensions.put(TCFields.LAYOUT_MOBILE_PORTRAIT, TCFields.Layout.YES);
                    break;
                case TABLET_PORTRAIT:
                    dimensions.put(TCFields.LAYOUT_TABLET_PORTRAIT, TCFields.Layout.YES);
                    break;
                case TABLET_LANDSCAPE:
                    dimensions.put(TCFields.LAYOUT_TABLET_LANDSCAPE, TCFields.Layout.YES);
                    break;
            }
        }
        return dimensions;
    }

    // метод возвращает ID папки для нового тест-кейса. Структура директорий вычисляется из @ParentPage
    // если директории отсутствуют, то они будут созданы динамически
    private String getTestCasePath() {
        final String[] folders = getFolderHierarchy();
        NewJiraTCCollector
                .getInstance()
                .getFolder(TCFields.ProjectId.valueOf(variants.get(0).getRole().getProjectName()).id)
                .createIfNotExists(folders);
        return "/" + String.join("/", folders);
    }

    private String[] getFolderHierarchy() {
        final List<String> parentPages = new ArrayList<>();
        collectParents(pageConfiguration.getPage(), parentPages);
        parentPages.add("Верстка");
        parentPages.add("Регресс");
        Collections.reverse(parentPages);
        return parentPages.toArray(new String[]{});
    }

    private void collectParents(final Class<? extends Page> page, final List<String> parentsPages) {
        if (page.isAnnotationPresent(ParentPage.class)) {
            parentsPages.add(page.getAnnotation(PageEntry.class).title());
            if (RootPage.class.isAssignableFrom(page.getAnnotation(ParentPage.class).pageClass()[0].getSuperclass())) {
                parentsPages.add(variants.get(0).getRole().name());
                return;
            }
            collectParents(page.getAnnotation(ParentPage.class).pageClass()[0], parentsPages);
        }
    }
}
