package ru.sbt.edu_power.assist_bot.tasks.system.task_explorer.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.ConfirmationDialogObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractButtonFormField;
import ru.sbt.edu_power.assist_bot.task_flow.Task;
import ru.sbt.edu_power.assist_bot.task_flow.TaskExplorer;

public class ExecutionTaskSection extends AbstractSection {
    private final Task task;

    public ExecutionTaskSection(final Task task) {
        super(new Accessory(task));
        this.task = task;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                .blockId(getId())
                .accessory(getAccessory().getElement())
                .text(asText(task.getTaskName(), false))
                .build();
    }

    private static class Accessory extends AbstractButtonFormField {
        private final Task task;

        Accessory(final Task task) {
            this.task = task;
            setId(task.getUuid());
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
                                                task.getTaskName() +
                                                "?",
                                                false
                                        ))
                                        .build()
                                )
                                .value("tg" + task.getUuid())
                                .build();
        }

        @Override
        public String getName() {
            return "Завершение задания " + task.getTaskName();
        }

        @Override
        public void buttonCallback() {
            TaskExplorer.getInstance().stopTask(task.getUuid());
        }
    }
}
