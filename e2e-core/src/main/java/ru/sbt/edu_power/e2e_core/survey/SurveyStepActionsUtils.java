package ru.sbt.edu_power.e2e_core.survey;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.elements.survey.ConfiguratorCheckBox;
import ru.sbt.edu_power.e2e_core.survey.blocks.SurveyWidgetConfigAccordionTabsListItem;
import ru.sbt.edu_power.e2e_core.survey.enums.LoadControlMethod;
import ru.sbt.edu_power.e2e_core.survey.enums.SurveyAnswerFieldList;
import ru.sbt.edu_power.e2e_core.survey.enums.SurveyAnswerFieldRenderVariant;
import ru.sbt.edu_power.e2e_core.survey.enums.SurveyDataTypes;
import ru.sbt.edu_power.e2e_core.survey.enums.SurveyWidgetTypes;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.e2e_core.blocks.BlockInitialized;
import ru.sbt.edu_power.e2e_core.elements.FieldFillingException;
import ru.sbt.edu_power.e2e_core.elements.formatting_text_input.FormattingTextInput;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractor;
import ru.sbt.edu_power.e2e_core.blocks.PathBuilder;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

@Slf4j
public class SurveyStepActionsUtils {
    private final String VARIANTS_FIELD_BLOCK_NAME = "Список ответов";
    private final String CORRECT_ANSWER_FIELD_NAME = "Значение правильного ответа";
    private final String CONFIG_TAB_LIST = "Список табов";

    public static <T extends BlockInitialized> T getSurveyQuestionBlock(final String blockName, final String question) {
        final PathBuilder path = new PathBuilder();
        path.setBlock(blockName);
        final Integer blockNumber = DriverUtils.getNumber(question);
        if (blockNumber == null) {
            path.setBlockArgument("вопрос", question);
        } else {
            path.setBlockNumber(blockNumber);
        }
        return new BlockExtractor(path.build()).getBlockWithWait(DriverConstants.ELEMENT_WAIT_5SEC);
    }

    public static <T> T createTypifiedSurveyInputElement(final String fieldName, @Nonnull final WebElement element) {
        for (final SurveyWidgetTypes type : SurveyWidgetTypes.values()) {
            final List<WebElement> elements = element.findElements(By.xpath(type.getDetectWidgetXpath()));
            if (!elements.isEmpty()) {
                try {
                    final Class<?> clazz = Class.forName("ru.sbt.edu_power.e2e_core.elements.survey." +
                                                         type.name());
                    final Object obj = clazz.getConstructor(WebElement.class).newInstance((WebElement) element);
                    return (T) obj;
                } catch (final Throwable e) {
                    throw new AutotestError("Ошибка при создании объекта поля", e);
                }
            }
        }
        throw new AutotestError("Не удалось определить тип поля " + fieldName);
    }

    /**
     * Метод нажимает на кнопку удаления всех полей из списка значений в модальном окне
     */
    void deleteAllFields(final SurveyDataTypes type) {
        final SurveyWidgetConfigAccordionTabsListItem block = getBlockByDataType(type);
        SurveyActions.clickWithChangeControl(
                "Удалить все",
                block.removeAllButton,
                LoadControlMethod.BY_BLOCK,
                getPathByDataType(type) + "->Список ответов"
        );
    }

    /**
     * Метод открывает таб со списком значений (ответы, строки, столбцы и т.д.)
     *
     * @param type тип требуемого списка
     */
    void openFieldsTab(final SurveyDataTypes type) {
        final SurveyWidgetConfigAccordionTabsListItem block = getBlockByDataType(type);
        if (block.tabContentTextBlock == null) {
            SurveyActions.clickWithChangeControl(
                    type.getTabName(),
                    block.tabNameTextBlock,
                    LoadControlMethod.BY_CONTAINER_NAME,
                    "Боковая панель настроек"
            );
        }
    }

    void closeFieldTab(final SurveyDataTypes type) {
        final SurveyWidgetConfigAccordionTabsListItem block = getBlockByDataType(type);
        if (block.tabContentTextBlock != null) {
            SurveyActions.clickWithChangeControl(
                    type.getTabName(),
                    block.tabNameTextBlock,
                    LoadControlMethod.BY_CONTAINER_NAME,
                    "Боковая панель настроек"
            );
        }
    }

    /**
     * Метод добавляет набор полей в список ответов (строк, столбцов) в модальном окне
     */
    void addFieldsBlock(final SurveyDataTypes type) {
        final SurveyWidgetConfigAccordionTabsListItem block = getBlockByDataType(type);


        SurveyActions.clickWithChangeControl(
                "Добавить новое значение",
                block.addItemButton,
                LoadControlMethod.BY_BLOCK,
                getPathByDataType(type) + "->Список ответов"
        );
    }

    private SurveyWidgetConfigAccordionTabsListItem getBlockByDataType(final SurveyDataTypes type) {
        return new BlockExtractor(getPathByDataType(type)).getBlock();
    }

    private String getPathByDataType(final SurveyDataTypes type) {
        return new PathBuilder()
                .setBlock(CONFIG_TAB_LIST)
                .setBlockArgument("Название", type.getTabName())
                .build();
    }

    /**
     * Метод заполняет поле вопроса активного виджета
     *
     * @param question вопрос
     */
    void fillQuestion(final String question) {
        final FormattingTextInput input = DriverUtils.getElementByTitle(SurveyDataTypes.QUESTION.getFieldName());
        try {
            input.fillField(question, true);
        } catch (final FieldFillingException e) {
            throw new AutotestError("Не удалось заполнить поле вопроса", e);
        }
    }

    void fillMultiSelect(final String state) {
        final ConfiguratorCheckBox input = DriverUtils.getElementByTitle(SurveyDataTypes.MULTI_SELECT.getFieldName());
        try {
            input.fillField(state, true);
        } catch (final FieldFillingException e) {
            throw new AutotestError("Не удалось заполнить чекбокс Мультиселект", e);
        }
    }

    void fillAnswer(final SurveyDataTypes type, final Map<SurveyAnswerFieldList, String> data) {
        data.forEach((field, value) -> fillAnswerItem(type, field, value, false));
    }

    /**
     * Метод проверяет поле вопроса активного виджета
     *
     * @param question вопрос
     */
    void verifyQuestion(final String question) {
        final FormattingTextInput input = DriverUtils.getElementByTitle(SurveyDataTypes.QUESTION.getFieldName());
        throwExceptionByUncheckedField(input, question);
    }

    void verifyMultiSelect(final String state) {
        final ConfiguratorCheckBox input = DriverUtils.getElementByTitle(SurveyDataTypes.MULTI_SELECT.getFieldName());
        throwExceptionByUncheckedField(input, state);
    }

    void verifyAnswers(final SurveyDataTypes type, final List<Map<SurveyAnswerFieldList, String>> data) {
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < data.size(); i++) {
            counter.set(i);
            data.get(counter.get()).forEach((k, v) -> {
                final String path = getPathToField(type, k, String.valueOf(counter.get() + 1));
                final Validatable field = new BlockExtractor(path).getElement();
                throwExceptionByUncheckedField(field, v);
            });
        }
    }

    void verifyCorrectAnswer(final String value) {
        openFieldsTab(SurveyDataTypes.CORRECT_ANSWER);
        final WebElement input = DriverUtils.getElementByTitle(CORRECT_ANSWER_FIELD_NAME);
        final Validatable surveyInput = createTypifiedSurveyInputElement(CORRECT_ANSWER_FIELD_NAME, input);
        throwExceptionByUncheckedField(surveyInput, value);
    }

    private void throwExceptionByUncheckedField(final Validatable element, final String expected) {
        if (element.validate(expected)) {
            return;
        }
        final String message = String.format(
                "Ожидаемое значение \"%s\" не соответствует фактическому \"%s\"",
                expected,
                element.getFieldValue()
        );
        throw new AutotestError(message);
    }

    /**
     * Некоторые поля для заполнения в конструкторе Survey не стабильные и при попытке заполнить перерисовываются
     * Что вызывает исключение StaleElementReferenceException при попытке повторить заполнение
     * Поэтому тут выполнено заполнение в рекурсии вместе с повторным поиском поля для заполнения
     *
     * @param field         поле
     * @param value         значение
     * @param stopRecursion на вход передавать false, в рекурсивном вызове здесь будет true для остановки
     */
    private void fillAnswerItem(
            final SurveyDataTypes type,
            final SurveyAnswerFieldList field,
            final String value,
            final boolean stopRecursion
    ) {
        try {
            final String path = getPathToField(type, field, "last");
            final Fillable input = new BlockExtractor(path).getElement();
            input.fillField(value, true);
        } catch (final StaleElementReferenceException e) {
            if (stopRecursion) {
                throw new AutotestError(String.format(
                        "Не удалось заполнить поле \"%s\" значением \"%s\"",
                        field.getFieldName(),
                        value
                ), e);
            }
            fillAnswerItem(type, field, value, true);
        } catch (final FieldFillingException e) {
            throw new AutotestError("Не удалось заполнить поле ответа", e);
        }
    }

    void fillCorrectAnswer(final String value) {
        final BooleanSupplier waitWhenFieldsBeFilled = () -> {
            try {
                openFieldsTab(SurveyDataTypes.CORRECT_ANSWER);
                final WebElement input = DriverUtils.getElementByTitle(CORRECT_ANSWER_FIELD_NAME);
                final Fillable surveyInput = createTypifiedSurveyInputElement(CORRECT_ANSWER_FIELD_NAME, input);
                surveyInput.fillField(value, true);
                closeFieldTab(SurveyDataTypes.CORRECT_ANSWER);
                return true;
            } catch (final FieldFillingException e) {
                throw new AutotestError("Не удалось заполнить поле правильного ответа", e);
            } catch (final Throwable e) {
                return false;
            }
        };

        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                "Не удалось заполнить правильный ответ",
                waitWhenFieldsBeFilled
        );
    }

    /**
     * Метод возвращает путь до последнего блока с полями в модалке ответов
     */
    private String getPathToField(
            final SurveyDataTypes type,
            final SurveyAnswerFieldList fieldName,
            final String fieldNumber
    ) {
        return new PathBuilder()
                .setBlock(CONFIG_TAB_LIST)
                .setBlockArgument("Название", type.getTabName())
                .setBlock(VARIANTS_FIELD_BLOCK_NAME)
                .setBlockNumber(fieldNumber)
                .setField(fieldName.getFieldName())
                .build();
    }

    /**
     * Метод конвертирует мапу с конфигурацией виджета из шага в структуру, разбитую на виджеты, типы полей и
     * их значения, готовую для использования в методе заполнения
     */
    Map<SurveyDataTypes, List<Map<SurveyAnswerFieldList, String>>> prepareData(
            final Map<String, String> data,
            final SurveyWidgetTypes widgetType
    ) {
        final Map<SurveyDataTypes, List<Map<SurveyAnswerFieldList, String>>> preparedData = new LinkedHashMap<>();
        data.forEach((k, v) -> {
            final List<Map<SurveyAnswerFieldList, String>> answerList = new LinkedList<>();
            final SurveyDataTypes dataType = SurveyDataTypes.getDataType(k);
            if (dataType == SurveyDataTypes.CORRECT_ANSWER) {
                final Map<SurveyAnswerFieldList, String> fields = new LinkedHashMap<>();
                fields.put(SurveyAnswerFieldList.TEXT, v.trim());
                answerList.add(fields);
            } else {
                for (final String answer : v.split(";")) {
                    final Map<SurveyAnswerFieldList, String> fields = new LinkedHashMap<>();
                    allocateFieldsByType(dataType, fields, widgetType, answer.trim());
                    answerList.add(fields);
                }
            }
            preparedData.put(dataType, answerList);
        });
        return preparedData;
    }

    private void allocateFieldsByType(
            final SurveyDataTypes dataType,
            final Map<SurveyAnswerFieldList, String> fields,
            final SurveyWidgetTypes widgetType,
            final String answer
    ) {
        if (dataType == SurveyDataTypes.QUESTION) {
            fields.put(SurveyAnswerFieldList.TEXT, answer);
        } else if (dataType == SurveyDataTypes.MULTI_SELECT) {
            final boolean state = DriverUtils.getBoolean(answer);
            fields.put(SurveyAnswerFieldList.TEXT, String.valueOf(state));
        } else if (widgetType == SurveyWidgetTypes.SurveyMatrixSelectMultiple) {
            final String[] fieldParts = answer.split(",");
            if (fieldParts.length == 2) {
                final SurveyAnswerFieldRenderVariant fieldRenderVariant =
                        SurveyAnswerFieldRenderVariant.getFieldRenderVariant(fieldParts[0].trim());
                fields.put(SurveyAnswerFieldList.CELL_TYPE, fieldRenderVariant.name().toLowerCase());
                fields.put(SurveyAnswerFieldList.OPTION, fieldParts[1].trim());
            } else {
                fields.put(SurveyAnswerFieldList.TEXT, answer);
            }
        } else if (widgetType == SurveyWidgetTypes.SurveyImageSelect) {
            fields.put(SurveyAnswerFieldList.IMAGE_LINK, answer);
        } else if(widgetType == SurveyWidgetTypes.SurveyTextInputMultiple) {
            fields.put(SurveyAnswerFieldList.OPTION, answer);
        } else {
            fields.put(SurveyAnswerFieldList.TEXT, answer);
        }
    }
}
