package ru.sbt.edu_power.e2e_core.elements;

import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Представляет из себя строку с перечислением значений через запятую, например:
 * Русский язык, Литература, Обществознание
 * <p>
 * Заполнение элемента не предполагается
 * Русский язык, Литература, Обществознание - список значений элемента, которые могут идти в любом порядке
 * <p>
 * Для проверки на вхождение элементов списка, в начало нужно добавить звёздочку:
 * Любимые предметы: *Русский язык, Обществознание
 */
public class SingleLineEnumeration extends TypifiedElement implements Validatable {
    public SingleLineEnumeration(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public void clear() {
        throw new AutotestError("Очистка элемента не предполагается");
    }

    @Override
    public void sendKeys(final CharSequence... keysToSend) {
        throw new AutotestError("Заполнение элемента не предполагается");
    }

    @Override
    public boolean validate(final String expectedDraft) {
        final String expected = expectedDraft.replaceAll("\\*", "");
        final List<String> expectedList = Arrays.stream(expected.split(","))
                                                .map(DataProcessing::decodeValue)
                                                .collect(Collectors.toList());
        final List<String> actualList = Arrays
                .stream(
                        getWrappedElement()
                                .getText()
                                .split(",")
                )
                .map(String::trim)
                .map(s -> s.replaceAll("\u00AD", ""))
                .collect(Collectors.toList());
        if (expectedDraft.startsWith("*")) {
            return actualList.containsAll(expectedList);
        } else {
            return actualList.containsAll(expectedList) && expectedList.containsAll(actualList);
        }
    }

    @Override
    public String getFieldValue() {
        return getText();
    }
}
