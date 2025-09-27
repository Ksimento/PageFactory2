package ru.sbt.edu_power.external_services.validator;

import org.junit.Assert;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class Validator {
    public static boolean matchValues(final Object actual, final Object expected) {
        return matchValues(actual, expected, false);
    }

    public static boolean matchValues(final Object actual, final Object expected, final boolean ignoreCase) {
        Assert.assertNotNull("Фактическое значение не может быть null", actual);
        Assert.assertNotNull("Ожидаемое значение не может быть null", expected);
        Assert.assertEquals("Значения должны быть одинакового типа", expected.getClass(), actual.getClass());
        final boolean matchResult;
        if (actual instanceof String) {
            final String cleanedActualValue = ((String) actual).replaceAll("\\p{Z}", " ").replaceAll("\u00AD", "");
            if ("*".equals(expected)) {
                matchResult = true;
            } else if ("***".equals(expected)) {
                matchResult = !cleanedActualValue.isEmpty();
            } else {
                final String containingValue = ((String) expected).replaceAll("\\*", "");
                final String containingValueWithCase = ignoreCase ? containingValue.toLowerCase() : containingValue;
                final String expectedWithCase = ignoreCase ? ((String) expected).toLowerCase() : (String) expected;
                final String actualWithCase = ignoreCase ? cleanedActualValue.toLowerCase() : cleanedActualValue;
                if (expectedWithCase.endsWith("*")) {
                    if (expectedWithCase.startsWith("*")) {
                        matchResult = actualWithCase.contains(containingValueWithCase);
                    } else {
                        matchResult = actualWithCase.startsWith(containingValueWithCase);
                    }
                } else if (expectedWithCase.startsWith("*")) {
                    matchResult = actualWithCase.endsWith(containingValueWithCase);
                } else {
                    matchResult = actualWithCase.equals(expectedWithCase);
                }
            }
        } else {
            matchResult = actual.equals(expected);
        }
        return matchResult;
    }

    public static boolean matchValueInList(final List<String> actualValues, final String expected) {
        return actualValues.stream().anyMatch(actual -> matchValues(actual, expected));
    }

    /**
     * Метод сравнивает два объекта, полученных из "Stash", один из которых является списком с помощью заданной операции сравнения: "входит", "=", "!="
     *
     * @param stashKey1 первый ключ
     * @param stashKey2 второй ключ
     * @param obj1      объект с левой стороны выражения
     * @param obj2      объект с правой стороны выражения
     * @param predicate операция сравнения
     */
    public static void compareListsByPredicate(
            final String stashKey1,
            final String stashKey2,
            final Object obj1,
            final Object obj2,
            final String predicate
    ) {
        final boolean result;
        if (obj1 instanceof List && obj2 instanceof List) {
            final PredicateSymbol predicateSymbol = PredicateSymbol.valueOfSymbol("равно".equals(predicate) ? "=" : predicate);
            result = matchListsByPredicate(
                    new TreeSet<Object>((Collection<?>) obj1),
                    new TreeSet<Object>((Collection<?>) obj2),
                    predicateSymbol
            );

            Assert.assertTrue(String.format(
                    "Список из кода '%s' не соответствует списку из кода '%s', операция сравнения '%s'",
                    stashKey1,
                    stashKey2,
                    predicate

            ), result);
        } else if (!(obj1 instanceof List) && obj2 instanceof List) {
            if (obj1 instanceof String) {
                result = matchValueInList((List) obj2, (String) obj1);
                Assert.assertTrue(String.format(
                        "Строка из кода '%s' не соответствует списку из кода '%s', операция сравнения '%s'",
                        stashKey1,
                        stashKey2,
                        predicate

                ), result);
                return;
            }
            throw new ValidateException(String.format(
                    "Код '%s' не является списком или строкой",
                    stashKey1
            ));
        } else if (obj1 instanceof List && !(obj2 instanceof List)) {
            if (obj2 instanceof String) {
                result = matchValueInList((List) obj1, (String) obj2);
                Assert.assertTrue(String.format(
                        "Строка из кода '%s' не соответствует списку из кода '%s', операция сравнения '%s'",
                        stashKey2,
                        stashKey1,
                        predicate

                ), result);
                return;
            }
            throw new ValidateException(String.format(
                    "Код '%s' не является списком или строкой",
                    stashKey2
            ));
        }
    }

    /**
     * Метод сравнивает два списка с помощью заданной операции сравнения: "входит", "=", "!="
     *
     * @param l1              Список с левой стороны выражения
     * @param l2              Список с правой стороны выражения
     * @param predicateSymbol Операция сравнения
     * @return Булево true если выражение верно
     */
    private static boolean matchListsByPredicate(
            final TreeSet<Object> l1,
            final TreeSet<Object> l2,
            final PredicateSymbol predicateSymbol
    ) {
        switch (predicateSymbol) {
            case EQUALS:
                return l2.equals(l1);
            case NOT_EQUALS:
                return !l2.equals(l1);
            case SUBLIST_OF:
                return l2.containsAll(l1);
            default:
                throw new ValidateException(String.format(
                        "Для операции \"%s\" не реализовано сравнение списков",
                        predicateSymbol
                ));
        }
    }

    // проверяет сортировку списка данных
    public static <T extends Comparable<T>> void checkSortData(
            final String sortDirection,
            final List<String> data,
            final String fieldFormat
    ) {
        final List<Object> actualList = data
                .stream()
                .map(String::toLowerCase)
                .map(text -> convert(validValue(text), fieldFormat))
                .collect(Collectors.toList());
        final List<Object> expectedList = new ArrayList<>(actualList);
        final SortDirection direction = SortDirection.getConst(sortDirection);
        if (direction == SortDirection.ASC) {
            expectedList.sort(Comparator.comparing(o -> ((T) o)));
        } else if (direction == SortDirection.DESC) {
            expectedList.sort((o1, o2) -> ((T) o2).compareTo((T) o1));
        }
        for (int i = 0; i < actualList.size(); i++) {
            Assert.assertEquals("Сортировка не соответствует ожидаемой", actualList.get(i), expectedList.get(i));
        }

    }

    public static DateFormatSymbols getNewSortMonths(final String locale) {
        final DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(new Locale(locale));
        String[] newShortMonths;
        switch (locale.toLowerCase().trim()) {
            case "ru":
                newShortMonths = new String[]{
                        "янв",
                        "фев",
                        "мар",
                        "апр",
                        "май",
                        "июн",
                        "июл",
                        "авг",
                        "сен",
                        "окт",
                        "ноя",
                        "дек"
                };
                break;
            case "en":
            case "es":
                newShortMonths = new String[]{
                        "jan",
                        "feb",
                        "mar",
                        "apr",
                        "may",
                        "jun",
                        "jul",
                        "aug",
                        "dep",
                        "oct",
                        "nov",
                        "dec"
                };
                break;
            default:
                newShortMonths = new String[]{};
        }
        if (newShortMonths.length != 0) {
            dateFormatSymbols.setShortMonths(newShortMonths);
        }
        return dateFormatSymbols;
    }


    /**
     * Метод сравнивает два значения с помощью знаков сравнения =, <, >, <=, >=, !=
     *
     * @param leftSideValue   Значение double с левой стороны выражения
     * @param rightSideValue  Значение double с правой стороны выражения
     * @param predicateSymbol Знак сравнения
     * @return Булево true если выражение верно
     */
    public static boolean matchByPredicate(
            final double leftSideValue,
            final double rightSideValue,
            final PredicateSymbol predicateSymbol
    ) {
        final double PRECISION_VALUE = 0.0000001;
        switch (predicateSymbol) {
            case EQUALS:
                return Math.abs(leftSideValue - rightSideValue) <= PRECISION_VALUE;
            case LESS:
                return leftSideValue < rightSideValue;
            case MORE:
                return leftSideValue > rightSideValue;
            case NOT_EQUALS:
                return Math.abs(leftSideValue - rightSideValue) > PRECISION_VALUE;
            case LESS_OR_EQUALS:
                return leftSideValue <= rightSideValue;
            case MORE_OR_EQUALS:
                return leftSideValue >= rightSideValue;
            default:
                throw new ValidateException(String.format(
                        "Для символа \"%s\" не реализовано проверки",
                        predicateSymbol
                ));
        }
    }

    // приводит данные к нужному формату
    private static Object convert(final String text, final String fieldFormat) {
        final FieldFormat format = FieldFormat.getConst(fieldFormat.split("#")[0]);
        switch (format) {
            case STRING:
                return text;
            case NUMBER:
                return Double.valueOf(text.split(" ")[0]);
            case DATE:
                final DateFormatSymbols oldShortMonths = getNewSortMonths("ru");
                final SimpleDateFormat dateFormat = new SimpleDateFormat(fieldFormat.split("#")[1], oldShortMonths);
                try {
                    return dateFormat.parse(text);
                } catch (final ParseException e) {
                    throw new ValidateException(e);
                }
            default:
                throw new ValidateException(String.format(
                        "Для формата \"%s\" обработчик не определён",
                        fieldFormat
                ));
        }
    }

    public static String validValue(final String text){
        return  text.replaceAll("\n", " ")
                    .replaceAll("\u00AD", "")
                    .replaceAll("\u00A0", " ");
    }

    public enum SortDirection {
        ASC("возрастанию"),
        DESC("убыванию");
        private final String direction;

        SortDirection(final String direction) {
            this.direction = direction;
        }

        public String getDirection() {
            return direction;
        }

        public static SortDirection getConst(final String direction) {
            for (final SortDirection sortDirection : SortDirection.values()) {
                if (direction.equalsIgnoreCase(sortDirection.getDirection())) {
                    return sortDirection;
                }
            }
            throw new ValidateException("Нет константы с аргументом " + direction);
        }
    }

    public enum FieldFormat {
        NUMBER("число"),
        DATE("дата"),
        STRING("строка");

        private final String format;

        FieldFormat(final String format) {
            this.format = format;
        }

        public String getFormat() {
            return format;
        }

        public static FieldFormat getConst(final String format) {
            for (final FieldFormat fieldFormat : FieldFormat.values()) {
                if (format.equalsIgnoreCase(fieldFormat.getFormat())) {
                    return fieldFormat;
                }
            }
            throw new ValidateException("Нет константы с аргументом " + format);
        }
    }

    public enum PredicateSymbol {
        EQUALS("="),
        LESS("<"),
        LESS_OR_EQUALS("<="),
        MORE(">"),
        MORE_OR_EQUALS(">="),
        NOT_EQUALS("!="),
        SUBLIST_OF("входит");

        private final String symbol;

        PredicateSymbol(final String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public static PredicateSymbol valueOfSymbol(final String symbol) {
            for (final PredicateSymbol predicateSymbol : PredicateSymbol.values()) {
                if (symbol.equals(predicateSymbol.getSymbol())) {
                    return predicateSymbol;
                }
            }
            throw new ValidateException(String.format("Для символа \"%s\" не установлено никакого значения", symbol));
        }
    }
}
