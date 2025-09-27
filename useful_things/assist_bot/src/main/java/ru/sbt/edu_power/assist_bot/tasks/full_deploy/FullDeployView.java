package ru.sbt.edu_power.assist_bot.tasks.full_deploy;

import ru.sbt.edu_power.external_services.version_releases.Services;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleasesChannelReader;
import ru.sbt.edu_power.assist_bot.services.jenkins.DockerTagSearch;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.DockerTagFromReleasesSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.DockerTagSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.DockerTagTextSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.StandSelectSection;
import ru.sbt.edu_power.assist_bot.slack.views.sections.TextSection;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.DefaultFullDeployTask;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IFullDeploy;

public class FullDeployView extends AbstractModal implements IFullDeploy {
    private final ReleasesChannelReader reader = new ReleasesChannelReader(this.getUserId());

    private final StandSelectSection standSelectSection =
            new StandSelectSection(
                    "Номер стенда в формате dev0-0"
//                    true,
//                    () -> deployProjectSelectSection.getAccessory().isFilled()
            );

    private final DockerTagSelectSection dockerTagSelectSectionFront =
            new DockerTagSelectSection(
                    DockerTagSearch.Repo.FRONT,
                    true,
                    () -> standSelectSection.getAccessory().isFilled()
            );

    private final DockerTagTextSection frontDockerTagTextSection =
            new DockerTagTextSection(dockerTagSelectSectionFront, "Тег для деплоя FRONT: ");

    private final DockerTagSelectSection dockerTagSelectSectionBack =
            new DockerTagSelectSection(
                    DockerTagSearch.Repo.BACK,
                    true,
                    () -> dockerTagSelectSectionFront.getAccessory().isFilled()
            );

    private final DockerTagTextSection backDockerTagTextSection =
            new DockerTagTextSection(dockerTagSelectSectionBack, "Тег для деплоя BACK: ");

    private final DockerTagFromReleasesSelectSection catalogTag = new DockerTagFromReleasesSelectSection(
            Services.CATALOG,
            reader,
            true,
            () -> dockerTagSelectSectionBack.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection userServiceTag = new DockerTagFromReleasesSelectSection(
            Services.EDU_USER_SERVICE,
            reader,
            true,
            () -> catalogTag.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection orgStructureServiceTag = new DockerTagFromReleasesSelectSection(
            Services.ORG_STRUCTURE,
            reader,
            true,
            () -> userServiceTag.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection frontMfeTag = new DockerTagFromReleasesSelectSection(
            Services.FRONTEND_MFE,
            reader,
            true,
            () -> orgStructureServiceTag.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection dataspaceCoreSchoolgroupTag = new DockerTagFromReleasesSelectSection(
            Services.DATASPACE_CORE_SCHOOLGROUP,
            reader,
            true,
            () -> frontMfeTag.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection apimGraviteeTag = new DockerTagFromReleasesSelectSection(
            Services.APIM_GRAVITEE,
            reader,
            true,
            () -> dataspaceCoreSchoolgroupTag.getAccessory().isFilled()
    );

    private final DockerTagFromReleasesSelectSection keycloakSber = new DockerTagFromReleasesSelectSection(
            Services.KEYCLOAK_SBER,
            reader,
            true,
            () -> apimGraviteeTag.getAccessory().isFilled()
    );

    private final TextSection textSection = new TextSection(
            () -> "Для деплоя будет использован дефолтный дамп. Это временное ограничение",
            true,
            () -> dockerTagSelectSectionBack.getAccessory().isFilled()
    );

    public FullDeployView() {
        reader.updateMessages();
        reader.removeUnsuccessfulServices();
    }

    @Override
    public String getName() {
        return "Деплой стенда";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(
                () -> new DefaultFullDeployTask(
                        standSelectSection.getAccessory().getValue(),
                        Main.QUEUE_EXECUTOR,
                        this
                )
        );
    }

    @Override
    public String getDockerTagFront() {
        return dockerTagSelectSectionFront.getAccessory().getValue();
    }

    @Override
    public String getDockerTagBack() {
        return dockerTagSelectSectionBack.getAccessory().getValue();
    }

    @Override
    public String getDockerTagCatalog() {
        return catalogTag.getAccessory().getValue();
    }

    @Override
    public String getDockerTagUserService() {
        return userServiceTag.getAccessory().getValue();
    }

    @Override
    public String getDockerTagOrgStructure() {
        return orgStructureServiceTag.getAccessory().getValue();
    }

    @Override
    public String getDockerTagMfe() {
        return frontMfeTag.getAccessory().getValue();
    }

    @Override
    public String getSchoolGroupTag() {
        return dataspaceCoreSchoolgroupTag.getAccessory().getValue();
    }

    @Override
    public String getApimGravitee() {
        return apimGraviteeTag.getAccessory().getValue();
    }

    @Override
    public String getKeycloakSber() {
        return keycloakSber.getAccessory().getValue();
    }
}
