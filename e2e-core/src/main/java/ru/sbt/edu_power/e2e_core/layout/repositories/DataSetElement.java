package ru.sbt.edu_power.e2e_core.layout.repositories;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.layout.enums.MeasuringTypes;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Класс реализует элемент дата-сета
 */
@Getter
@Setter
@Slf4j
public class DataSetElement {
    private final String uuid;
    private final MeasuringTypes measuringType;
    private final String elementName;
    private boolean ignored;
    private final DataSetRepository params;
    private final DataSetPositionRepository position;
    private transient DataSetContainerRepository container;
    private final transient WebElement element;
    private String parent;
    private final Set<String> children = new HashSet<>();

    public DataSetElement(
            final MeasuringTypes measuringType,
            final String elementName,
            final DataSetRepository params,
            final DataSetPositionRepository position,
            final DataSetContainerRepository container,
            final WebElement element
    ) {
        uuid = UUID.randomUUID().toString();
        this.measuringType = measuringType;
        this.elementName = elementName;
        this.ignored = false;
        this.params = params;
        this.position = position;
        this.container = container;
        this.element = element;
    }

    public DataSetElement(final JsonObject jsonObject) {
        uuid = jsonObject.get("uuid").getAsString();
        measuringType = MeasuringTypes.getMeasuringType(jsonObject.get("measuringType").getAsString());
        elementName = jsonObject.get("elementName").getAsString();
        ignored = jsonObject.get("ignored").getAsBoolean();
        params = restoreDataSetFromJson(jsonObject.getAsJsonObject("params"), measuringType);
        position = new Gson().fromJson(jsonObject.getAsJsonObject("position"), DataSetPositionRepository.class);
        container = null;
        element = null;
        parent = jsonObject.get("parent").getAsString();
        for (final JsonElement element : jsonObject.getAsJsonArray("children")) {
            children.add(element.getAsString());
        }
    }

    public String getParent() {
        return parent;
    }

    public Set<String> getChildren() {
        return children;
    }

    public void setParent(final String parent) {
        this.parent = parent;
    }

    public void addChild(final String child) {
        this.children.add(child);
    }

    public List<String> matchByParams(final DataSetElement actualElement) {
        if (isIgnored()) {
            return new ArrayList<>();
        }
        check(actualElement);
        final List<String> paramErrorList = new ArrayList<>();
        if (this.getMeasuringType() != MeasuringTypes.POSITION) {
            paramErrorList.addAll(getParams().match(actualElement.getParams()));
        }
        if (!paramErrorList.isEmpty()) {
            addElementTextToErrorList(actualElement, paramErrorList);
        }
        return paramErrorList;
    }

    public List<String> matchByPosition(final DataSetElement actualElement) {
        if (isIgnored()) {
            return new ArrayList<>();
        }
        check(actualElement);
        final List<String> positionErrorList = getPosition().match(actualElement.getPosition());
        if (!positionErrorList.isEmpty()) {
            addElementTextToErrorList(actualElement, positionErrorList);
        }
        return positionErrorList;
    }

    private void addElementTextToErrorList(final DataSetElement actualDataSetElement, final List<String> allErrorsList) {
        if (actualDataSetElement.getParams() instanceof DataSetTextRepository) {
            allErrorsList.add("Текущий текст в элементе: " +
                              ((DataSetTextRepository) actualDataSetElement.getParams()).getContent() + "\n");
        }
    }

    private void check(final DataSetElement actualElement) {
        if (isIgnored()) {
            return;
        }
        Assert.assertEquals(
                "В ожидаемом элементе не установлен контейнер из актуального",
                getContainer(),
                actualElement.getContainer()
        );
        if (getMeasuringType() != actualElement.getMeasuringType()) {
            throw new AutotestError(String.format(
                    "Нарушен контекст в элементе датасета или нарушен порядок элементов на странице. " +
                    "Рекомендуется переделать тест-сет.\nОжидаемый тип элемента: %s\nФактический тип элемента: %s",
                    getMeasuringType(),
                    actualElement.getMeasuringType()
            ));
        }
        if (!getElementName().equals(actualElement.getElementName())) {
            throw new AutotestError(String.format(
                    "Нарушен контекст в элементе датасета.\nОжидаемое название элемента: %s\nФактическое название элемента: %s",
                    getElementName(),
                    actualElement.getElementName()
            ));
        }
    }

    private DataSetRepository restoreDataSetFromJson(final JsonObject jsonObject, final MeasuringTypes type) {
        final Class<? extends DataSetRepository> repositoryClass;
        switch (type) {
            case KATEX:
            case TEXT:
                repositoryClass = DataSetTextRepository.class;
                break;
            case SVG:
                repositoryClass = DataSetSvgRepository.class;
                break;
            case DECOR:
                repositoryClass = DataSetDecorRepository.class;
                break;
            case FORMS:
                repositoryClass = DataSetFormsRepository.class;
                break;
            case IMAGE:
                repositoryClass = DataSetImageRepository.class;
                break;
            case BEFORE_AFTER:
                repositoryClass = DataSetBeforeAfterRepository.class;
                break;
            case CHAMOMILEITEMS:
                repositoryClass = DataSetChamomileItemsRepository.class;
                break;
            case LIBRARY:
                repositoryClass = DataSetLibraryRepository.class;
                break;
            case COMPOSITION:
                repositoryClass = DataSetCompositionRepository.class;
                break;
            case POSITION:
                return null;
            default:
                throw new AutotestError("Тип элемента не может быть создан в этом методе " + type.name());
        }
        return new Gson().fromJson(jsonObject, repositoryClass);
    }

    @Override
    public final int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DataSetElement) {
            return this.getJson().equals(((DataSetElement) obj).getJson());
        }
        return false;
    }

    private String getJson() {
        final Gson gson = new Gson();
        return gson.toJson(this);
    }

    public boolean equalsByParams(final DataSetElement dataSet) {
        return Objects.equals(this.getParams(), dataSet.getParams());
    }

    public boolean equalsByContext(final DataSetElement dataSet) {
        return this.getMeasuringType() == dataSet.getMeasuringType() &&
               this.getElementName().equals(dataSet.getElementName());
    }

    @Override
    public String toString() {
        return "Measuring type: " + getMeasuringType().name() +
               "\nElement name: " + getElementName() +
               "\nParams: \n" + getParams() +
               "\nPosition: \n" + getPosition();
    }
}
