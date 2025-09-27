package ru.sbt.edu_power.e2e_core.layout.enums;

import lombok.Getter;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Типы измерений параметров вёрстки
 */
@Getter
public enum MeasuringTypes {
    // Измеряются размеры и позиция элемента относительно экрана и родителя
    POSITION(".", ElementsColor.VIOLET, false, true),
    // Получает параметры шрифта из текстовых блоков внутри элемента (каждый блок будет обработан отдельно)
    TEXT(
            "descendant-or-self::*[contains(@class, 'measuringTypeText') and not(ancestor::*[@class = 'mjx-chtml MathJax_CHTML'])]",
            ElementsColor.ORANGE,
            true,
            true
    ),
    // Получает текстовое содержимое без данных о расположении и размеров, используется для сохранения текста со страниц справки
    LIBRARY(
            "descendant-or-self::*[contains(@class, 'measuringTypeText') and not(ancestor::*[@class = 'measuringTypeText'])]",
            TEXT.color,
            false,
            false
    ),
    // SVG иконки, содержащие только <path> элементы
    SVG("descendant-or-self::*[name() = 'svg']", ElementsColor.MAGENTA, true, true),
    // CHAMOMILEITEMS Снимок Ромашки по предметам
    CHAMOMILEITEMS("descendant-or-self::*[name() = 'svg']", ElementsColor.DEFAULT, false, true),
    //    Изображения, как в тэге <img> так и в стиле background-image
    IMAGE("descendant-or-self::img | .", ElementsColor.BLUE, true, true),
    // Элементы веб-форм, такие как input, textarea, select
    FORMS(
            "descendant-or-self::input[not(@type = 'checkbox')] | descendant-or-self::button | descendant-or-self::textarea",
            ElementsColor.BROWN,
            true,
            true
    ),
    // Элементы, содержащие оформление (фоновый цвет, скругление углов, тени, рамки)
    DECOR("descendant-or-self::*[contains(@class, 'layoutDecorationElement')]", ElementsColor.NAVY, true, true),
    //    Формулы
    KATEX(
            "descendant-or-self::span[@class = 'mjx-math']//*[contains(@class, 'measuringTypeText')]",
            ElementsColor.ORANGE,
            true,
            true
    ),
    BEFORE_AFTER(
            "descendant-or-self::*[contains(@class, 'layoutAfterElement') or contains(@class, 'layoutBeforeElement')]",
            ElementsColor.NAVY,
            true,
            true
    ),
    // Специальный тип, который производит сбор сведений сразу по всем типам, отмеченным isMultitype:true
    MULTITYPE(".", ElementsColor.DEFAULT, false, true),
    // Используется для получения слепка SVG композиции (сложные SVG элементы, выходящие за рамки обычных иконок)
    COMPOSITION("(descendant-or-self::*[name() = 'svg'])[1]", ElementsColor.MAGENTA, false, true);

    private final String xpath;
    private final ElementsColor color;
    private final boolean isMultitype;
    private final boolean isVisual;

    MeasuringTypes(
            final String xpath,
            final ElementsColor color,
            final boolean isMultitype,
            final boolean isVisual
    ) {
        this.xpath = xpath;
        this.color = color;
        this.isMultitype = isMultitype;
        this.isVisual = isVisual;
    }

    public static MeasuringTypes getMeasuringType(final String measuringType) {
        return Stream.of(MeasuringTypes.values())
                     .filter(m -> measuringType.equalsIgnoreCase(m.name()))
                     .findFirst()
                     .orElseThrow(() -> new AutotestError(String.format(
                             "Неизвестный тип измерения элемента \"%s\"." +
                             "Укажите один или несколько из имеющихся через пробел, " +
                             "точку с запятой или запятую: \"%s\"", measuringType,
                             Arrays.stream(MeasuringTypes.values())
                                   .map(MeasuringTypes::name)
                                   .collect(Collectors.joining(", "))
                     )));
    }
}
