package ru.sbt.edu_power.test_manager;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.RegressTestRunCreation;
import ru.sbt.edu_power.external_services.mattermost.Regressman;
import ru.sbt.edu_power.risk_assessment.TestCaseRiskAssessment;

import java.util.NoSuchElementException;

@Slf4j
public class TestRunCreation {
    private static final String EDU_CHANNEL = "qa";
    private static final String S21_CHANNEL = "s21_testing";
    private RegressTestRunCreation regressTestRunCreation;

    public void create() {
        Assert.assertNotEquals("Не указан риск", Environment.RISK_LEVEL, TCFields.Risk.UNDEFINED);
        Assert.assertNotNull("Не указана версия", Environment.VERSION);
        Assert.assertNotNull("Не указан проект", Environment.PROJECT);
        if (Environment.SKIP_RISK_UPDATE) {
            log.info(
                    "Обновление рисков пропущено. SKIP_RISK_UPDATE: {}; RISK_LEVEL: {}",
                    false,
                    Environment.RISK_LEVEL
            );
        } else {
            final TestCaseRiskAssessment assessment = new TestCaseRiskAssessment();
            assessment.collect(Environment.PROJECT.name());
        }
        regressTestRunCreation = new RegressTestRunCreation(Environment.PROJECT, Environment.VERSION);
        regressTestRunCreation.create(
                Environment.TEST_RUN_PRESET,
                Environment.RISK_LEVEL
        );
        sendMessage();
    }

    private void sendMessage() {
         switch (Environment.PROJECT){
            case S21:
                sendMessageRegressman(S21_CHANNEL) ;
                return;
            case EDU:
                sendMessageRegressman(EDU_CHANNEL);
                return;
            default:
                throw new NoSuchElementException("Указан несуществующий канал");
        }
    }

    private void sendMessageRegressman(String channel){
        Regressman.getInstance().sendPostByChannelName(
                channel,
                String.format(
                        "Создан тест-сет %s [%s](%s%s)",
                        regressTestRunCreation.getBlankTestRunCreation().getTestRunModel().getKey(),
                        regressTestRunCreation.getBlankTestRunCreation().getTestRunModel().getName(),
                        PropReader.get("jira.ui.testrun.endpoint"),
                        regressTestRunCreation.getBlankTestRunCreation().getTestRunModel().getKey()
                )
        );
    }
}
