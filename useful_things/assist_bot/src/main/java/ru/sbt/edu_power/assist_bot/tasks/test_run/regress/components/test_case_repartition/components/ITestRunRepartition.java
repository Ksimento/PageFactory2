package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.components;

import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run.TestRunSlice;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.TestCaseRepartition;

import java.util.List;

// интерфейс для методов модалки перемещения тест-кейсов
public interface ITestRunRepartition {
    String getFromUserId();
    String getToUserId();
    TestRunSlice getTestRunSlice();
    TestCaseRepartition getTestCaseRepartition();
    String getChatId();
    List<String> getFolders();
}
