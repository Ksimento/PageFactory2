package ru.sbt.sber_learning.stepDefs;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import ru.sbt.edu_power.e2e_core.test_runner.AfterTest;
import ru.sbt.edu_power.e2e_core.test_runner.IHooks;

import java.net.MalformedURLException;

public class Hooks implements IHooks {
    private final AfterTest afterTest = new AfterTest();

    @Override
    @Before(value = "@skip", order = 1)
    public void beforeSkipTest(final Scenario scenario) {
        afterTest.beforeSkipTest(scenario);
    }

    @Override
    @Before(order = 1)
    public void beforeRememberStartTime(final Scenario scenario) {
        afterTest.beforeRememberStartTime(scenario);
    }

    @Override
    @Before
    public void beforeSplitStand(final Scenario scenario) throws MalformedURLException {
        afterTest.beforeSplitStand(scenario);
    }

    @Override
    @Before(value = "not @jira_report", order = 10)
    public void beforeSetUserAgentAndWindowSize(final Scenario scenario) {
        afterTest.beforeSetUserAgentAndWindowSize(scenario);
    }

    @Override
    @Before(order = 20)
    public void beforeDevToolsUp() {
        afterTest.beforeDevToolsUp();
    }

    @Override
    @Before(value = "not @no_toast", order = 30)
    public void beforeDevToolsNetworkingUp(final Scenario scenario) {
        afterTest.beforeDevToolsNetworkingUp(scenario);
    }

    @Override
    @After(order = 10)
    public void afterCollectStatistic() {
        afterTest.afterCollectStatistic();
    }

    @Override
    @After(order = 100)
    public void afterNetworkDown() {
        afterTest.afterNetworkDown();
    }

    @Override
    @After(order = 500)
    public void afterCloseAllDrivers() {
        afterTest.afterCloseAllDrivers();
    }

    @Override
    @Before(value = "@moon_only")
    public void beforeMoonSeparator() {
        afterTest.beforeMoonSeparator();
    }

    @Override
    @Before(value = "@no_headless")
    public void beforeNoHeadlessSeparator() {
        afterTest.beforeNoHeadlessSeparator();
    }

    @Override
    @Before(value = "@no_toast")
    public void beforeDisableRedToastDetected() {
        afterTest.beforeDisableRedToastDetected();
    }

    @Override
    public void beforeDisable404UpsDetected() {
        afterTest.beforeDisable404UpsDetected();
    }

    @Override
    @After
    public void afterResultReport(final Scenario scenario) {
        afterTest.afterResultReport(scenario);
    }

    @Override
    public void beforeMarkMousePosition(final Scenario scenario) {
        afterTest.beforeMarkMousePosition(scenario);
    }

    @Override
    @Before(order = 10001)
    public void beforeTestExecuted(Scenario scenario) {
        afterTest.beforeSkipTest(scenario);
    }
}
