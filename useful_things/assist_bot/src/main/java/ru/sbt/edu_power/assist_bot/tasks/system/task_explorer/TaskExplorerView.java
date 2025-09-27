package ru.sbt.edu_power.assist_bot.tasks.system.task_explorer;

import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.Section;
import ru.sbt.edu_power.assist_bot.task_flow.Task;
import ru.sbt.edu_power.assist_bot.task_flow.TaskExplorer;
import ru.sbt.edu_power.assist_bot.task_flow.TaskGlue;
import ru.sbt.edu_power.assist_bot.tasks.system.task_explorer.components.ExecutionTaskSection;
import ru.sbt.edu_power.assist_bot.tasks.system.task_explorer.components.TaskGlueSection;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskExplorerView extends AbstractModal {

    @Override
    public List<Section> getSections() {
        final Set<TaskGlue> taskGlueList = TaskExplorer.getInstance().getTaskGlueList();
        final List<Section> sections = taskGlueList
                .stream()
                .map(TaskGlueSection::new)
                .collect(Collectors.toList());
        final Set<Task> tasks = TaskExplorer.getInstance().getExecutedTasks();
        tasks.stream()
             .map(ExecutionTaskSection::new)
             .forEach(sections::add);
        return sections;
    }

    @Override
    public String getName() {
        return "Диспетчер задач";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(() -> {});
    }
}
