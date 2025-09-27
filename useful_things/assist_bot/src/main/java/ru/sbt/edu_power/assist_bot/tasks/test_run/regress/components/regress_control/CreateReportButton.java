package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.regress_control;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.ConfirmationDialogObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractButtonFormField;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public class CreateReportButton extends AbstractSection {

    protected CreateReportButton(
            final AtomicBoolean generateFinalReport,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(generateFinalReport), isAndCondition, constructConditions);
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .text(asText("Выполнить генерацию итогового отчёта и остановить ассистента", false))
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractButtonFormField {
        private final AtomicBoolean generateFinalReport;

        Accessory(final AtomicBoolean generateFinalReport) {
            this.generateFinalReport = generateFinalReport;
        }

        @Override
        public BlockElement getElement() {
            return ButtonElement.builder()
                                .actionId(getId())
                                .value("report")
                                .text(asText(getName(), false))
                                .confirm(
                                        ConfirmationDialogObject.builder()
                                                                .text(asText("Создать итоговый отчёт?", false))
                                                                .deny(asText("Отмена", false))
                                                                .confirm(asText("Создать", false))
                                                                .title(asText("Итоговый отчёт", false))
                                                                .build()
                                )
                                .build();
        }

        @Override
        public String getName() {
            return "Сгенерировать отчёт";
        }

        @Override
        public void buttonCallback() {
            generateFinalReport.set(true);
        }
    }
}
