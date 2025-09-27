package ru.sbt.edu_power.e2e_core.test_runner;

import cucumber.api.Scenario;

import java.net.MalformedURLException;

public interface IHooks {
    void beforeSkipTest(Scenario scenario);
    void beforeRememberStartTime(Scenario scenario);
    void beforeSplitStand(Scenario scenario) throws MalformedURLException;
    void beforeSetUserAgentAndWindowSize(Scenario scenario);
    void beforeDevToolsUp();
    void beforeDevToolsNetworkingUp(Scenario scenario);
    void beforeTestExecuted(Scenario scenario);
    void afterCollectStatistic();
    void afterNetworkDown();
    void afterCloseAllDrivers();
    void beforeMoonSeparator();
    void beforeNoHeadlessSeparator();
    void beforeDisableRedToastDetected();
    void beforeDisable404UpsDetected();
    void afterResultReport(Scenario scenario);
    void beforeMarkMousePosition(Scenario scenario);
}
