package ru.sbt.edu_power.e2e_core.table_processing;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractor;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.checkbox.CheckBox;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.error_processing.ErrorCollector;
import ru.sbt.edu_power.e2e_core.fields.FieldUtils;
import ru.sbt.edu_power.e2e_core.fields.FindUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.pagefactory.utils.HtmlElementUtils;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TextBlock;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Класс реализует работу с произвольными таблицами.
 * Для обеспечения работы методов класса, необходимо
 * к описанию элемента добавить аннотации:
 * <p>
 * FindTableHeaderCol(xpath = "") - этот xpath должен находить все элементы заголовков
 * таблицы (даже если заголовок пустой, например первый)
 * FindTableCell(xpath = "") - этот xpath должен находить все элементы ячеек таблицы,
 * как в заголовке, так и в строках. При этом количество полученных элементов должно быть
 * равно произведению количества столбцов на количество строк (включая строку с заголовками столбцов)
 * <p>
 * При написании таблицы в сценарии, первый (и возможно второй) столбцы - используются для поиска нужной строки,
 * поэтому первым столбцом должен идти тот, который содержит в себе уникальное значение. Если уникального нет, то
 * используем второй столбец для получения уникальной пары значений.
 * <p>
 * Класс позволяет выполнить проверку наличия строки в таблице (сверяет соответствие предоставленных значений столбцов
 * реальным значениям из таблицы)
 * <p>
 * Можно использовать wildcard в начале или конце строки (или с обоих сторон) - символ звёздочки. В этом случае
 * будет проверяться на вхождение, а не на эквивалентность. Если вместо значения указать звёздочку, то валидным будет
 * любое значение поля.
 * <p>
 * Класс позволяет выполнить построчное заполнение пустой таблицы
 * <p>
 * Если колонка не имеет названия, тогда в таблице сценария её нужно назвать empty
 */
@Slf4j
public class TableActions {
    // Веб-элемент таблицы
    private final WebElement tableElement;
    // xpath ячеек заголовков таблицы
    private final String headerColXpath;
    // xpath ячеек таблицы кроме заголовков
    private final String cellXpath;
    // мапа веб-элементов на текстовое содержимое этих элементов для столбца, по которому ведётся поиск
    private final Map<WebElement, String> columnElementToTextMap = new LinkedHashMap<>();
    // мапа веб-элемента заголовка на текстовое содержимое заголовка
    private final Map<WebElement, String> headerElementToTextMap = new HashMap<>();
    // мапа веб-элемента ячейки на текстовое содержимое ячейки. Заполняется только при обращении в ячейку и используется как кэш
    private final Map<WebElement, String> cellTextMap = new HashMap<>();
    // мапа веб-элементов ячеек на типизированные элементы этих ячеек. Заполняется только при обращении в ячейку и используется как кэш
    private final Map<WebElement, TypifiedElement> typifiedElementMap = new HashMap<>();
    // список из строк таблицы, которые прошли отбор. Каждая строка - мапа содержимого ячейки на её веб-элемент
    private final List<Map<String, WebElement>> resultedHeaderNameToElementMapList = new ArrayList<>();
    // список строк из таблицы которая передаётся в сценарии. Строка состоит из мапы названия столбца к содержимому ячейки этой строки
    private final List<Map<String, String>> userHeaderToRowValueMapList = new ArrayList<>();
    // список веб-элементов всех ячеек кроме заголовков
    private List<WebElement> cellList;
    // список веб-элементов всех ячеек заголовков
    private List<WebElement> headerCellList;
    // количество строк данных в таблице на странице
    private int rows;
    // количество столбцов таблицы на странице
    private int cols;
    // здесь сохраняется последнее значение, которое обрабатывалось перед неудачным завершением поиска строки.
    // Будет показано пользователю как возможное актуальное значение
    private String failureValue;

    public TableActions(final String tableNameOrPath) {
        final Field field;
        if (tableNameOrPath.contains("->")) {
            final BlockExtractor block = new BlockExtractor(tableNameOrPath);
            final String tableName = block.getBlockFieldName();
            field = TableUtils.getFieldByName(block.getBlock().getClass(), tableName);
        } else {
            field = TableUtils.getFieldByName(tableNameOrPath);
        }
        tableElement = FindUtils.getElementByNameOrPath(tableNameOrPath);
        Assert.assertNotNull("Таблица не отображается, либо неверно указан xpath", tableElement);
        headerColXpath = TableUtils.getXpath(field, FindTableHeaderCol.class);
        cellXpath = TableUtils.getXpath(field, FindTableCell.class);
    }

    public void fillTable(final List<List<String>> data, final Boolean validated) {
        resetTable();
        Assert.assertTrue("В таблице не содержится строк для заполнения", rows > 0);
        prepareUserData(data, false);
        for (final Map<String, String> draftRow : userHeaderToRowValueMapList) {
            findRows(draftRow, false, true);
            for (final Map<String, WebElement> rowElements : resultedHeaderNameToElementMapList) {
                final Map<String, String> rowXpaths = new HashMap<>();
                if (!validated) {
                    rowElements.forEach((k, v) -> rowXpaths.put(k, DriverUtils.getElementXPath(v)));
                }
                rowElements.forEach((k, v) -> {
                    try {
                        final WebElement w;
                        if (!validated) {
                            final List<WebElement> elementsOnPageByXpath = DriverUtils.findElementsOnPageByXpath(
                                    rowXpaths.get(
                                            k));
                            Assert.assertEquals("Найдено больше одного элемента", 1, elementsOnPageByXpath.size());
                            w = elementsOnPageByXpath.get(0);
                        } else {
                            w = v;
                        }
                        fillCell(w, draftRow.get(k), validated);
                    } catch (final FieldFillingException e) {
                        throw new AutotestError("Не удалось заполнить значение в столбце " + k, e);
                    }
                });
            }
        }
    }

    public int getAllRowsNumber() {
        resetTable();
        return rows;
    }

    private int getNumberCell(final String name) {
        final String[] numberColum = name.split("№number:");
        try {
            return Integer.parseInt(numberColum[1]);
        } catch (NumberFormatException e) {
            throw new AutotestError(String.format(
                    "Передано не числовое значение \"%s\"",
                    numberColum[1]
            ));
        }
    }

    /**
     * Метод выполняет клик по заголовку таблицы. Если в заголовке чекбокс - он будет отмечен или снят
     *
     * @param columnName имя заголовка
     */


    public void clickOnColumnHeader(final String columnName) {
        resetTable();
        final String colum = columnName.split("№number:")[0];
        int number = 1;
        if (columnName.contains("№number:")) {
            number = getNumberCell(columnName);
        }
        int count = 1;
        for (final WebElement element : headerElementToTextMap.keySet()) {
            if (Validator.matchValues(headerElementToTextMap.get(element), TableUtils.prepareColumnName(colum))) {
                if (number != count) {
                    count++;
                } else {
                    specifyElement(element);
                    if (typifiedElementMap.get(element) instanceof CheckBox) {
                        final boolean currentState = ((CheckBox) typifiedElementMap.get(element)).getFieldState();
                        try {
                            ((Fillable) typifiedElementMap.get(element)).fillField(String.valueOf(!currentState), true);
                        } catch (final FieldFillingException e) {
                            throw new AutotestError("Не удалось заполнить чекбокс в заголовке столбца", e);
                        }
                    } else {
                        final List<WebElement> target = element.findElements(By.xpath(".//*[string-length(text()) > 0]"));
                        ClickActions.safeClick(colum, target.isEmpty() ? element : target.get(0));
                    }
                    return;
                }
            }
        }
        throw new AutotestError("В таблице не найдена колонка " + colum);
    }

    /**
     * Метод проверяет сортировку данных в столбце таблицы
     *
     * @param sortDirection направление сортировки (по возрастанию или по убыванию)
     * @param columnName    название колонки в которой проверяется сортировка
     * @param fieldFormat   формат ячейки (строка, число, дата)
     */
    public void checkTableSort(
            final String sortDirection,
            final String columnName,
            final String fieldFormat
    ) {
        resetTable();
        mappingColumnElements(TableUtils.prepareColumnName(columnName));
        Validator.checkSortData(sortDirection, new ArrayList<>(columnElementToTextMap.values()), fieldFormat);
    }

    public void verifyTable(final List<List<String>> data) {
        resetTable();
        Assert.assertTrue("В таблице не содержится строк для проверки", rows > 0);
        prepareUserData(data, false);
        for (final Map<String, String> draftRow : userHeaderToRowValueMapList) {
            findRows(draftRow, true, true);
        }
    }

    public void fillEmptyTable(final List<List<String>> data) {
        resetTable();
        prepareUserData(data, false);
        for (final Map<String, String> draftRow : userHeaderToRowValueMapList) {
            resetTable();
            for (int col = 0; col < cols; col++) {
                final int cellNumber = userHeaderToRowValueMapList.indexOf(draftRow) * cols + col;
                try {
                    fillCell(
                            cellList.get(cellNumber),
                            draftRow.get(headerElementToTextMap.get(headerCellList.get(col))),
                            true
                    );
                } catch (final FieldFillingException e) {
                    throw new AutotestError("Не удалось заполнить ячейку в таблице", e);
                }
            }
        }
    }

    public void clickOnTableElement(
            final List<List<String>> data,
            final String draftColumnName,
            final boolean isChangeControl
    ) {

        AtomicReference<WebElement> cellElement = new AtomicReference<>();
        BooleanSupplier waitInStaleElementCell = () -> {
            try {
                cellElement.set(findCellElement(data, draftColumnName));
                return true;
            } catch (StaleElementReferenceException e) {
                return false;
            }
        };
        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                String.format("Не найдена ссылка на объект \"%s\"", draftColumnName),
                waitInStaleElementCell
        );
        final String textColumnName = "Элемент таблицы в колонке " + draftColumnName;
        if (isChangeControl) {
            ClickActions.clickWithChangeControl(textColumnName, cellElement.get());
        } else {
            ClickActions.safeClick(textColumnName, cellElement.get());
        }
    }

    public void moveOnTableElement(final List<List<String>> data, final String draftColumnName) {
        final WebElement cellElement = findCellElement(data, draftColumnName);
        Mover.moveToElement(cellElement);
    }

    public WebElement findCellElement(final List<List<String>> data, final String draftColumnName) {
        final WebElement cellElementDraft = findTableElement(data, draftColumnName);
        Mover.moveToElement(cellElementDraft);
//        Попытаемся найти кнопку для нажатия
        final List<WebElement> buttonList = cellElementDraft.findElements(By.xpath(".//button | .//*[name() = 'svg']"));
        final WebElement cellElement;
        if (!buttonList.isEmpty()) {
            cellElement = buttonList.get(0);
        } else {
            final List<WebElement> textElements = cellElementDraft.findElements(By.xpath(
                    ".//*[string-length(text()) > 0]"));
            if (!textElements.isEmpty()) {
                cellElement = textElements.get(0);
            } else {
                cellElement = cellElementDraft;
            }
        }
        return cellElement;
    }

    public WebElement findTableElement(final List<List<String>> data, final String draftColumnName) {
        final String[] numberColum = draftColumnName.split("№number:");
        final boolean isNumberColumnName = draftColumnName.contains("№number:");
        int number;
        if (draftColumnName.contains("№number:")) {
            number = getNumberCell(draftColumnName);
        }
        resetTable();
        Assert.assertTrue("В таблице не содержится строк для взаимодействия", rows > 0);
        prepareUserData(data, isNumberColumnName);
        final String tmpColName = DataProcessing.decodeValue(numberColum[0]).trim().toLowerCase();
        final String decodedColumnName = tmpColName.isEmpty() ? "empty" : tmpColName;
        final List<String> columnNameList = headerElementToTextMap
                .values()
                .stream()
                .filter(v -> Validator.matchValues(v, decodedColumnName))
                .collect(Collectors.toList());
        if (!isNumberColumnName) {
            Assert.assertFalse(String.format(
                    "По заданному условию \"%s\" найдено несколько колонок: %s",
                    decodedColumnName,
                    String.join(", ", columnNameList)
            ), columnNameList.size() > 1);

            Assert.assertFalse(String.format(
                    "По заданному условию \"%s\" не найдено ни одной колонки",
                    decodedColumnName
            ), columnNameList.isEmpty());
        }
        findRows(userHeaderToRowValueMapList.get(0), true, true);
        return resultedHeaderNameToElementMapList
                .get(0)
                .get(TableUtils.prepareColumnName(isNumberColumnName ? draftColumnName.replace(
                        "№number:1",
                        ""
                ) : columnNameList.get(0)));
    }

    private void prepareUserData(final List<List<String>> data, final boolean isNumber) {
        final List<String> columns = new ArrayList<>();
        data.get(0).forEach(v -> {
            final String preparedColumnName = TableUtils.prepareColumnName(DataProcessing.decodeValue(v));

            final List<String> headerList = new ArrayList<>();
            if (preparedColumnName.contains("№number:")) {
                headerList.add(preparedColumnName);
            } else {
                headerList.addAll(headerElementToTextMap
                        .values()
                        .stream()
                        .filter(v1 -> Validator.matchValues(v1, preparedColumnName))
                        .collect(Collectors.toList()));
            }
            if (!isNumber) {
                Assert.assertFalse(
                        String.format("Колонка \"%s\" не найдена", v),
                        headerList.isEmpty()
                );
                Assert.assertEquals(String.format(
                        "Обнаружено несколько совпадений колонок \"%s\": %s укажите маску №number: для выбора номера колонки",
                        v,
                        String.join(", ", headerList)
                ), headerList.size(), 1);
            }
            columns.add(headerList.get(0));

        });
        for (int row = 1; row < data.size(); row++) {
            final Map<String, String> map = new LinkedHashMap<>();
            for (int col = 0; col < data.get(0).size(); col++) {
                map.put(columns.get(col), DataProcessing.decodeValue(data.get(row).get(col)));
            }
            userHeaderToRowValueMapList.add(map);
        }
    }

    private void fillCell(final WebElement element, final String value, final Boolean validated)
            throws FieldFillingException {
        if (DriverUtils.empty(value)) {
            return;
        }
        specifyElement(element);
        if (typifiedElementMap.get(element) instanceof TextBlock) {
            return;
        }
        FieldUtils.fillField(typifiedElementMap.get(element), value, validated);
    }


    private void resetTable() {
        final BooleanSupplier waitWhenTableBeInit = () -> {
            //        Считываю все элементы страницы по предоставленным xpath
            cellList = tableElement.findElements(By.xpath(cellXpath));
            headerCellList = tableElement.findElements(By.xpath(headerColXpath));
            Assert.assertFalse("Таблица не содержит заголовков", headerCellList.isEmpty());

//        Подсчитываю количество столбцов и строк
            cols = headerCellList.size();
            rows = cellList.size() / headerCellList.size();
            return rows > 0;
        };
        Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenTableBeInit);
        Assert.assertEquals(
                "Количество ячеек в строке не соответствует количеству столбцов, " +
                "необходимо проверить xpath таблицы",
                rows * cols, cellList.size()
        );
        cellTextMap.clear();
        typifiedElementMap.clear();
        mappingHeader();
    }

    private void mappingHeader() {
        headerElementToTextMap.clear();
        for (final WebElement element : headerCellList) {
            final String text = getText(element);
            headerElementToTextMap.put(element, TableUtils.prepareColumnName(text.equals(" ") ? text.trim() : text));
        }
    }

    private void mappingColumnElements(final String columnName) {
        final int columnNumber = headerElementToTextMap
                .keySet()
                .stream()
                .filter(element -> headerElementToTextMap.get(element).equals(columnName))
                .findFirst()
                .map(element -> headerCellList.indexOf(element))
                .orElse(-1);
        Assert.assertTrue(String.format("Колонки \"%s\" нет в таблице", columnName), columnNumber != -1);
        IntStream.range(0, rows)
                 .map(row -> row * cols + columnNumber)
                 .forEach(cellNumber -> columnElementToTextMap.put(
                         cellList.get(cellNumber),
                         getText(cellList.get(cellNumber))
                 ));
    }

    private void findRows(final Map<String, String> draftRow, final boolean fullMatching, final boolean assertion) {
        int counter = 0;
        for (final String columnName : draftRow.keySet()) {
            final String decodeColumName = columnName.replace("№number:1", "");
            if (counter == 0) {
                findRows(decodeColumName.split("№number:")[0], draftRow.get(columnName), assertion);
            } else {
                filterSearchedRows(decodeColumName, draftRow.get(columnName), assertion, fullMatching);
            }
            counter++;
            if (!fullMatching && counter > 1) {
                break;
            }
        }
    }

    private void findRows(final String columnName, final String value, final boolean assertion) {
        mappingColumnElements(columnName);
        final Map<WebElement, String> searchedMap = columnElementToTextMap
                .keySet()
                .stream()
                .filter(element -> Validator.matchValues(columnElementToTextMap.get(element), value))
                .collect(Collectors.toMap(element -> element, columnElementToTextMap::get, (a, b) -> b));
        if (searchedMap.isEmpty()) {
            final List<String> failures;
            final String containingValue = value.replaceAll("\\*", "");
            failures = columnElementToTextMap
                    .keySet()
                    .stream()
                    .filter(element -> columnElementToTextMap.get(element).contains(containingValue))
                    .map(columnElementToTextMap::get)
                    .collect(Collectors.toList());
            failureValue = " Похожие значения: " + String.join("; ", failures);
        }
        if (assertion) {
            Assert.assertFalse(
                    String.format(
                            "В таблице не найдены строки соответствующие значению \"%s\" в колонке \"%s\".\"%s\"",
                            value,
                            columnName,
                            failureValue
                    ), searchedMap.isEmpty());
        }

//        Цикл записывает список из мапы строки (текст заголовка таблицы к элементу ячейки в этой строке)
        resultedHeaderNameToElementMapList.clear();
        for (final WebElement element : searchedMap.keySet()) {
            final int row = getRowNumber(element);
            final Map<String, WebElement> headerTextToRowCellMap = new HashMap<>();
            for (int col = 0; col < cols; col++) {
                final int cell = row * cols + col;
                final String nameHeader = headerElementToTextMap.get(headerCellList.get(col));
                if (headerTextToRowCellMap.size() != 0 && headerTextToRowCellMap.get(nameHeader) != null) {
                    if (headerTextToRowCellMap.containsKey(nameHeader + "№number:2")) {
                        final int number = (int) headerTextToRowCellMap
                                .keySet()
                                .stream()
                                .filter(e -> e.startsWith(nameHeader + "№number:"))
                                .count() + 2;
                        headerTextToRowCellMap.put(
                                nameHeader + "№number:" + number,
                                cellList.get(cell)
                        );
                    } else {
                        headerTextToRowCellMap.put(
                                nameHeader + "№number:2",
                                cellList.get(cell)
                        );
                    }
                } else {
                    headerTextToRowCellMap.put(headerElementToTextMap.get(headerCellList.get(col)), cellList.get(cell));
                }
            }
            resultedHeaderNameToElementMapList.add(headerTextToRowCellMap);
        }
    }

    private void filterSearchedRows(
            final String columnName,
            final String expected,
            final boolean assertion,
            final boolean forced
    ) {
        if ("*".equals(expected) || (resultedHeaderNameToElementMapList.size() == 1 && !forced)) {
            return;
        }
        final List<Map<String, WebElement>> tempList = new ArrayList<>(resultedHeaderNameToElementMapList);
        for (final Map<String, WebElement> row : resultedHeaderNameToElementMapList) {
            final WebElement element = row.get(columnName);
            if (DriverUtils.empty(expected)) {
                if (DriverUtils.empty(getText(element))) {
                    continue;
                } else {
                    tempList.remove(row);
                    failureValue = "";
                }
            }
            final String actualValue = getText(element);
            if (!Validator.matchValues(actualValue, expected)) {
                tempList.remove(row);
                failureValue = actualValue;
            }
            if (assertion) {
                Assert.assertFalse(
                        String.format(
                                "Ожидаемое значение \"%s\" в колонке \"%s\" не соответствует фактическому \"%s\". Возможная строка: \"%s\"",
                                expected,
                                columnName,
                                failureValue,
                                getRowText(row)
                        ), tempList.isEmpty());
            }
        }
        resultedHeaderNameToElementMapList.clear();
        resultedHeaderNameToElementMapList.addAll(tempList);
    }

    private String getRowText(final Map<String, WebElement> row) {
        return row.values().stream().map(this::getText).collect(Collectors.joining("; "));
    }

    private int getRowNumber(final WebElement element) {
        return cellList.indexOf(element) / cols;
    }

    private String getText(final WebElement element) {
        if (!cellTextMap.containsKey(element)) {
            specifyElement(element);
            final TypifiedElement typifiedElement = typifiedElementMap.get(element);
            String text;
            if (typifiedElement instanceof CheckBox) {
                text = String.valueOf(((CheckBox) typifiedElement).getFieldState());
            } else {
                text = typifiedElement.getText().trim();
            }
            text = text.isEmpty() ? element.getAttribute("textContent") : text;
            cellTextMap.put(element, text.replaceAll("\\n", " "));
        }
        return cellTextMap.get(element);
    }

    public static void verifyTableRows(
            final String tableName,
            final List<List<String>> data,
            final boolean isRowsExisted
    ) {
        final List<String> header = data.get(0);
        final ErrorCollector errorCollector = new ErrorCollector();
        for (int i = 1; i < data.size(); i++) {
            final List<List<String>> verifiedRow = new ArrayList<>();
            verifiedRow.add(header);
            verifiedRow.add(data.get(i));
            String message = "";
            boolean isValid;
            try {
                new TableActions(tableName)
                        .verifyTable(verifiedRow);
                isValid = isRowsExisted;
            } catch (final AssertionError e) {
                isValid = !isRowsExisted;
                message = e.getMessage();
            }
            if (isRowsExisted) {
                errorCollector.assertTrue(
                        String.format(
                                "Строка не найдена в таблице: %s \n %s",
                                String.join(" | ", data.get(i)),
                                message
                        ),
                        isValid
                );
            } else {
                errorCollector.assertTrue(
                        String.format(
                                "Строка не должна присутствовать в таблице: %s \n %s",
                                String.join(" | ", data.get(i)),
                                message
                        ),
                        isValid
                );
            }
        }
        errorCollector.assertAll();
    }


    private void specifyElement(final WebElement element) {
        if (typifiedElementMap.containsKey(element)) {
            return;
        }
        final Class<? extends TypifiedElement> elementType = FieldUtils.detectFieldType(element);
        final TypifiedElement typifiedElement = HtmlElementUtils.createElementWithCustomType(elementType, element);
        typifiedElementMap.put(element, typifiedElement);
    }

}
