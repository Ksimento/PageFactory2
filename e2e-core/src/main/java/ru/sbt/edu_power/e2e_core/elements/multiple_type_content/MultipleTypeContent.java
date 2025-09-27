package ru.sbt.edu_power.e2e_core.elements.multiple_type_content;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Класс реализует возможность определить содержимое HTML документа и сравнить с ожидаемым
 * Поддерживаемые типы элементов - изображения (img), фрейм (iframe), текст (p, h1-h6), KATEX формулы (по классу katex)
 * Для текста сравнивается содержимое каждого отдельного тега (p, h1-h6)
 * Для katex сравнивается содержимое отрендеренной формулы целиком
 * Для изображения сравнивается имя файла изображения
 * Для фрейма сравнивается адрес страницы
 * <p>
 * Допустимые типы контента:
 * IMAGE
 * KATEX
 * TEXT
 * IFRAME
 * <p>
 * Для валидации содержимого требуется перечислить через точку с запятой все элементы контента в виде
 * type : value
 * Например:
 * TEXT : проверяемый текст;IMAGE : image_name.jpg;TEXT : stash#value_01
 */
public class MultipleTypeContent extends TypifiedElement implements Validatable {
    private final Map<ContentType, List<WebElement>> elementsMap = new EnumMap<>(ContentType.class);
    private final Map<ContentType, List<String>> valuesMap = new EnumMap<>(ContentType.class);

    public MultipleTypeContent(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public String getText() {
        return toString(getValuesMap());
    }

    @Override
    public String getFieldValue() {
        return toString(getValuesMap());
    }

    @Override
    public boolean validate(final String expectedData) {
        final Map<ContentType, List<String>> expectedMap = toMap(expectedData);
        if (!expectedData.startsWith("*")) {
            final String expected = toString(expectedMap);
            final String actual = toString(getValuesMap());
            return expected.equals(actual);
        }
        if (!getValuesMap().keySet().containsAll(expectedMap.keySet())) {
            return false;
        }
        return expectedMap
                .keySet()
                .stream()
                .allMatch(contentType ->
                    expectedMap.get(contentType).stream()
                               .allMatch(v -> Validator.matchValueInList(getValuesMap().get(contentType), v))
                );
    }

    private String toString(final Map<ContentType, List<String>> map) {
        return map.keySet().stream()
                  .sorted()
                  .flatMap(contentType -> map
                          .get(contentType)
                          .stream()
                          .sorted()
                          .map(v -> contentType + " : " + v))
                  .collect(Collectors.joining("; "));
    }

    private Map<ContentType, List<String>> toMap(final String data) {
        final Map<ContentType, List<String>> map = new EnumMap<>(ContentType.class);
        Arrays.stream(data.replaceAll("^\\*", "").split(";"))
              .forEach(e -> {
                  final String[] parts = DataProcessing.decodeValue(e).split(":", 2);
                  if (parts.length < 2) {
                      throw new AutotestError(String.format("Значение \"%s\" должно быть представлено в виде 'CONTENT_TYPE : value'", e));
                  }
                  final ContentType contentType = getContentTypeByName(parts[0].trim());
                  if (!map.containsKey(contentType)) {
                      map.put(contentType, new ArrayList<>());
                  }
                  map.get(contentType).add(DataProcessing.decodeValue(parts[1]));
              });
        return map;
    }

    private ContentType getContentTypeByName(final String name) {
        for (final ContentType contentType : ContentType.values()) {
            if (name.equalsIgnoreCase(contentType.name())) {
                return contentType;
            }
        }
        throw new AutotestError(String.format(
                "Неподдерживаемый тип контента: \"%s\"\n" +
                "Доступные значения: %s",
                name,
                Arrays.stream(ContentType.values())
                      .map(Enum::name).collect(Collectors.joining(", "))
        ));
    }

    private Map<ContentType, List<String>> getValuesMap() {
        if (valuesMap.isEmpty()) {
            getElementsMap().keySet().forEach(contentType ->
                    valuesMap.put(contentType, getValueList(contentType)));
        }
        return valuesMap;
    }

    private List<String> getValueList(final ContentType contentType) {
        final List<String> valueList;
        switch (contentType) {
            case TEXT:
                valueList = getElementsMap()
                        .get(contentType)
                        .stream()
                        .map(WebElement::getText)
                        .flatMap(v -> Arrays.stream(v.split("\\n")))
                        .filter(v -> !v.isEmpty())
                        .collect(Collectors.toList());
                break;
            case IMAGE:
            case AUDIO:
            case VIDEO:
                valueList = getElementsMap()
                        .get(contentType)
                        .stream()
                        .map(element -> {
                            final String[] srcParts = element
                                    .findElement(By.xpath(contentType.getValueElementXpath()))
                                    .getAttribute("src").split("/");
                            return srcParts[srcParts.length - 1];
                        })
                        .collect(Collectors.toList());
                break;
            case IFRAME:
                valueList = getElementsMap()
                        .get(contentType)
                        .stream()
                        .map(element -> element
                                .findElement(By.xpath(contentType.getValueElementXpath()))
                                .getAttribute("src"))
                        .collect(Collectors.toList());
                break;
            case PDF:
                valueList = getElementsMap()
                        .get(contentType)
                        .stream()
                        .map(element -> element.getAttribute("type"))
                        .collect(Collectors.toList());
                break;
            case KATEX:
                valueList = getElementsMap()
                        .get(contentType)
                        .stream()
                        .map(element -> element
                                .findElement(By.xpath(contentType.getValueElementXpath()))
                                .getText().replaceAll("\\n", ""))
                        .collect(Collectors.toList());
                break;
            default:
                throw new AutotestError(String.format(
                        "Получение значений для типа \"%s\" не реализовано",
                        contentType
                ));
        }
        return valueList;
    }

    private Map<ContentType, List<WebElement>> getElementsMap() {
        if (elementsMap.isEmpty()) {
            for (final ContentType contentType : ContentType.values()) {
                final List<WebElement> elements = getWrappedElement()
                        .findElements(By.xpath(contentType.getIdentificationXpath()));
                if (elements.isEmpty()) {
                    continue;
                }
                elementsMap.put(contentType, elements);
            }
        }
        return elementsMap;
    }
}
