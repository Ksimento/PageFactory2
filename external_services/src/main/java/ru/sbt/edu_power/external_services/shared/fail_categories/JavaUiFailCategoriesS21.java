package ru.sbt.edu_power.external_services.shared.fail_categories;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor
@Getter
public enum JavaUiFailCategoriesS21 implements IFailCategories {
    PRELOADER("", "Прелоадер на странице не завершился", FailCategoryColor.GOLD.getColor(), FailCategoryShape.CIRCLE),
    LOGIN("авториз", "", FailCategoryColor.BROWN.getColor(), FailCategoryShape.RHOMBUS),
    JDBC("", "JDBC Connection", FailCategoryColor.ORANGE.getColor(), FailCategoryShape.SQUARE),
    SERVICE_UNAVAILABLE("", "RetryableException: Connection refused|RetryableException: connect timed out executing", FailCategoryColor.GREEN.getColor(), FailCategoryShape.RHOMBUS),
    PAGE_NOT_FOUND("", "Обнаружен экран 404", FailCategoryColor.PURPLE.getColor(), FailCategoryShape.SQUARE),
    UPS_PAGE("", "Обнаружен экран УПС", FailCategoryColor.YELLOW.getColor(), FailCategoryShape.CIRCLE),
    BACKEND_ERRORS("", "При выполнении операции с бэкенда переданы ошибки", FailCategoryColor.BROWN.getColor(), FailCategoryShape.RHOMBUS),
    APPLICANT_ERRORS("", "Студент с логином .* не создался|Студент с логином .* не разблокирован|Студент .* не перевелся в класс .* за 6 минут", FailCategoryColor.BLUE.getColor(), FailCategoryShape.RHOMBUS),
    EMAIL_ERRORS("", "Нет писем в почтовом ящике по заданным параметрам|503 Service Unavailable: \"no healthy upstream\"", FailCategoryColor.BLUE.getColor(), FailCategoryShape.CIRCLE),
    API_ERROR("выполняет .* запрос|API", "", FailCategoryColor.YELLOW.getColor(), FailCategoryShape.CIRCLE),
    GITLAB("", "GitlabFeignException: 404", FailCategoryColor.YELLOW.getColor(), FailCategoryShape.RHOMBUS),
    GITLAB_USER_BLOCKED("", "GITLAB_USER_BLOCKED", FailCategoryColor.YELLOW.getColor(), FailCategoryShape.SQUARE),
    GITLAB_500_ERRORS("", "Элемент \"Wait until the code review is completed\" не появился" +
            "Элемент \"Project failed\" не появился|" +
            "Элемент \"Project is completed successfully\" не появился|" +
            "Элемент \"Sign up for a Peer Review\" не появился|" +
            "Пайплайн не успел завершиться expected:<[success]> but was:<[failed]>|" +
            "Элемент \"Процент выполнения проекта\" не появился|" +
            "Джоба не успела завершиться с нужным статусом expected:<[success]> but was:<[failed]>" +
            "Элемент \"Список Auto проверок->1->Статус .*\" не появился",
            FailCategoryColor.GOLD.getColor(), FailCategoryShape.SQUARE),
    GITLAB_NOT_FORKING_ERRORS("", "Не удалось создать проект в гитлаб", FailCategoryColor.GOLD.getColor(), FailCategoryShape.RHOMBUS),
    FRONTEND_ERRORS("", "В консоли JS обнаружены ошибки", FailCategoryColor.RED.getColor(), FailCategoryShape.CIRCLE),
    INTERRUPTED("", "InterruptedException", FailCategoryColor.ORANGE.getColor(), FailCategoryShape.RHOMBUS),
    PGADMIN_ERRORS("запускает postgres|закрывает postgres|в БД", "", FailCategoryColor.BLUE.getColor(), FailCategoryShape.SQUARE),
    UNDEFINED("", "", FailCategoryColor.RED.getColor(), FailCategoryShape.CIRCLE),
    ;
    private final String step;
    private final String trace;
    private final String color;
    private final FailCategoryShape shape;

    public static JavaUiFailCategoriesS21 determineOrNull(final String name, final String trace) {
        return (JavaUiFailCategoriesS21) UNDEFINED.determine(JavaUiFailCategoriesS21.values(), name, trace);
    }

    public static JavaUiFailCategoriesS21 determine(final String name, final String trace) {
        return Optional.ofNullable(determineOrNull(name, trace)).orElse(UNDEFINED);
    }

    @Override
    public String getName() {
        return name();
    }
}
