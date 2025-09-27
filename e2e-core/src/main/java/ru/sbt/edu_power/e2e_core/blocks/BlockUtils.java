package ru.sbt.edu_power.e2e_core.blocks;

import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.widgets.Widget;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.PageManager;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BlockUtils {
    /**
     * Условно любой путь по блокам до элемента можно разбить на некие паттерны,
     * мы их будем использовать для разбора пути и информировании об ошибках в написании путей
     * <p>
     * Условные обозначения:
     * block - название блока
     * field - поле в этом блоке
     * value - значение поля (полученное со страницы с помощью getText())
     * number - номер блока (для навигации по номеру)
     * <p>
     * Возможные паттерны:
     * <p>
     * BLOCK_LIST: block
     * IDENTIFIED_BLOCK: block->number или block->field#value
     * ELEMENT_IN_BLOCK: block->number->field или block->field#value->field
     * <p>
     * Возможные последовательности паттернов в пути:
     * <p>
     * BLOCK_LIST
     * IDENTIFIED_BLOCK
     * ELEMENT_IN_BLOCK
     * IDENTIFIED_BLOCK->BLOCK_LIST
     * IDENTIFIED_BLOCK->IDENTIFIED_BLOCK
     * IDENTIFIED_BLOCK->ELEMENT_IN_BLOCK
     * IDENTIFIED_BLOCK->IDENTIFIED_BLOCK->BLOCK_LIST
     * IDENTIFIED_BLOCK->IDENTIFIED_BLOCK->IDENTIFIED_BLOCK
     * IDENTIFIED_BLOCK->IDENTIFIED_BLOCK->ELEMENT_IN_BLOCK
     * и так далее: последовательности любой длины с IDENTIFIED_BLOCK с начала до предпоследнего и заканчивается на любой паттерн
     * <p>
     * В зависимости от последнего (или единственного) звена последовательности,
     * будем получать типы элементов:
     * BLOCK_LIST - список из блоков
     * IDENTIFIED_BLOCK - экземпляр блока (соответствующий условиям)
     * ELEMENT_IN_BLOCK - экземпляр элемента из найденного блока
     *
     * @param path путь в формате "список модулей->название#Модуль 1->длительность"
     * @return список паттернов
     */
    public static List<Pattern> getPattern(final String path) {
        final List<Pattern> patterns = new ArrayList<>();
        String context = PageContext.getCurrentPage().getClass().getName();
        if (path.contains("->")) {
            final List<String> pathComponents = Arrays.asList(path.split("->"));
            final Iterator<String> iterator = pathComponents.iterator();
            int counter = 0;
            while (iterator.hasNext()) {
                final String firstItem = iterator.next();
                if (isBlockList(firstItem, context)) {
                    if (iterator.hasNext()) {
                        final String draftSecondItem = iterator.next();
                        final String secondItem;
                        if (draftSecondItem.startsWith("stash")) {
                            secondItem = DataProcessing.decodeValue(draftSecondItem);
                        } else {
                            secondItem = draftSecondItem;
                        }
                        counter++;
                        context = getElementClassName(firstItem, context);
                        if (!secondItem.contains("#") && !"last".equals(secondItem) && !isNumber(secondItem)) {
                            throw new AutotestError(
                                    String.format(
                                            "Неверный аргумент для поиска блока: \"%s\". Возможные аргументы: номер блока, поле#значение, last",
                                            secondItem
                                    )
                            );
                        }
                        if (secondItem.contains("#")) {
                            getFieldByName(secondItem.substring(0, secondItem.indexOf("#")), context);
                        }
                        if (iterator.hasNext()) {
                            counter++;
                            if (isBlockList(pathComponents.get(counter), context)) {
                                patterns.add(Pattern.IDENTIFIED_BLOCK);
                                continue;
                            }
                            final String fieldName = iterator.next();
                            if (isWidget(fieldName, context)) {
                                patterns.add(Pattern.WIDGET_IN_BLOCK);
                                // если обнаружен виджет, то дальше разбирать нельзя, так как начинается структура виджета
                                break;
                            }
                            patterns.add(Pattern.ELEMENT_IN_BLOCK);
                        } else {
                            patterns.add(Pattern.IDENTIFIED_BLOCK);
                        }
                    } else {
                        patterns.add(Pattern.BLOCK_LIST);
                    }
                } else {
                    throw new AutotestError(String.format("\"%s\" должен быть блоком", firstItem));
                }
            }
        } else {
            if (isBlockList(path, context)) {
                patterns.add(Pattern.BLOCK_LIST);
            } else {
                patterns.add(Pattern.NON_BLOCK);
            }
        }
        return patterns;
    }

    static boolean isNumber(final String stringNumber) {
        try {
            Integer.parseInt(stringNumber);
            return true;
        } catch (final NumberFormatException ignored) {

        }
        return false;
    }

    /**
     * Метод проверяет, является ли поле списком из блоков
     */
    static boolean isBlockList(final String name, final String context) {
        return getFieldByName(name, context).getType().getTypeName().contains("List");
    }

    /**
     * Метод проверяет, является ли поле виджетом
     */
    static boolean isWidget(final String name, final String context) {
        return Widget.class.isAssignableFrom(getFieldByName(name, context).getType());
    }

    static String getElementClassName(final String name, final String context) {
        final Field field = getFieldByName(name, context);
        String className = field.getGenericType().getTypeName();
        if (className.contains("<")) {
            className = className.substring(className.indexOf("<") + 1, className.indexOf(">"));
        }
        return className;
    }

    static Field getFieldByNameFromPage(final String name, final Class<? extends Page> pageClass) {
        for (final Map.Entry<Field, String> field : PageManager.getPageRepository().get(pageClass).entrySet()) {
            if (name.equals(field.getValue())) {
                return field.getKey();
            }
        }
        throw new AutotestError(String.format("Для страницы \"%s\" не найден элемент \"%s\"", pageClass, name));
    }

    static Field getFieldByNameFromBlock(final String name, final Class<? extends BlockInitialized> blockClass) {
        for (final Field field : blockClass.getFields()) {
            if (field.isAnnotationPresent(ElementTitle.class)
                && field.getAnnotation(ElementTitle.class).value().equals(name)) {
                return field;
            }
        }
        throw new AutotestError(String.format(
                "Поле \"%s\" не найдено для контекста \"%s\"",
                name,
                blockClass.getName()
        ));
    }

    static Field getFieldByName(final String name, final String context) {
        try {
            final Class<? extends Page> contextClass = (Class<? extends Page>) Class.forName(context);
            final Field[] fields = contextClass.getFields();
            for (final Field field : fields) {
                if (field.isAnnotationPresent(ElementTitle.class)
                    && field.getAnnotation(ElementTitle.class).value().equals(name)) {
                    return field;
                }
            }
        } catch (final ClassNotFoundException e) {
            throw new BlockExtractorException("Неверно указан контекст для поиска поля", e);
        }
        throw new NoElementFoundInBlockContext(String.format("Поле \"%s\" не найдено для контекста \"%s\"", name, context));
    }

    public static <T> Class<? extends T> getClass(final String className) {
        try {
            return (Class<? extends T>) Class.forName(className);
        } catch (final ClassNotFoundException e) {
            throw new AutotestError(e);
        }
    }

    public enum Pattern {
        BLOCK_LIST(1),
        IDENTIFIED_BLOCK(2),
        ELEMENT_IN_BLOCK(3),
        WIDGET_IN_BLOCK(4),
        NON_BLOCK(-1);
        private final int pathComponents;

        Pattern(final int pathComponents) {
            this.pathComponents = pathComponents;
        }

        public int getPathComponents() {
            return pathComponents;
        }
    }
}
