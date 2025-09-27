package ru.sbt.edu_power.assist_bot.yaml_configurations;

import lombok.Getter;
import lombok.Setter;
import ru.sbt.edu_power.external_services.version_releases.Services;

import java.util.ArrayList;
import java.util.List;

// Модель данных для yaml файла с докер-тегами для деплоя стендов через фулл деплой
@Setter
@Getter
public class DeployVersions {
    public Global edupower_back;
    public Global edupower_front;
    public Global edupower_catalog;
    public Global school21_front;
    public Global school21_front_exam;
    public Elspet elspet;
    public List<MServ> microservices = new ArrayList<>();
    public MFE microfrontend;

    public MServ getMicroserviceByName(final String name) {
        return microservices.stream()
                .filter(ms -> ms.servicename.equals(name))
                .findFirst()
                .orElse(null);
    }

    public MServ getMicroserviceByName(final Services name) {
        return microservices.stream()
                            .filter(ms -> ms.servicename.equals(name.getServiceName()))
                            .findFirst()
                            .orElse(null);
    }

    @Setter
    @Getter
    public static class Global {
        public boolean deploy;
        public String dockertag;
        public String k8s_config_branch;

        public void setDockertag(final String dockerTag) {
            this.dockertag = dockerTag;
            k8s_config_branch = getK8sConfig(dockerTag);
        }

        private String getK8sConfig(final String dockerTag) {
            return dockerTag.replace("__", "/").split("_")[0].split("-")[0];
        }
    }

    @Setter
    @Getter
    public static class MServ {
        public String servicename;
        public String dockertag;
        public String k8s_config_branch;
        public boolean deploy;

        public void setDockertag(final String dockerTag) {
            this.dockertag = dockerTag;
            if (!"master".equals(k8s_config_branch)) {
                k8s_config_branch = getK8sConfig(dockerTag);
            }
            deploy = true;
        }

        private String getK8sConfig(final String dockerTag) {
            return dockerTag.replace("__", "/").split("_")[0];
        }
    }

    @Setter
    @Getter
    public static class MFE {
        public Global edupower_frontend_mfe;
    }

    @Setter
    @Getter
    public static class Elspet {
        public String elspet_deploy_versions;
        public boolean deploy;

        public void setElspetDeployVersions(final String elspetDeployVersions) {
            this.elspet_deploy_versions = elspet_deploy_versions;
            deploy = true;
        }
    }
}
