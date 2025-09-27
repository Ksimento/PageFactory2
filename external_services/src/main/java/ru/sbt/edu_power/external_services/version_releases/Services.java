package ru.sbt.edu_power.external_services.version_releases;

import java.util.stream.Stream;

public enum Services {
    BACKEND("edupower_back", "BACKEND-SDP"),
    FRONTEND("edupower_front", "FRONTEND-SDP"),
    FRONTEND_MFE("edupower_frontend_mfe", "EDU-FRONTEND-MFE"),
    CATALOG("edupower_catalog", "CATALOG"),
    ORG_STRUCTURE("org-structure", "ORG-STRUCTURE"),
    EDU_USER_SERVICE("edu-user-service", "EDU-USER-SERVICE"),
    EDU_REGISTRATION_SERVICE("edu-registration-service", "EDU-REGISTRATION-SERVICE"),
    DATASPACE_CORE_SCHOOLGROUP("dataspace-core-schoolgroup", "DATASPACE-CORE-SCHOOLGROUP"),
    CLEVER_FRONTEND_MFE("clever-frontend-mfe", "CLEVER-FRONTEND-MFE"),
    CLEVER_ADMIN_FRONTEND("clever-admin-frontend", "CLEVER-ADMIN-FRONTEND"),
    ELSPET_IDENTITY("ELSPET_IDENTITY", "ELSPET_IDENTITY"),
    ELSPET_CAMUNDA("ELSPET_CAMUNDA", "ELSPET_CAMUNDA"),
    S21_FRONTEND_EXAM("school21_front_exam", "S21-APPLICATION-EXAM"),
    S21_FRONTEND("school21_front", "S21-APPLICATION"),
    FRONTEND_LANDING_ACCELERATORS("FRONTEND-LANDING-ACCELERATORS", "FRONTEND-LANDING-ACCELERATORS"),
    VAULT("VAULT", "VAULT"),
    APIM_GRAVITEE("apim-gravitee", "APIM-GRAVITEE"),
    ELSPET_HASURA_MIGRATION("ELSPET-HASURA-MIGRATION", "ELSPET-HASURA-MIGRATION"),
    KEYCLOAK_SBER("keycloak-sber", "KEYCLOAK-SBER"),
    OTHER("other", "other"),
    ;
    private final String serviceName;
    private final String nameInSlack;

    Services(final String serviceName, final String nameInSlack) {
        this.serviceName = serviceName;
        this.nameInSlack = nameInSlack;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getNameInSlack() {
        return nameInSlack;
    }

    public static Services getBySlackName(final String slackName) {
        return Stream.of(values())
                .filter(s -> slackName.startsWith(s.getNameInSlack()))
                .findFirst()
                .orElse(OTHER);
    }
}
