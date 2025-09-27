package ru.sbt.edu_power.assist_bot.tasks.test_run.ift_assistant;

import ru.sbt.edu_power.assist_bot.task_flow.Container;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.issue.IssueStorage;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.IftAssistantTask;

public class IftDispatcher implements IDispatcher {
    private final IftAssistantView view;

    public IftDispatcher(final IftAssistantView view) {
        this.view = view;
    }

    @Override
    public void dispatch() {
        final IftAssistantTask task = new IftAssistantTask(
                view,
                getName(),
                new Container<>(new IssueStorage(
                        view.getProject(),
                        view.getVersion()
                )));
        new TaskGlue(getName(), view.getUserId())
                .add(task)
                .addQueueCompleteCondition(task::isTaskComplete)
                .execute();
    }

    private String getName() {
        return String.format("IFT ассистент %s: %s", view.getProject().name(), view.getVersion().getName());
    }
}
