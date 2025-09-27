package ru.sbt.edu_power.assist_bot.slack;

import com.slack.api.app_backend.interactive_components.payload.BlockActionPayload;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.assist_bot.slack.views.Element;
import ru.sbt.edu_power.assist_bot.task_flow.Reset;
import ru.sbt.edu_power.assist_bot.task_flow.Task;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class ManageFailedTasksButton extends Element {
    private final Map<Task, String> taskToButtonIdMap = new HashMap<>();
    private final Map<String, TaskGlue.Dependent> buttonIdToDependentMap = new HashMap<>();
    private final List<LayoutBlock> sectionBlocks = new ArrayList<>();
    private final TaskGlue taskGlue;

    public ManageFailedTasksButton(final TaskGlue taskGlue) {
        this.taskGlue = taskGlue;
    }

    public void postMessage(final TaskGlue.Dependent dependent, final String userId)
            throws IOException, SlackApiException {
        addErrorMessage(dependent);
        addSkipStageButton(dependent);
        collectSections(dependent);
        addInterruptScenarioButton();
        final ChatPostMessageResponse response = APP.client().chatPostMessage(r -> r
                .token(BOT_TOKEN)
                .channel(userId)
                .text("Выберите действия для продолжения работы")
                .blocks(sectionBlocks)
        );
        if (!response.isOk()) {
            log.error(response.toString());
        }
    }

    private void addErrorMessage(final TaskGlue.Dependent dependent) {
        final SectionBlock errorSection = SectionBlock
                .builder()
                .text(asMrkdn("*У нас есть проблема.* Задание '" +
                             dependent.getDependent().getTaskName() +
                             "' провалилось, что будешь делать?"))
                .build();
        sectionBlocks.add(errorSection);
    }

    private void addSkipStageButton(final TaskGlue.Dependent dependent) {
        final String skipUuid = UUID.randomUUID().toString();
        final ButtonElement button = ButtonElement
                .builder()
                .actionId(skipUuid)
                .text(asText("Пропустить", false))
                .value(skipUuid)
                .build();
        final SectionBlock skipStageSection = SectionBlock
                .builder()
                .blockId("block_" + skipUuid)
                .accessory(button)
                .text(asText("Зачесть задание '" +
                             dependent.getDependent().getTaskName() +
                             "' как выполненное и продолжить со следующего шага", false))
                .build();
        sectionBlocks.add(skipStageSection);
        APP.blockAction(skipUuid, (req, ctx) -> {
            dependent.setStatus(TaskExecutionStatus.SUCCESS);
            taskGlue.getQueue()
                    .forEach(d -> d
                            .getSources()
                            .stream()
                            .filter(s -> s.getSource().equals(dependent.getDependent()))
                            .forEach(TaskGlue.Source::clearCondition)
                    );
            final ChatUpdateResponse chatUpdateResponse = ctx.client().chatUpdate(u -> u
                    .blocks(new ArrayList<>())
                    .text("*Ну окей, я закрыл глаза на провал задания '" +
                          dependent.getDependent().getTaskName() +
                          "'.* _Но я не буду делать это вечно, сделайте уже что бы всё работало!_ :smirk_cat:")
                    .ts(req.getPayload().getMessage().getTs())
                    .channel(req.getPayload().getChannel().getId())
                    .token(BOT_TOKEN)
            );
            if (!chatUpdateResponse.isOk()) {
                log.error(chatUpdateResponse.toString());
            }
            return ctx.ack();
        });
    }

    private void addInterruptScenarioButton() {
        final String interruptUuid = UUID.randomUUID().toString();
        final ButtonElement button = ButtonElement
                .builder()
                .actionId(interruptUuid)
                .text(asText("Прервать", false))
                .style("danger")
                .value(interruptUuid)
                .build();
        final SectionBlock skipStageSection = SectionBlock
                .builder()
                .blockId("block_" + interruptUuid)
                .accessory(button)
                .text(asText("Прервать выполнение всего сценария", false))
                .build();
        sectionBlocks.add(skipStageSection);
        APP.blockAction(interruptUuid, (req, ctx) -> {
            taskGlue.stop();
            final ChatUpdateResponse chatUpdateResponse = APP.client().chatUpdate(r -> r
                    .blocks(new ArrayList<>())
                    .text("*Исполнено! сценарий остановлен*. _У тебя осталось ещё два желания_ :male_mage:")
                    .ts(req.getPayload().getMessage().getTs())
                    .channel(req.getPayload().getChannel().getId())
                    .token(BOT_TOKEN)
            );
            if (!chatUpdateResponse.isOk()) {
                log.error(chatUpdateResponse.toString());
            }
            return ctx.ack();
        });
    }

    private void collectSections(final TaskGlue.Dependent dependent) {
        generateButtonId(dependent);
        final SectionBlock dependentSection = SectionBlock
                .builder()
                .blockId("block_" +
                         taskToButtonIdMap.get(dependent.getDependent()))
                .accessory(getButton(dependent.getDependent()))
                .text(asText(
                        "Выполнить перезапуск начиная с задачи '" + dependent.getDependent().getTaskName() + "'",
                        false
                ))
                .build();
        sectionBlocks.add(dependentSection);
        // регистрация обработчика нажатия на кнопку
        APP.blockAction(taskToButtonIdMap.get(dependent.getDependent()), (req, ctx) -> {
            final BlockActionPayload.Action action = req.getPayload().getActions().get(0);
            if ("button".equals(action.getType())) {
                resetTasksState(buttonIdToDependentMap.get(action.getValue()));

                final ChatUpdateResponse chatUpdateResponse = ctx.client().chatUpdate(r -> r
                        .blocks(new ArrayList<>())
                        .text(
                                "*Исполнено! Все задачи перезапущены начиная с '" +
                                buttonIdToDependentMap.get(action.getValue()).getDependent().getTaskName() +
                                "'*. _Может пока по пиву?_ :beers:")
                        .channel(req.getPayload().getChannel().getId())
                        .ts(req.getPayload().getMessage().getTs())
                        .token(BOT_TOKEN)
                );

                if (!chatUpdateResponse.isOk()) {
                    log.error(chatUpdateResponse.toString());
                }
            }
            return ctx.ack();
        });
        dependent.getSources().forEach(source -> {
            taskGlue.getQueue()
                    .stream()
                    .filter(d -> d.getDependent().equals(source.getSource()))
                    .findFirst()
                    .ifPresent(this::collectSections);
        });
    }

    private BlockElement getButton(final Task task) {
        return ButtonElement.builder()
                            .text(asText("Перезапустить", false))
                            .actionId(taskToButtonIdMap.get(task))
                            .value(taskToButtonIdMap.get(task))
                            .build();
    }

    private void generateButtonId(final TaskGlue.Dependent dependent) {
        taskToButtonIdMap.put(dependent.getDependent(), UUID.randomUUID().toString());
        buttonIdToDependentMap.put(taskToButtonIdMap.get(dependent.getDependent()), dependent);
    }

    private void resetTasksState(final TaskGlue.Dependent dependent) {
        dependent.setStatus(TaskExecutionStatus.NOT_STARTED);
        if (dependent.getDependent() instanceof Reset) {
            ((Reset) dependent.getDependent()).reset();
        }
        taskGlue.getQueue()
                .stream()
                .filter(d -> d.getSources().stream().anyMatch(s -> s.getSource().equals(dependent.getDependent())))
                .forEach(this::resetTasksState);
    }

}
