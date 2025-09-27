package ru.sbt.sber_learning.test_runner;

import org.jsoup.select.Evaluator;
import org.openqa.selenium.By;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.auth.AuthAction;
import ru.sbt.edu_power.e2e_core.elements.checkbox.CheckBox;
import ru.sbt.edu_power.e2e_core.elements.dropdown.Select;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbt.edu_power.e2e_core.fields.FieldUtils;
import ru.sbt.edu_power.e2e_core.page_tags.PageGraphCollector;
import ru.sbt.sber_learning.pages.auth.AuthPage;
import ru.sbt.sber_learning.pages.root_pages.RootEDU;
import ru.sbt.sber_learning.pages.root_pages.RootS21;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.yandex.qatools.htmlelements.element.TextBlock;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import java.util.Arrays;

public class Configuration {
    public static void configure() {
        FieldUtils.configure(element -> {
            final Class<? extends TypifiedElement> fieldClass;
            final String TEXT_INPUT = "descendant-or-self::input[@type = 'text' or @type = 'number'] | descendant-or-self::textarea";
            final String CHECKBOX = "descendant-or-self::input[@type = 'checkbox']";
            final String SELECT = "descendant-or-self::div[@data-testid = 'dropdown'] " +
                                  "| descendant-or-self::button[@data-testid = 'UIKIT.Button' and contains(@class, 'MuiButtonBase-root')]" +
                                  "| descendant-or-self::button[contains(@data-testid,'Dropdown') and contains(@class, 'MuiButtonBase-root')]";
            if (!element.findElements(By.xpath(TEXT_INPUT)).isEmpty()) {
                fieldClass = TextInput.class;
            } else if (!element.findElements(By.xpath(CHECKBOX)).isEmpty()) {
                fieldClass = CheckBox.class;
            } else if (!element.findElements(By.xpath(SELECT)).isEmpty()) {
                fieldClass = Select.class;
            } else {
                fieldClass = TextBlock.class;
            }
            return fieldClass;
        });

        ClickActions.configure(() -> PageContext.getCurrentPage() instanceof AuthPage);

        PageGraphCollector.configure((page) -> page.equals(RootEDU.class) || page.equals(RootS21.class));

        PageControls.configure(() ->
                Arrays.asList(
                        new Evaluator.AttributeWithValue("data-testid", "STUDENT.ActivityMonitor.ActivityTimer"),
                        new Evaluator.AttributeWithValue("data-testid", "Badge.Wrapper.UikitV3"),
                        new Evaluator.AttributeWithValue("id", "notificationBlock"),
                        new Evaluator.Tag("script"),
                        new Evaluator.Tag("noscript")
                )
        );

        AuthAction.configure(AuthPage.class);
    }
}
