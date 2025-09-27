package ru.sbt.edu_power.assist_bot.tasks.test_run.remove_tests;

import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;
import ru.sbt.edu_power.assist_bot.task_flow.templates.SlackMessage;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestCaseRemoveFromTestRun;

public class RemoveTestsDispatcher implements IDispatcher {
    private final RemoveTestsView view;

    public RemoveTestsDispatcher(final RemoveTestsView view) {
        this.view = view;
    }

    @Override
    public void dispatch() {
        final String testRunKey = view.getTestRunSelectSection().getAccessory().getValue();
        final TestCaseRemoveFromTestRun testCaseRemoveFromTestRun = new TestCaseRemoveFromTestRun(testRunKey);
        testCaseRemoveFromTestRun.execute();
        new SlackMessage(
                () -> "Не пройденные тест-кейсы из тест-сета " + testRunKey + " удалены",
                view.getUserId(),
                Main.QUEUE_EXECUTOR,
                view
        );
    }
}
