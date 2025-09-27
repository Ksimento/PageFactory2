package ru.sbt.edu_power.external_services.jira.test_manager.test_run.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TestResults {
    @SerializedName(value = "data")
    private List<Results> resultsList;
    @Getter
    @Setter
    public class Results {
        private Integer executionTime;
        private boolean automated;
        private TestResultStatus testResultStatus;
        private TestRun testRun;
        private TestCase testCase;
        private String executionDate;

        @Getter
        @Setter
        public class TestResultStatus{
            private String name;
        }
        @Getter
        @Setter
        public class TestRun{
            private String key;
        }
        @Getter
        @Setter
        public class TestCase{
            private String id;
        }
    }
}