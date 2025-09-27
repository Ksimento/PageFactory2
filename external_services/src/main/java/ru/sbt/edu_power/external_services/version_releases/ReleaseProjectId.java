package ru.sbt.edu_power.external_services.version_releases;

import java.util.stream.Stream;

// Проекты, по которым выполняется релизный процесс
public enum ReleaseProjectId {
    SBERCLASS("Sberclass"),
    USER_SERVICE("Сервис пользователей"),
    MFE("MFE"),
    S21("Школа 21"),
    B2C("Иннополис"),
    ACC_LANDING("Акселератор лендинг"),
    ACC_SELF_REGISTRATION("Саморегистрация"),
    LIT("Сберграмотность"),
    EDU_SCHEDULE_SERVICE("edu-schedule-service"),
    HASURA("Hasura"),
    CATALOG("Catalog"),
    UNDEFINED("Неизвестный сервис");

    private final String service;

    ReleaseProjectId(final String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }

    public static ReleaseProjectId getByService(final String service) {
        return Stream.of(ReleaseProjectId.values())
                     .filter(v -> v.service.equals(service))
                     .findFirst()
                     .orElse(UNDEFINED);
    }
}
