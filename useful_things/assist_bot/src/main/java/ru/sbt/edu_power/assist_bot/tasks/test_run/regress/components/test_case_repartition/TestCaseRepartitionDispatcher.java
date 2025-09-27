package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition;

import com.slack.api.model.User;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.tasks.TestCaseMoveTask;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModelFolder;
import ru.sbt.edu_power.assist_bot.slack.users.SlackUsers;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;
import ru.sbt.edu_power.assist_bot.task_flow.templates.SlackMessage;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components.ITestRunRepartition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestCaseRepartitionDispatcher implements IDispatcher {
    private final AbstractModal view;

    public TestCaseRepartitionDispatcher(final AbstractModal view) {
        this.view = view;
    }

    @Override
    public void dispatch() {
        // список ID папок для перемещения
        final List<String> folders = ((ITestRunRepartition) view).getFolders();

        final TestCaseMove testCaseMove = new TestCaseMove(
                ((ITestRunRepartition) view).getTestRunSlice(),
                ((ITestRunRepartition) view).getFromUserId(),
                ((ITestRunRepartition) view).getToUserId(),
                TestCaseRepartitionUtils.getExecutions(
                        ((ITestRunRepartition) view).getTestCaseRepartition(), folders
                )
        );

        // список объектов папок (для построения полных путей)
        final List<TestCaseModelFolder> folderModels = folders
                .stream()
                .map(folderId -> ((ITestRunRepartition) view)
                        .getTestCaseRepartition()
                        .getFolderIdToFolderMap()
                        .get(folderId))
                .collect(Collectors.toList());

        // задача на перемещение
        final TestCaseMoveTask testCaseMoveTask = new TestCaseMoveTask(
                view,
                testCaseMove
        );


        // создание и запуск исполняемой очереди
        final TaskGlue taskGlue = new TaskGlue("Перемещение тест-кейсов", view.getUserId());
        taskGlue.add(testCaseMoveTask);

        // задача на уведомление в чат о перемещении
        final User user = SlackUsers.getInstance().getUser(view.getUserId());
        if (Objects.nonNull(user)) {
            final SlackMessage slackMessage = new SlackMessage(
                    () -> String.format(
                            "%s переместил %d тест-кейсов от %s к %s:\n%s",
                            user.getProfile().getDisplayName(),
                            testCaseMove.getExecutions().size(),
                            testCaseMove.getUserFrom().getDisplayName(),
                            testCaseMove.getUserTo().getDisplayName(),
                            folderModels.stream()
                                        .map(TestCaseRepartitionUtils::getFolderPath)
                                        .collect(Collectors.joining("\n"))
                    ),
                    user.getId(),
                    view
            );
            taskGlue.add(slackMessage, testCaseMoveTask, true, testCaseMoveTask::isTaskComplete)
                    .addQueueCompleteCondition(slackMessage::isTaskComplete);
        } else {
            taskGlue.addQueueCompleteCondition(testCaseMoveTask::isTaskComplete);
        }
        taskGlue.execute();
    }
}
