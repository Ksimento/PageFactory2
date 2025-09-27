package ru.sbt.edu_power.allure_comparator;

import ru.sbt.edu_power.external_services.jenkins.allure.enums.Children;

public class ChildrenPair {
    public final Children fst;
    public final Children snd;

    public ChildrenPair(
            final Children fst,
            final Children snd
    ) {
        this.fst = fst;
        this.snd = snd;
    }

}
