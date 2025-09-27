package ru.sbt.edu_power.test_manager.report_component_tests;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportComponentTests {
    private static final String JOB_URL = System.getProperty("jobUrl");

    public void evaluate(){

        List<ReportComponent> reportComponentList = new ArrayList<>();
        Arrays.stream(JOB_URL.split(",")).forEach(e-> { final String name = e.split("=")[0].trim();
            final String url = e.split("=")[1].trim();
            reportComponentList.add(new ReportComponent(name,url).evaluate());});
        new ReportPageComponent("96185578").generateDocument(reportComponentList);
        
    }

}
