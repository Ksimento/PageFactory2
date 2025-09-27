import cucumber.api.CucumberOptions;
import org.junit.runner.RunWith;
import ru.sbt.sber_learning.test_runner.TestRunner;
import ru.sbtqa.tag.pagefactory.Tag;

@RunWith(TestRunner.class)
@CucumberOptions(monochrome = true,
        plugin = {"pretty","junit:target/cucumber/result.xml", "json:target/cucumber/pagefactory.json"},
        // Укажите здесь корневые пакеты, в которых хранятся шаги
        // Если вы написали свои классы с шагами, не забудьте указать здесь пакеты, в которых они находятся
        glue = {"ru.sbtqa.tag.stepdefs.ru", "ru.sbt.sber_learning.stepDefs", "ru.sbt.edu_power.e2e_core.step_defs"},
        // Здесь можно указать тэг теста или нескольких тестов, которые нужно запустить
        tags = {"@sysadmin_pages_event"}
)

public class CucumberTest extends Tag {
}
