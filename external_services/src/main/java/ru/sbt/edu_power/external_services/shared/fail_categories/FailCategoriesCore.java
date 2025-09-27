package ru.sbt.edu_power.external_services.shared.fail_categories;

import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.jira.tc_verifier.TCFields;

public class FailCategoriesCore {
    public static IFailCategories determine(final String project, final String step, final String trace) {
        final TCFields.ProjectId projectId = TCFields.ProjectId.valueOf(project);
        switch (projectId) {
            case MFE:
            case BTC:
            case EDU:
            case LIT:
                return JavaUiFailCategoriesEdu.determine(step, trace);
            case S21:
                return JavaUiFailCategoriesS21.determine(step, trace);
            default:
                throw new ExternalServicesException("Не реализована поддержка для проекта " + projectId.name());
        }
    }

    public static IFailCategories determineOrNull(final String project, final String trace) {
        final TCFields.ProjectId projectId = TCFields.ProjectId.valueOf(project);
        switch (projectId) {
            case MFE:
            case BTC:
            case EDU:
            case LIT:
                return JavaUiFailCategoriesEdu.determineOrNull("", trace);
            case S21:
                return JavaUiFailCategoriesS21.determineOrNull("", trace);
            default:
                throw new ExternalServicesException("Не реализована поддержка для проекта " + projectId.name());
        }
    }
}
