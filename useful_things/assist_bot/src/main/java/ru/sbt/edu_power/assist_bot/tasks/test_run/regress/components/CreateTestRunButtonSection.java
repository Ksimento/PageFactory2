package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.RegressPreset;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.RegressStartView;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.RegressTestRunCreation;
import ru.sbt.edu_power.assist_bot.slack.SlackClient;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractButtonFormField;
import ru.sbt.edu_power.assist_bot.task_flow.templates.SlackMessage;

import java.util.function.BooleanSupplier;

public class CreateTestRunButtonSection extends AbstractSection {

    public CreateTestRunButtonSection(
            final RegressStartView view
    ) {
        super(new Accessory(view));
    }

    public CreateTestRunButtonSection(
            final RegressStartView view,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(view), isAndCondition, constructConditions);
    }

    @Override
    public boolean constructCondition() {
        return super.constructCondition() && !getAccessory().isFilled();
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .text(asText("Для продолжения нужно создать новый тест-сет или выбрать существующий", false))
                           .blockId(getId())
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractButtonFormField {
        private final RegressStartView view;

        Accessory(final RegressStartView view) {
            this.view = view;
        }

        @Override
        public BlockElement getElement() {
            return getButton("Создать новый тест-сет", "create");
        }

        @Override
        public String getName() {
            return "Кнопка создания тест-сета";
        }

        @Override
        public void buttonCallback() {
            final RegressPreset preset = RegressPreset.valueOf(view.getRegressPresetSelectSection()
                                                                   .getAccessory()
                                                                   .getValue());
            final RegressTestRunCreation.Preset testRunPreset;
            switch (preset) {
                case FULL_REGRESS:
                    testRunPreset = RegressTestRunCreation.Preset.FULL_REGRESS;
                    break;
                case FULL_REGRESS_MFE:
                    testRunPreset = RegressTestRunCreation.Preset.FULL_REGRESS_MFE;
                    break;
                case SMALL_REGRESS:
                    testRunPreset = RegressTestRunCreation.Preset.SMALL_REGRESS;
                    break;
                case SMALL_REGRESS_MFE:
                    testRunPreset = RegressTestRunCreation.Preset.SMALL_REGRESS_MFE;
                    break;
                case HOT_FIX:
                    testRunPreset = RegressTestRunCreation.Preset.AUTO_ONLY;
                    break;
                case HOT_FIX_MFE:
                    testRunPreset = RegressTestRunCreation.Preset.AUTO_ONLY_MFE;
                    break;
                default:
                    throw new AssistBotException("Нельзя создать тест-сет для типа регресса " + preset);
            }
            final RegressTestRunCreation regressTestRunCreation = new RegressTestRunCreation(
                    view.getJiraProjectId(),
                    view.getTestRunVersion()
            );
            view.getRegressTestRunCreationContainer().setObject(regressTestRunCreation);
            final TCFields.Risk risk = TCFields.Risk.valueOf(view
                    .getTestRunRiskDependencySelect()
                    .getAccessory()
                    .getValue());
            if (risk != TCFields.Risk.LOWEST) {
                SlackClient.sendText(
                        "Перед создание тест-сета будет выполнено обновление поля Риск в тест-кейсах",
                        view.getUserId()
                );
            }
            new Thread(() -> regressTestRunCreation.create(testRunPreset, risk), "Создание тест-сета " + testRunPreset.name()).start();
            new SlackMessage(
                    () -> {
                        final String testSetKey = regressTestRunCreation
                                .getBlankTestRunCreation()
                                .getTestRunModel()
                                .getKey();
                        return String.format(
                                "Создан тест-сет для проведения регресса <https://jira.pcbltools.ru/jira/secure/Tests.jspa#/testPlayer/%s|%s (%s)>",
                                testSetKey,
                                view
                                        .getRegressTestRunCreationContainer()
                                        .getObject()
                                        .getBlankTestRunCreation()
                                        .getTestRunModel()
                                        .getName(),
                                testSetKey
                        );
                    },
                    view.getSlackChannel(),
                    Main.QUEUE_EXECUTOR,
                    view,
                    true,
                    () -> regressTestRunCreation.getStatus() == TaskExecutionStatus.SUCCESS
            );
        }
    }
}
