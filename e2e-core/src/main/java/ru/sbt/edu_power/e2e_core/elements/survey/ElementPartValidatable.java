package ru.sbt.edu_power.e2e_core.elements.survey;

import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbtqa.tag.qautils.errors.AutotestError;

/**
 * Интерфейс определяет возможность элемента проверять текст вариантов ответа
 */
public interface ElementPartValidatable extends WebElement {
    /**
     * Метод должен реализовать возможность проверить ожидаемое значение варианта ответа
     * с фактическим по номеру варианта ответа
     * @param elementPartId номер варианта ответа
     * @param expected      ожидаемое значение в формате MultipleTypeContent
     * @return              если значения сходятся, возвращаем true
     */
    boolean partialValidate(final String elementPartId, final String expected);

    /**
     * Метод должен реализовать возможность получения текста варианта ответа по его номеру
     * @param elementPartId номер варианта ответа в формате MultipleTypeContent
     * @return              актуальный текст варианта ответа
     */
    String getPartContent(final String elementPartId);

    /**
     * Предполагаем, что варианты ответов проверяем по порядковому номеру
     * @param elementPartId номер варианта ответа
     * @return              возвращаем int номер, если данные в запросе определены верно
     */
    default int getElementNumber(final String elementPartId) {
        final Integer elementNumber = DriverUtils.getNumber(elementPartId);
        if (null == elementNumber) {
            throw new AutotestError("Требуется указать номер элемента");
        }
        return elementNumber;
    }
}
