package ru.sbt.edu_power.e2e_core.survey;

import ru.sbtqa.tag.datajack.Stash;

public class SurveyExecutor {
    public static SurveyStepActions getSurveyStep() {
        final String SURVEY_STEP_ACTIONS = "SurveyStepActions";
        if (!Stash.asMap().containsKey(SURVEY_STEP_ACTIONS)) {
            Stash.put(SURVEY_STEP_ACTIONS, new SurveyStepActions());
        }
        return Stash.getValue(SURVEY_STEP_ACTIONS);
    }
}
