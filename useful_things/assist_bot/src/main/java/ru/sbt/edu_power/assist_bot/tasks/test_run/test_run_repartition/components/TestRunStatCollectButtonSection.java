package ru.sbt.edu_power.assist_bot.tasks.test_run.test_run_repartition.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunSlice;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.TestRunSelectSection;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractButtonFormField;
import ru.sbt.edu_power.assist_bot.task_flow.Container;

import java.util.UUID;
import java.util.function.BooleanSupplier;

@Slf4j
public class TestRunStatCollectButtonSection extends AbstractSection {
    private final TestRunSelectSection testRunSelectSection;

    public TestRunStatCollectButtonSection(
            final Container<TestRunSlice> testRunSliceContainer,
            final TestRunSelectSection testRunSelectSection
    ) {
        super(new Accessory(testRunSliceContainer, testRunSelectSection));
        this.testRunSelectSection = testRunSelectSection;
    }

    public TestRunStatCollectButtonSection(
            final Container<TestRunSlice> testRunSliceContainer,
            final TestRunSelectSection testRunSelectSection,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(testRunSliceContainer, testRunSelectSection), isAndCondition, constructConditions);
        this.testRunSelectSection = testRunSelectSection;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                .blockId(getId())
                .text(asText(String.format(
                        "Для продолжения нужно загрузить данные тест-сета %s. Для этого потребуется некоторое время, не более двух минут",
                        testRunSelectSection.getAccessory().getValue()
                ), false))
                .accessory(getAccessory().getElement())
                .build();
    }

    private static class Accessory extends AbstractButtonFormField {
        private final Container<TestRunSlice> testRunSliceContainer;
        private final TestRunSelectSection testRunSelectSection;

        Accessory(
                final Container<TestRunSlice> testRunSliceContainer,
                final TestRunSelectSection testRunSelectSection
        ) {
            this.testRunSliceContainer = testRunSliceContainer;
            this.testRunSelectSection = testRunSelectSection;
        }

        @Override
        public BlockElement getElement() {
            return getButton(getName(), "download");
        }

        @Override
        public String getName() {
            return "Загрузка тест-сета";
        }

        @Override
        public void buttonCallback() {
            final String timer = UUID.randomUUID().toString();
            Timer.startTimer(timer);
            final TestRunSlice testRunSlice = new TestRunSlice(testRunSelectSection.getAccessory().getValue());
            testRunSlice.load();
            testRunSliceContainer.setObject(testRunSlice);
            log.info("Данные тест-сета загружены {}", Timer.getDelta(timer, "buttonCallback"));
        }
    }
}
