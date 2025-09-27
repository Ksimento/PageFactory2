package ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces;

public interface IFullDeploy {
    String getDockerTagFront();
    String getDockerTagBack();
    String getDockerTagCatalog();
    String getDockerTagUserService();
    String getDockerTagOrgStructure();
    String getDockerTagMfe();
    String getSchoolGroupTag();
    String getApimGravitee();
    String getKeycloakSber();
}
