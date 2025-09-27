package ru.sbt.edu_power.external_services.shared.fail_categories;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor
@Getter
public enum JavaUiFailCategoriesEdu implements IFailCategories {
    PRELOADER("", "Прелоадер на странице не завершился|element ([data-qa=\"preloader\"]) still visible", FailCategoryColor.GOLD.getColor(), FailCategoryShape.CIRCLE),
    LOGIN("авториз", "", FailCategoryColor.BROWN.getColor(), FailCategoryShape.CIRCLE),
    JDBC("", "JDBC Connection", FailCategoryColor.ORANGE.getColor(), FailCategoryShape.SQUARE),
    SERVICE_UNAVAILABLE("", "RetryableException: Connection refused|RetryableException: connect timed out executing", FailCategoryColor.GREEN.getColor(), FailCategoryShape.RHOMBUS),
    PAGE_NOT_FOUND("", "Обнаружен экран 404", FailCategoryColor.PURPLE.getColor(), FailCategoryShape.SQUARE),
    UPS_PAGE("", "Обнаружен экран УПС", FailCategoryColor.YELLOW.getColor(), FailCategoryShape.CIRCLE),
    BACKEND_ERRORS("", "При выполнении операции с бэкенда переданы ошибки", FailCategoryColor.BROWN.getColor(), FailCategoryShape.RHOMBUS),
    API_ERROR("выполняет .* запрос|API", "", FailCategoryColor.YELLOW.getColor(), FailCategoryShape.RHOMBUS),
    FRONTEND_ERRORS("", "В консоли JS обнаружены ошибки", FailCategoryColor.ORANGE.getColor(), FailCategoryShape.CIRCLE),
    INTERRUPTED("", "InterruptedException", FailCategoryColor.ORANGE.getColor(), FailCategoryShape.RHOMBUS),
    ACC_BACK_SERVICE("", "операцию в acc-back-service", FailCategoryColor.PURPLE.getColor(), FailCategoryShape.CIRCLE),
    UNDEFINED("", "", FailCategoryColor.RED.getColor(), FailCategoryShape.CIRCLE),
    GRAPHQL("","Обнаружена graphql ошибка",FailCategoryColor.RED.getColor(), FailCategoryShape.RHOMBUS),
    NAVIGATION_TIMEOUT("","TimeoutError: Navigation timeout of",FailCategoryColor.BROWN.getColor(), FailCategoryShape.SQUARE)
    ;
    private final String step;
    private final String trace;
    private final String color;
    private final FailCategoryShape shape;

    public static JavaUiFailCategoriesEdu determineOrNull(final String name, final String trace) {
        return (JavaUiFailCategoriesEdu) UNDEFINED.determine(JavaUiFailCategoriesEdu.values(),name, trace);
    }

    public static JavaUiFailCategoriesEdu determine(final String name, final String trace) {
        return Optional.ofNullable(determineOrNull(name, trace)).orElse(UNDEFINED);
    }

    @Override
    public String getName() {
        return name();
    }
}
