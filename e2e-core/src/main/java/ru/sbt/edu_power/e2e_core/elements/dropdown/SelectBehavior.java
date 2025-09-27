package ru.sbt.edu_power.e2e_core.elements.dropdown;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SelectBehavior {
    private final Select select;

    SelectBehavior(final Select select) {
        this.select = select;
    }

    String getTextBehavior() {
        final String currentValue;
        switch (select.getSelectType()) {
            case TEACHER_FILTER:
            case SELECT_S21:
            case SELECT:
            case SELECT_MFE:
            case SELECT_V5:
            case SELECT_V5_STATISTIC:
            case SELECT_MFE_BA:
            case SELECT_MFE_COMBOBOX:
            case SELECT_V3:
            case COMBOBOX_V1:
                currentValue = select.getValueElement().getText().replaceAll("\n", ",");
                break;
            case DISABLED_SELECT:
            case REPORTS_SELECT:
            case COMBOBOX_V2:
            case S21_COMBOBOX:
            case AWARD_COMBO_BOX:
            case SELECT_RANGE:
                currentValue = select.getValueElement().getAttribute("value");
                break;
            case MULTI_COMBOBOX_V2:
            case COMBOBOX_MULTIPLE:
                final List<WebElement> tags = select.getTags();
                if (tags.isEmpty()) {
                    currentValue = select.getValueElement().getAttribute("value");
                } else {
                    currentValue = tags
                            .stream()
                            .map(WebElement::getText)
                            .collect(Collectors.joining(", "));
                }
                break;
            case TAG_SELECT:
            case S21_TAGS_DROPDOWN:
            case FILTER_TAG_SELECT:
            case COLORED_TAG_SELECT:
            case MULTI_SELECT_V2:
                currentValue = select.getTags()
                                     .stream()
                                     .map(WebElement::getText)
                                     .collect(Collectors.joining(", "));
                break;
            case SELECT_FILTER_MFE:
                List<WebElement> listElement = select.getTags();
                currentValue = listElement.isEmpty() ? "" : listElement.stream()
                                                                       .map(WebElement::getText)
                                                                       .collect(Collectors.joining(", "));
                break;
            default:
                throw new AutotestError(String.format(
                        "Для дропдауна типа \"%s\" получение значения не реализовано",
                        select.getSelectType().name()
                ));
        }
        return currentValue;
    }

    void fillBehavior(final Boolean validated) throws FieldFillingException {
        final Dropdown dropdown;
        switch (select.getSelectType()) {
            case SELECT_MFE:
            case SELECT_V5:
            case SELECT_MFE_BA:
                Mover.scrollGridToElement(select);
            case TEACHER_FILTER:
            case SELECT_V3:
            case SELECT_S21:
            case SELECT:
            case SELECT_V5_STATISTIC:
                dropdown = select.getDropdown("", validated);
                ClickActions.safeClick(
                        select.getValues().get(0),
                        dropdown.getItemByName(select.getValues().get(0), true)
                );
                break;
            case SELECT_RANGE:
                fillRange(select.getValues().get(0));
                break;
            case REPORTS_SELECT:
                fillFieldSelectReport(validated);
                break;
            case AWARD_COMBO_BOX:
            case COMBOBOX_V2:
            case S21_COMBOBOX:
            case COMBOBOX_V1:
                dropdown = select.getDropdown(select.getValues().get(0), validated);
                ClickActions.safeClick(
                        select.getValues().get(0),
                        dropdown.getItemByName(select.getValues().get(0), true)
                );
                break;
            case SELECT_FILTER_MFE:
                selectFilterMfe(validated);
                break;
            case TAG_SELECT:
            case S21_TAGS_DROPDOWN:
                select.clearSelect();
                try {
                    tagSelect(validated);
                } catch (final AutotestError e) {
                    throw new FieldFillingException(e);
                }
                break;
            case MULTI_SELECT_V2:
            case MULTI_COMBOBOX_V2:
            case COLORED_TAG_SELECT:
            case SELECT_MFE_COMBOBOX:
                select.clearSelect();
                if (select.getValues().toString().equals("[]")) {
                    break;
                }
            case COMBOBOX_MULTIPLE:
            case FILTER_TAG_SELECT:
                try {
                    filterTagSelect(validated);
                } catch (final AutotestError e) {
                    throw new FieldFillingException(e);
                }
                break;
            default:
                throw new FieldFillingException(String.format(
                        "Для дропдауна типа \"%s\" заполнение не реализовано",
                        select.getSelectType().name()
                ));
        }
    }

    private void selectFilterMfe(final boolean validated) throws FieldFillingException {
        final Dropdown dropdown;
        BooleanSupplier clearTag = () -> {
            if (!"".equals(getTextBehavior())) {
                ClickActions.safeClick("Кнопка удаления тега", select.findElement(By.xpath(select.getSelectType().getRemoveTagXpath())));
                return false;
            }
            return true;
        };
        Timer.executeTimerThrowable(DriverConstants.TIMEOUT, "Не удалось очистить теги", clearTag);
        dropdown = select.getDropdown("", validated);
        ClickActions.safeClick(
                select.getValues().get(0),
                dropdown.getItemByName(select.getValues().get(0), true)
        );
    }

    private void filterTagSelect(final Boolean validated) {
        final AtomicReference<WebElement> itemElement = new AtomicReference<>();
        final AtomicReference<Dropdown> dropdown = new AtomicReference<>();
        select.getValues().forEach(v -> {
            try {
                dropdown.set(select.getDropdown(v, validated));
            } catch (final FieldFillingException e) {
                throw new AutotestError(e);
            }
            final BooleanSupplier waitWhenItemBeSelected = () -> {
                try {
                    itemElement.set(dropdown.get().getItemByName(v, true));
                    ClickActions.safeClick(itemElement.get().getText(), itemElement.get());
                    return true;
                } catch (final StaleElementReferenceException e) {
                    return false;
                }
            };
            Timer.executeTimerThrowable(
                    DriverConstants.ELEMENT_WAIT_5SEC,
                    "Не удалось выбрать элемент: " + v,
                    waitWhenItemBeSelected
            );
        });
        closeSelectBehavior();
    }

    /**
     * Заполнение селекта с диапазоном, метод автоматически производит клик на диапазон или же на единое значение повторно
     *
     * @param value - диапазон
     * @throws FieldFillingException
     */
    private void fillRange(final String value) throws FieldFillingException {
        final Dropdown dropdown = select.getDropdown("", true);
        final Pattern pattern = Pattern.compile("[1-9]");
        final String xpathItemSelect = select.getSelectType().getDropdownItemXpath();
        final WebDriver webDriver = Environment.getDriverService().getDriver();
        final String[] rangeValue = value.split("-");
        if (pattern.matcher(rangeValue[0]).find()) {
            ClickActions.safeClick(
                    rangeValue[0],
                    webDriver
                            .findElements(By.xpath(xpathItemSelect))
                            .stream()
                            .filter(e -> e.getText().contains(rangeValue[0] + "-"))
                            .findFirst()
                            .orElseThrow(() -> new AutotestError("Не найден первый диапазон"))

            );
            if (pattern.matcher(rangeValue[1]).find()) {
                final String secondRange = rangeValue[1].split(" ")[0];
                ClickActions.safeClick(
                        secondRange,
                        webDriver
                                .findElements(By.xpath(xpathItemSelect))
                                .stream()
                                .filter(e -> e.getText().contains(secondRange))
                                .findFirst()
                                .orElseThrow(() -> new AutotestError("Не второй диапазон"))
                );
            } else {
                ClickActions.safeClick(
                        rangeValue[0],
                        webDriver
                                .findElements(By.xpath(xpathItemSelect))
                                .stream()
                                .filter(e -> e.getText().contains(rangeValue[0] + "-"))
                                .findFirst()
                                .orElseThrow(() -> new AutotestError("Не найден повторно первый диапазон"))
                );
            }
        } else {
            ClickActions.safeClick(
                    value,
                    dropdown.getItemByName(value, true)
            );
        }
    }

    private void tagSelect(final Boolean validated) {
        final AtomicReference<WebElement> itemElement = new AtomicReference<>();
        select.getValues().forEach(v -> {
            final Dropdown d;
            try {
                d = select.getDropdown(v, validated);
            } catch (final FieldFillingException e) {
                throw new AutotestError(e);
            }
            final BooleanSupplier waitWhenItemBeSelected = () -> {
                try {
                    itemElement.set(d.getItemByName(v, false, "Добавить новый тег: *"));
                    if (null == itemElement.get()) {
                        itemElement.set(d.getItemByName("Добавить новый тег: *", true));
                    }
                    ClickActions.safeClick(itemElement.get().getText(), itemElement.get());
                    return true;
                } catch (final StaleElementReferenceException e) {
                    return false;
                }
            };
            Timer.executeTimerThrowable(
                    DriverConstants.ELEMENT_WAIT_5SEC,
                    "Не удалось выбрать элемент: " + v,
                    waitWhenItemBeSelected
            );
        });
    }

    // Метод заполняет дропдаун на форме Отчетов
    private void fillFieldSelectReport(final Boolean validated) throws FieldFillingException {
        Dropdown dropdown = select.getDropdown("", validated);
        final String name = DataProcessing.decodeValue(select.getValues().get(0)).replace(" ", "");
        List<WebElement> itemsList = dropdown.getItemsList();
        final List<WebElement> webElements = itemsList
                .stream()
                .filter(e -> Validator.matchValues(e
                        .getText()
                        .replace(" ", ""), name)
                )
                .collect(
                        Collectors.toList());
        if (webElements.size() > 1) {
            throw new AutotestError(String.format(
                    "В списке более 2х значений \"%s\"",
                    select.getValues().get(0)
            ));
        }
        if (!webElements.isEmpty()) {
            ClickActions.safeClick("Клик по элементу в выпадающем списке", webElements.get(0));
        } else {
            throw new AutotestError(String.format(
                    "Значение \"%s\" не найдено в выпадающем списке",
                    select.getValues().get(0)
            ));
        }
    }


    void clearBehavior() throws FieldFillingException {
        switch (select.getSelectType()) {
            case TAG_SELECT:
            case FILTER_TAG_SELECT:
            case MULTI_SELECT_V2:
            case COLORED_TAG_SELECT:
            case MULTI_COMBOBOX_V2:
            case COMBOBOX_MULTIPLE:
            case S21_TAGS_DROPDOWN:
            case SELECT_MFE_COMBOBOX:
                final List<WebElement> elements = new ArrayList<>();
                final BooleanSupplier waitWhenTagsBeRemoved = () -> {
                    elements.clear();
                    elements.addAll(select.getTags());
                    if (elements.isEmpty()) {
                        return true;
                    }
                    try {
                        ClickActions.safeClick(elements.get(0).getText(), elements.get(0).findElement(
                                By.xpath(select.getSelectType().getRemoveTagXpath())
                        ));
                    } catch (final StaleElementReferenceException ignored) {
                    }
                    return false;
                };
                final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenTagsBeRemoved);
                if (!result) {
                    throw new FieldFillingException("Не удалось удалить все теги");
                }
                break;
            default:
                throw new FieldFillingException(String.format(
                        "Дропдаун типа \"%s\" не поддерживает очистку",
                        select.getSelectType().name()
                ));
        }
    }

    // Вызывается только в воркфлоу заполнения поля
    void closeSelectBehavior() {
        if (DriverUtils.isElementDisplayed(select.getSelectType().getDropdownBoxXpath(), null)) {
            ClickActions.safeClick("Кнопка закрытия дропдауна", select.getCloseButton());
        }
    }

    boolean validateFieldBehavior() {
        final List<String> actual = Arrays
                .stream(select.getText().split(","))
                .map(DataProcessing::decodeValue)
                .map(e -> e.replace("Введите или выберите из списка", "").replace("Выбери из списка",""))
                .collect(Collectors.toList());
        final boolean validateResult = select.getValues()
                                             .stream()
                                             .allMatch(e -> Validator.matchValueInList(actual, e));
        switch (select.getSelectType()) {
            case SELECT_RANGE:
                if (select.getValueElement().getAttribute("value").isEmpty() &&
                    !select.getValues().get(0).contains("-")) {
                    return true;
                }
            case SELECT_MFE_COMBOBOX:
            case FILTER_TAG_SELECT:
                return validateResult;
            case DISABLED_SELECT:
            case REPORTS_SELECT:
            case COMBOBOX_V2:
            case S21_COMBOBOX:
            case AWARD_COMBO_BOX:
            case COMBOBOX_V1:
            case TAG_SELECT:
            case TEACHER_FILTER:
            case SELECT_V5:
            case SELECT_V5_STATISTIC:
            case SELECT_S21:
            case S21_TAGS_DROPDOWN:
            case SELECT:
            case SELECT_MFE:
            case SELECT_MFE_BA:
            case SELECT_V3:
            case MULTI_SELECT_V2:
            case MULTI_COMBOBOX_V2:
            case COMBOBOX_MULTIPLE:
            case COLORED_TAG_SELECT:
                return validateResult &&
                       select.getValues().size() == actual.size();
            default:
                throw new AutotestError("Поведение валидации поля не описано для типа " + select.getSelectType());
        }
    }
}
