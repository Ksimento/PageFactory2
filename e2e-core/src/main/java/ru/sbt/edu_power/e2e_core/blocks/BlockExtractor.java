package ru.sbt.edu_power.e2e_core.blocks;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class BlockExtractor {
    // Путь до блока или элемента блока
    private final String PATH;
    // Составляющие части полного пути до элемента блока
    private final List<String> pathItems;
    // Указатель текущей позиции в пути
    private int pointer;
    // Название конечного элемента блока
    private final String PATH_LAST_ITEM;
    // Хранилище лога поиска блока
    private final List<String> blockSearchHistory = new ArrayList<>();
    // Паттерн построения пути до элемента блока
    private final List<BlockUtils.Pattern> patterns;
    // Текущий контекст для поиска элементов пути
    private Class<? extends BlockInitialized> blockContextClass;
    // Текущая страница
    private final Class<? extends Page> CURRENT_PAGE_CLASS;
    // Хранилище блоков, удовлетворяющих условиям поиска
    private final List<WebElement> currentIterationFilteredBlockList = new ArrayList<>();
    // Элементы блоков в пределах одного цикла поиска
    private final List<WebElement> blockCandidateList = new ArrayList<>();
    // Класс блока в контексте которого ведётся текущий поиск
    private Class<? extends BlockInitialized> currentIterationBlockContextClass;
    // Аргумент поиска блока
    private String argument;
    // Xpath блока в текущей итерации
    private String blockXpath;
    // номер блока при поиске
    private Integer numberBlock;
    private boolean isNumberBlock = false;
    // Xpath до элемента блока
    private String fieldXpath;
    // Массив из аргумента поиска и уточняющей фразы
    private String[] additionalOptions;
    // Значение поля из аргумента для отбора блока
    private String value;
    // Название поля из аргумента для отбора блока
    private String argumentFieldName;
    // Список блоков удовлетворяющих аргументам поиска
    private final List<WebElement> blockList = new ArrayList<>();
    // Найденный блок
    private BlockInitialized block;
    // Вебэлемент, относительно которого можно искать блоки, например виджет
    private WebElement initialWebElement;

    public BlockExtractor(final String path) {
        CURRENT_PAGE_CLASS = PageContext.getCurrentPage().getClass();
        PATH = path;
        patterns = BlockUtils.getPattern(path);
        pathItems = Arrays.asList(path.split("->"));
        PATH_LAST_ITEM = pathItems.get(pathItems.size() - 1);
    }

    public BlockExtractor(final String path, final WebElement element) {
        initialWebElement = element;
        CURRENT_PAGE_CLASS = PageContext.getCurrentPage().getClass();
        PATH = path;
        patterns = BlockUtils.getPattern(path);
        pathItems = Arrays.asList(path.split("->"));
        PATH_LAST_ITEM = pathItems.get(pathItems.size() - 1);
    }

    public BlockUtils.Pattern getLastPattern() {
        return patterns.get(patterns.size() - 1);
    }

    @SuppressWarnings("unchecked")
    public <T extends BlockInitialized> T getBlockWithWait(final int timeoutInSecond) {
        final AtomicReference<Throwable> throwable = new AtomicReference<>();
        final BooleanSupplier waitWhenBlockPresent = () -> {
            try {
                getBlock();
                return true;
            } catch (final BlockExtractorException e) {
                throw new AutotestError(e);
            } catch (final Throwable e) {
                reset();
                throwable.set(e);
            }
            return false;
        };
        final boolean result = Timer.executeTimer(timeoutInSecond, waitWhenBlockPresent);
        if (!result) {
            throw new AutotestError(throwable.get());
        }
        return (T) block;
    }

    @SuppressWarnings("unchecked")
    public <T extends WebElement> T getElementWithWait(final int timeout) {
        final AtomicReference<T> element = new AtomicReference<>();
        final BooleanSupplier waitWhenElementBeDisplayed = () -> {
            try {
                element.set((T) getBlockWithWait(timeout).getElementByName(getBlockElementName()));
                return true;
            } catch (final Throwable e) {
                return false;
            }
        };
        Timer.executeTimerThrowable(timeout, "Элемент не найден " + PATH, waitWhenElementBeDisplayed);
        return element.get();
    }

    private String getBlockElementName() {
        final String[] parts = PATH.split("->", pointer + 1);
        return parts[parts.length - 1];
    }

    @SuppressWarnings("unchecked")
    public <T extends WebElement> List<T> getElementCollection() {
        checkPathMustEndsByFieldName();
        return getInitializedBlockCollection()
                .stream()
                .map(block -> (T) block.getElementByName(getBlockElementName()))
                .collect(Collectors.toList());
    }

    public String getBlockFieldName() {
        return PATH_LAST_ITEM;
    }

    /**
     * Метод возвращает экземпляр поля найденного блока. Название поля получено из пути.
     * Если в пути не окажется названия элемента блока, будет выдано исключение
     */
    @SuppressWarnings("unchecked")
    public <T extends WebElement> T getElement() {
        checkPathMustEndsByFieldName();
        final WebElement element = getBlock().getElementByName(getBlockElementName());
        if (null == element) {
            throw new BlockExtractorException(String.format("Элемент \"%s\" отсутствует", getBlockElementName()));
        }
        return (T) element;
    }

    /**
     * Метод возвращает список всех блоков, удовлетворяющих условию
     */
    public List<WebElement> getBlockCollection() {
        collectBlocks();
        return blockList;
    }

    /**
     * Метод возвращает список всех блоков, удовлетворяющих условию, предварительно проинициализировав их как блоки
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends BlockInitialized> List<T> getInitializedBlockCollection() {
        return getBlockCollection()
                .stream()
                .map(element -> (T) createBlock(element))
                .collect(Collectors.toList());
    }

    public int getBlockNumber() {
        String newPath = PATH;
        String argument = null;
        while (true) {
            final List<BlockUtils.Pattern> patternList = BlockUtils.getPattern(newPath);
            if (patternList.get(patternList.size() - 1) == BlockUtils.Pattern.IDENTIFIED_BLOCK) {
                argument = newPath.substring(newPath.lastIndexOf("->") + 2);
            }
            if (patternList.get(patternList.size() - 1) == BlockUtils.Pattern.BLOCK_LIST) {
                if (null == argument) {
                    throw new BlockExtractorException("Путь до блока должен иметь ");
                }
                break;
            }
            newPath = newPath.substring(0, newPath.lastIndexOf("->"));
        }
        final BlockExtractor block = new BlockExtractor(newPath);
        final BooleanSupplier waitWhenBlockListBePresent = () -> !block.getBlockCollection().isEmpty();
        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                "Страница не содержит списка блоков",
                waitWhenBlockListBePresent
        );
        // Если в аргументе last - возвращаем общее количество блоков
        if ("last".equals(argument)) {
            return block.getBlockCollection().size();
        }
        final String[] argumentOptions = argument.split("#", 2);
        final String fieldName = argumentOptions[0];
        final String options = argumentOptions[1];
        final String clearOption = argumentOptions[1].split("&&")[0];
        final String fieldValue = DataProcessing.decodeValue(clearOption);
        final String additionalOption = options.contains("&&") ? options.split("&&")[1] : "";
        final List<BlockInitialized> blockList = block.getInitializedBlockCollection();
        final Map<Integer, BlockInitialized> filteredBlockMap = new HashMap<>();
        for (int i = 0; i < blockList.size(); i++) {
            final DriverUtils.ValidatedValue result = DriverUtils
                    .validateField(blockList.get(i).getElementByName(fieldName), fieldValue);
            if (result.getValidateResult()) {
                if (!"".equals(additionalOption)) {
                    if (!blockList.get(i).getText().contains(additionalOption)) {
                        continue;
                    }
                }
                filteredBlockMap.put(i, blockList.get(i));
            }
        }
        if (filteredBlockMap.isEmpty()) {
            throw new BlockExtractorException("Не найдено блоков удовлетворяющих условию");
        }
        if (filteredBlockMap.size() > 1) {
            throw new BlockExtractorException("Найдено более одного блока удовлетворяющих условию");
        }
        return filteredBlockMap.keySet().iterator().next() + 1;
    }

    /**
     * Метод возвращает найденный экземпляр блока, при условии что нашёлся только один блок
     * Если блоков несколько - будет выдано исключение
     *
     * @return экземпляр найденного блока
     */
    public <T extends BlockInitialized> T getBlock() {
        collectBlocks();
        if (blockList.isEmpty()) {
            throw new NoSuchElementException("Блок не найден\n" + String.join("\n", blockSearchHistory));
        }
        if (isNumberBlock) {
            block = createBlock(blockList.get(numberBlock));
            return (T) block;
        }
        if (blockList.size() > 1) {
            throw new BlockExtractorException(
                    String.format(
                            "Использованы неоптимальные данные для поиска, найдено \"%s\" блоков\n\"%s\"",
                            blockList.size(),
                            String.join("\n", blockSearchHistory)
                    )

            );
        }
        block = createBlock(blockList.get(0));
        return (T) block;
    }

    /**
     * Метод проверяет сортировку данных в полях списка блоков
     *
     * @param sortDirection направление сортировки (по возрастанию или по убыванию)
     * @param fieldName     название поля блока по которому проверяется сортировка
     * @param fieldFormat   формат ячейки (строка, число, дата)
     */
    public void checkBlockSort(
            final String sortDirection,
            final String fieldName,
            final String fieldFormat
    ) {
        final List<BlockInitialized> blockList = getInitializedBlockCollection();
        final List<String> data = blockList
                .stream()
                .map(block -> block.getElementByName(fieldName))
                .filter(Objects::nonNull)
                .map(WebElement::getText)
                .collect(Collectors.toList());
        Validator.checkSortData(
                sortDirection,
                data,
                fieldFormat
        );
    }

    public BlockExtractor reset() {
        block = null;
        blockList.clear();
        currentIterationBlockContextClass = null;
        currentIterationFilteredBlockList.clear();
        blockSearchHistory.clear();
        pointer = 0;
        return this;
    }

    /**
     * Метод производит отбор подходящих блоков на странице, удовлетворяющих аргументам поиска
     */
    private void collectBlocks() {
        if (isBlocksCollected()) {
            return;
        }
        final Iterator<BlockUtils.Pattern> patternIterator = patterns.iterator();
        final Iterator<String> pathItemsIterator = pathItems.iterator();
        while (patternIterator.hasNext()) {
            // смещаем указатель пути на единицу
            pointer++;
            blockCandidateList.clear();
            final BlockUtils.Pattern pattern = patternIterator.next();
            blockSearchHistory.add("Паттерн: " + pattern.name());

            // Название и класс элемента блока
            final String blockName = pathItemsIterator.next();
            blockSearchHistory.add("Имя блока: " + blockName);

            // Получаем xpath до элемента блока из описания к нему
            blockXpath = getXpath(blockName);
            blockSearchHistory.add("Xpath блока: " + blockXpath);

            // Устанавливаем блок как текущий контекст
            currentIterationBlockContextClass = getBlockClass(blockName);
            blockSearchHistory.add("Класс блока: " + currentIterationBlockContextClass.getName());

            if (pattern == BlockUtils.Pattern.BLOCK_LIST) {
                if (initialWebElement != null) {
                    currentIterationFilteredBlockList.addAll(initialWebElement.findElements(By.xpath(blockXpath)));
                } else if (currentIterationFilteredBlockList.isEmpty()) {
                    currentIterationFilteredBlockList.addAll(DriverUtils.findElementsOnPageByXpath(blockXpath));
                } else {
                    blockCandidateList.addAll(
                            currentIterationFilteredBlockList
                                    .stream()
                                    .flatMap(block -> block.findElements(By.xpath(blockXpath)).stream())
                                    .collect(Collectors.toList())
                    );
                    currentIterationFilteredBlockList.clear();
                    currentIterationFilteredBlockList.addAll(blockCandidateList);
                    blockCandidateList.clear();
                }
                break;
            }

            // Получаем аргументы для определения подходящих блоков
            final String pathItem = pathItemsIterator.next().trim();
            // смещаем указатель текущей позиции в пути блока
            pointer++;
            argument = pathItem.startsWith("stash") ? DataProcessing.decodeValue(pathItem) : pathItem;
            blockSearchHistory.add("Аргумент для поиска блока: " + argument);

            if (argument.contains("#")) {
                filterBlocksByArgument();
            } else {
                filterBlocksByNumber();
            }
        }
        blockList.clear();
        blockList.addAll(currentIterationFilteredBlockList);
    }

    private boolean isBlocksCollected() {
        if (!blockList.isEmpty()) {
            try {
                blockList.get(0).isDisplayed();
                return true;
            } catch (final StaleElementReferenceException ignored) {
                blockList.clear();
                block = null;
                blockSearchHistory.clear();
                currentIterationBlockContextClass = null;
                currentIterationFilteredBlockList.clear();
            }
        }
        return false;
    }

    /**
     * Метод выполняет поиск среди кандидатов на искомый блок, используя сравнение полей блока с ожидаемым значением
     */
    private void filterBlocksByArgument() {
        // Отделяем уточняющий аргумент через &&
        additionalOptions = argument.split("&&");
        // Выделяем поле из аргумента
        argumentFieldName = additionalOptions[0].split("#", 2)[0];
        // Выделяем значение поля и пытаемся его декодировать
        final String valueDraft = additionalOptions[0].split("#", 2)[1];
        value = DataProcessing.decodeValue(valueDraft);
        paramNumber();
        // Очищенное от звёздочек значение аргумента поиска
        final String containingValue = value.replaceAll("\\*", "");
        // Заранее ограничиваем список всех элементов только теми, в которых может присутствовать нужный текст
        final String blockXpathFastSearch = blockXpath
                .concat("/self::*[descendant-or-self::*[contains(node(), '")
                .concat(containingValue).concat("')]]");
        blockSearchHistory.add("Xpath для ограничения поиска блока: " + blockXpathFastSearch);
        List<WebElement> searchedElements;
        if (initialWebElement != null) {
            searchedElements = initialWebElement
                    .findElements(By.xpath(blockXpathFastSearch));
            if (searchedElements.isEmpty()) {
                searchedElements = initialWebElement
                        .findElements(By.xpath(blockXpath));
            }
            blockCandidateList.addAll(searchedElements);
        } else if (currentIterationFilteredBlockList.isEmpty()) {
            searchedElements = Environment
                    .getDriverService()
                    .getDriver()
                    .findElements(By.xpath(blockXpathFastSearch));
            if (searchedElements.isEmpty()) {
                searchedElements = Environment
                        .getDriverService()
                        .getDriver()
                        .findElements(By.xpath(blockXpath));
            }
            blockSearchHistory.add("Найдено блоков: " + searchedElements.size());
            blockCandidateList.addAll(searchedElements);
        } else {
            for (final WebElement block : currentIterationFilteredBlockList) {
                searchedElements = block.findElements(By.xpath(blockXpathFastSearch));
                if (searchedElements.isEmpty()) {
                    searchedElements = block.findElements(By.xpath(blockXpath));
                }
                blockSearchHistory.add("Найдено блоков: " + searchedElements.size());
                blockCandidateList.addAll(searchedElements);
            }
        }
        // Получаем xpath до элемента поля
        fieldXpath = getXpath(argumentFieldName);
        blockSearchHistory.add("Xpath элемента блока: " + fieldXpath);
        // Находим все блоки, текст в поле которых равно искомому значению
        currentIterationFilteredBlockList.clear();
        currentIterationFilteredBlockList
                .addAll(blockCandidateList
                        .stream()
                        .filter(this::filterBlocksByFieldValue)
                        .collect(Collectors.toList())
                );
    }

    /**
     * Фильтр для найденного элемента. Выполняет его инициализацию и валидацию в соответствии с аргументами пути
     *
     * @param element веб-элемент валидируемого поля блока
     * @return true если элемент соответствует аргументам поиска
     */
    private boolean filterBlocksByFieldValue(final WebElement element) {
        final List<WebElement> elements = element.findElements(By.xpath(fieldXpath));
        if (elements.isEmpty()) {
            return false;
        }
        if (elements.size() > 1) {
            if (isNumberBlock) {
            } else {
                throw new AutotestError(
                        "Количество найденных элементов в блоке больше одного. " +
                        "Возможно, в xpath элемента блока не добавлена точка в начале"
                );
            }
        }
        blockSearchHistory.add("Найден экземпляр подходящего блока по xpath: " +
                               elements.get(0).getText());
        final boolean isTargetBlock;
        final WebElement typifiedField = createElement(argumentFieldName, elements.get(0));
        if (typifiedField instanceof Validatable) {
            isTargetBlock = DriverUtils.validateField(typifiedField, value).getValidateResult();
        } else {
            final String actualText = typifiedField
                    .getText()
                    .trim()
                    .replaceAll("\n", " ")
                    .replaceAll("\u00AD", "");
            final String actualFieldText = actualText.isEmpty() ? typifiedField.getAttribute("textContent") : actualText;
            isTargetBlock = Validator.matchValues(actualFieldText, value, true);
        }
        final boolean isAcceptAdditionalOptions = additionalOptions.length <= 1 ||
                                                  element
                                                          .getText()
                                                          .replaceAll("\u00AD", "")
                                                          .toLowerCase()
                                                          .contains(additionalOptions[1].toLowerCase());

        if ((isTargetBlock && isAcceptAdditionalOptions) ||isNumberBlock) {
            blockSearchHistory.add("Блок удовлетворяет условиям поиска: " + elements.get(0).getText());
            return true;
        }
        return false;
    }

    private void paramNumber() {
        Arrays.stream(additionalOptions).forEach(e -> {
            if (e.startsWith("number#")) {
                final String number = e.replace("number#", "");
                numberBlock = Integer.parseInt(number)-1;
                isNumberBlock = true;
            }
        });
    }

    /**
     * Находит все имеющиеся блоки списка и возвращает один из них по номеру из аргументов пути
     */
    private void filterBlocksByNumber() {
        if (initialWebElement != null) {
            blockCandidateList.addAll(initialWebElement.findElements(By.xpath(blockXpath)));
        }
        if (currentIterationFilteredBlockList.isEmpty()) {
            blockCandidateList.addAll(DriverUtils.findElementsOnPageByXpath(blockXpath));
        } else {
            for (final WebElement block : currentIterationFilteredBlockList) {
                blockCandidateList.addAll(block.findElements(By.xpath(blockXpath)));
            }
        }
        final int blockNumber;
        if ("last".equals(argument)) {
            blockNumber = blockCandidateList.size() - 1;
        } else {
            blockNumber = Integer.parseInt(argument) - 1;
        }
        currentIterationFilteredBlockList.clear();
        Assert.assertTrue(
                String.format("Блока с номером %d на странице нет", blockNumber),
                blockCandidateList.size() > blockNumber
        );
        currentIterationFilteredBlockList.add(blockCandidateList.get(blockNumber));
    }

    /**
     * Метод проверяет что путь заканчивается названием поля, иначе выдаёт исключение
     */
    private void checkPathMustEndsByFieldName() {
        if (patterns.get(patterns.size() - 1) != BlockUtils.Pattern.ELEMENT_IN_BLOCK &&
            patterns.get(patterns.size() - 1) != BlockUtils.Pattern.WIDGET_IN_BLOCK) {
            throw new BlockExtractorException(String.format(
                    "Путь блока \"%s\" должен заканчиваться названием элемента. \"%s\" не является названием элемента блока",
                    PATH,
                    PATH_LAST_ITEM
            ));
        }
    }

    /**
     * Получает xpath из аннотации @FindBy по имени блока
     *
     * @param blockName название элемента в описании страницы или блока
     * @return строка Xpath
     */
    private String getXpath(final String blockName) {
        if (currentIterationBlockContextClass == null) {
            return DriverUtils.getXpath(blockName);
        } else {
            for (final Field field : currentIterationBlockContextClass.getFields()) {
                if (field.isAnnotationPresent(ElementTitle.class)
                    && field.getAnnotation(ElementTitle.class).value().equals(blockName)) {
                    return field.getAnnotation(FindBy.class).xpath();
                }
            }
        }
        throw new NoElementFoundInBlockContext(String.format(
                "Не удалось найти элемент \"%s\" в контексте \"%s\"",
                blockName,
                currentIterationBlockContextClass.getName()
        ));
    }

    /**
     * Метод получает класс списочного элемента
     *
     * @param blockName название элемента в описании страницы или блока
     * @return класс блока
     */
    private Class<? extends BlockInitialized> getBlockClass(final String blockName) {
        final Field field;
        if (currentIterationBlockContextClass == null) {
            field = BlockUtils.getFieldByNameFromPage(blockName, CURRENT_PAGE_CLASS);
        } else {
            field = BlockUtils.getFieldByNameFromBlock(blockName, currentIterationBlockContextClass);
        }
        final String className = field.getGenericType().getTypeName();
        if (!className.contains("List")) {
            throw new BlockExtractorException(String.format(
                    "Списочный элемент '%s' должен иметь класс List<>",
                    blockName
            ));
        }
        final String blockClassName = className.substring(className.indexOf("<") + 1, className.indexOf(">"));
        return BlockUtils.getClass(blockClassName);
    }

    /**
     * Метод создаёт типизированный элемент, который является полем блока
     *
     * @param fieldName      название поля блока
     * @param wrappedElement веб-элемент, который требуется проинициализировать как поле блока
     * @return экземпляр созданного блока
     */
    @SuppressWarnings("unchecked")
    private <T extends WebElement> T createElement(final String fieldName, final WebElement wrappedElement) {
        T element;
        final Field field = BlockUtils.getFieldByNameFromBlock(fieldName, currentIterationBlockContextClass);
        try {
            element = (T) field.getType().getConstructor(WebElement.class).newInstance(wrappedElement);
        } catch (final StaleElementReferenceException s) {
            element = null;
        } catch (final Throwable e) {
            throw new AutotestError("Не удалось создать элемент", e);
        }
        return element;
    }

    /**
     * Метод создаёт экземпляр блока
     *
     * @param wrappedElement веб-элемент, который должен быть проинициализирован как блок
     * @return экземпляр созданного блока
     */
    private BlockInitialized createBlock(final WebElement wrappedElement) {
        try {
            return currentIterationBlockContextClass.getConstructor(WebElement.class).newInstance(wrappedElement);
        } catch (final InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new BlockExtractorException("Не удалось создать блок", e);
        } catch (final IllegalAccessException e) {
            throw new BlockExtractorException(String.format(
                    "Блок \"%s\" должен иметь public конструктор",
                    currentIterationBlockContextClass
                            .getName()
            ), e);
        }
    }
}
