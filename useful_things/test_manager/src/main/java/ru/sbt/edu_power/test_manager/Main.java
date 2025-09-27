package ru.sbt.edu_power.test_manager;

import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.test_manager.alarm_400_branch.AlarmSendMessage;
import ru.sbt.edu_power.test_manager.check_pr_running_tests.CheckRunningTest;
import ru.sbt.edu_power.test_manager.delete_old_branch.DeleteOldBranch;
import ru.sbt.edu_power.test_manager.old_branch_notify.OldBranchNotify;
import ru.sbt.edu_power.test_manager.pull_request_no_build.LoadBranch;
import ru.sbt.edu_power.test_manager.pull_request_no_build.PrintCommit;
import ru.sbt.edu_power.test_manager.regress_report.RegressReport;
import ru.sbt.edu_power.test_manager.remove_test_cases.TestCaseRemoveFromTestRun;
import ru.sbt.edu_power.test_manager.report_component_tests.ReportComponentTests;
import ru.sbt.edu_power.test_manager.report_estimate_time.ReportEstimateTime;
import ru.sbt.edu_power.test_manager.update_estimate_time_testcase.UpdateEstimateTime;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

@Slf4j
public class Main {

    public static void main(final String[] args) {
        JiraConnect.configureConnection();
        switch (Environment.TEST_MANAGER_PROCESS) {
            case TEST_RUN_CREATION:
                new TestRunCreation().create();
                break;
            case REMOVE_NOT_EXECUTED_TEST_CASES:
                final AtomicInteger pageSize = new AtomicInteger(300);
                final BooleanSupplier remover = () -> {
                    try {
                        new TestCaseRemoveFromTestRun(Environment.TEST_RUN_KEY, pageSize.get()).execute();
                    } catch (final JiraConnectionException e) {
                        pageSize.set(pageSize.get() / 2);
                        return false;
                    }
                    return true;
                };
                Timer.executeTimer(3600, remover);
                break;
            case REGRESS_REPORT:
                new RegressReport().report();
                break;
            case ALARM_SEND_MESSAGE:
                new AlarmSendMessage().sendMessage();
                break;
            case REPORT_COMPONENT:
                new ReportComponentTests().evaluate();
                break;
            case OLD_BRANCH_NOTIFICATION:
                new OldBranchNotify().evaluate();
                break;
            case CHECK_PR_RUNNING_TESTS:
                new CheckRunningTest().evaluate(BBRepos.EDU_FRONT);
                new CheckRunningTest().evaluate(BBRepos.EDU_BACK);
                new CheckRunningTest().evaluate(BBRepos.PROJECT_S21_APPLICATION_EXAM);
                new CheckRunningTest().evaluate(BBRepos.PROJECT_S21_ADMINISTRATION);
                new CheckRunningTest().evaluate(BBRepos.PROJECT_S21_APPLICATION);
                break;
            case REPORT_ESTIMATE_TIME:
                new ReportEstimateTime().createReport();
                break;
            case UPDATE_ESTIMATE_TIME:
                new UpdateEstimateTime().loadTestSet();
                break;
            case PULL_REQUEST_NO_BUILD:
                new PrintCommit().print(new LoadBranch().evaluate());
                break;
            case DELETE_OLD_BRANCH:
                new DeleteOldBranch().clearOldBranchRepository();
                break;
            default:
                throw new TestManagerException("Нет обработчика для процесса " +
                                               Environment.TEST_MANAGER_PROCESS.name());
        }
    }
}
