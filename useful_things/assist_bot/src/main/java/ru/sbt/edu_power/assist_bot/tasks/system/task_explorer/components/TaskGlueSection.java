package ru.sbt.edu_power.assist_bot.tasks.system.task_explorer.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.ConfirmationDialogObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractButtonFormField;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;

@Slf4j
public class TaskGlueSection extends AbstractSection {
    private final TaskGlue taskGlue;

    public TaskGlueSection(final TaskGlue taskGlue) {
        super(new Accessory(taskGlue));
        this.taskGlue = taskGlue;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .text(asText(taskGlue.getName(), false))
                           .accessory(getAccessory().getElement())
                           .blockId(getId())
                           .build();
    }

    private static class Accessory extends AbstractButtonFormField {
        private final TaskGlue taskGlue;

        Accessory(final TaskGlue taskGlue) {
            this.taskGlue = taskGlue;
            setId(taskGlue.getUuid());
        }

        @Override
        public BlockElement getElement() {
            return ButtonElement.builder()
                                .actionId(getId())
                                .text(asText("Завершить", false))
                                .confirm(ConfirmationDialogObject
                                        .builder()
                                        .title(asText("Завершение задания", false))
                                        .confirm(asText("Завершить", false))
                                        .deny(asText("Отмена", false))
                                        .text(asText(
                                                "Вы действительно хотите завершить задачу " +
                                                taskGlue.getName() +
                                                "?",
                                                false
                                        ))
                                        .build()
                                )
                                .value("tg" + taskGlue.getUuid())
                                .build();
        }

        @Override
        public String getName() {
            return "Завершение задания " + taskGlue.getName();
        }

        @Override
        public void buttonCallback() {
            taskGlue.stop();
        }
    }
}
