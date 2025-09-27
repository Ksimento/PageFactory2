package ru.sbt.edu_power.assist_bot.tasks.test_run.test_run_repartition;

import com.slack.api.model.block.LayoutBlock;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

// генерация кнопки для открытия модалки с настройками для перемещения тест-кейсов
public class TestRunRepartitionRegistration extends AbstractSlackRegistration {

    @Override
    public SlackRoles[] acceptedRoles() {
        return new SlackRoles[]{SlackRoles.ADMIN, SlackRoles.USER};
    }

    @Override
    public String getName() {
        return "Перемещение ТК";
    }

    @Override
    public StartSection getStartSection() {
        return StartSection.REGRESS;
    }

    @Override
    public LayoutBlock getStartButton() {
        return getStartButton("Перераспределить", "Инструмент для перемещения тест-кейсов между тестировщиками в тест-сете");
    }

    @Override
    public void registerStartButton() {
        registerStartButton(TestRunRepartitionExternalView::new);
    }
}
