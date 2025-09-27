package ru.sbt.edu_power.e2e_core.layout;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import ru.sbt.edu_power.e2e_core.layout.enums.TestingMode;
import ru.sbt.edu_power.e2e_core.layout.repositories.DataSetElement;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

public final class DataSetCollection extends ArrayList<DataSetElement> {
    private static final long serialVersionUID = -734314832956644540L;
    private final ElementPairCollection elementPairs = new ElementPairCollection();

    public DataSetCollection() {
    }

    public DataSetCollection(final String json) {
        final Type type = new TypeToken<Set<Object>>() {
        }.getType();
        final Gson gson = new Gson();
        for (final JsonElement element : gson.toJsonTree(gson.fromJson(json, type)).getAsJsonArray()) {
            add(new DataSetElement(element.getAsJsonObject()));
        }
    }

    public String getJson() {
        return new Gson().toJson(this);
    }

    // Текущая коллекция содержит ожидаемые данные, сравниваемая - актуальные (полученные из браузера)
    public void match(final DataSetCollection actualCollection, final TestingMode testingMode) {
        forEach(expected ->
                actualCollection.forEach(actual -> elementPairs.add(new ElementPair(expected, actual)))
        );
        elementPairs.normalizeCollection(testingMode);
        this.removeIf(elementPairs.getExpectedValidElements()::contains);
        actualCollection.removeIf(elementPairs.getActualValidElements()::contains);
    }

    public DataSetCollection getRootCollection() {
        final DataSetCollection rootCollection = new DataSetCollection();
        stream().filter(e -> e.getParent().isEmpty())
                .forEach(rootCollection::add);
        return rootCollection;
    }

    public DataSetCollection getChildren(final String parentUuid) {
        return stream()
                .filter(e -> parentUuid.equals(e.getParent()))
                .collect(Collectors.toCollection(DataSetCollection::new));
    }

    public ElementPairCollection getElementPairs() {
        return elementPairs;
    }

    public boolean isUuidExists(final String uuid) {
        return stream().map(DataSetElement::getUuid).allMatch(uuid::equals);
    }

    public DataSetElement getByUuid(final String uuid) {
        return stream()
                .filter(e -> uuid.equals(e.getUuid()))
                .findFirst()
                .orElseThrow(() -> new AutotestError("Не найден элемент с UUID " + uuid));
    }
}
