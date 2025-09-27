package ru.sbt.edu_power.external_services.jenkins.allure;

import lombok.Getter;

import java.util.stream.Stream;

@Getter
public enum JobType {
    NIGHTLY_BUILD_EDU("edu-nightly-build", true, false, true, true, true,false),
    NIGHTLY_BUILD_BTC("btc-nightly-build", false, false, false, false, false,true),
    DAILY_BUILD_EDU("edu-daily-build", true, false, true, true, true,false),
    NIGHTLY_BUILD_S21("s21-nightly-build", true, false, false, false, true,false),
    DAILY_BUILD_S21("s21-daily-build", true, false, false, false, false,false),
    E2E_JAVA_ACTUALIZATION("e2e-java-actualization",true, false, false, false, false,false),
    E2E_JS_ACTUALIZATION("e2e-js-actualization",false, false, true, false, false,false),
    QA_JAVA_UI_PARALLEL("qa-java-ui-parallel",false, true, false, false, false,false),
    SIMPLE_ALLURE_JOB("",false, false, false, false,false,false);

    private final String pattern;
    private final boolean hasJavaParallel;
    private final boolean hasJavaUi;
    private final boolean hasJsUi;
    private final boolean hasJsFt1Ui;
    private final boolean hasJavaApi;
    private final boolean hasBootcamp;

    JobType(
            final String pattern,
            final boolean hasJavaParallel,
            final boolean hasJavaUi,
            final boolean hasJsUi,
            final boolean hasJsFt1Ui,
            final boolean hasJavaApi,
            final boolean hasBootcamp
    ) {
        this.pattern = pattern;
        this.hasJavaParallel = hasJavaParallel;
        this.hasJavaUi = hasJavaUi;
        this.hasJsUi = hasJsUi;
        this.hasJsFt1Ui = hasJsFt1Ui;
        this.hasJavaApi = hasJavaApi;
        this.hasBootcamp = hasBootcamp;
    }

    public static JobType determineJobType(final String jobUrl) {
        return Stream.of(JobType.values())
                     .filter(t -> jobUrl.contains(t.getPattern()))
                     .findFirst()
                     .orElse(SIMPLE_ALLURE_JOB);
    }
}
