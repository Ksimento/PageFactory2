package ru.sbt.edu_power.e2e_core.smoke;

import lombok.SneakyThrows;
import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.allure.AllureUtils;
import ru.sbt.edu_power.e2e_core.data.UrlProcessing;
import ru.sbt.edu_power.e2e_core.error_processing.NotCriticalErrorAccumulator;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.PageManager;
import ru.sbtqa.tag.pagefactory.context.PageContext;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.reflections.Reflections.log;

/**
 * Класс реализует получение роута из pageObject и применение к нему
 * замещающих параметров (обозначенных в роуте как %s)
 */
public class RoutEvaluator {

    /**
     * Выполняет получение роута, замещение параметров при необходимости и
     * переход по этому роуту с проверкой отсутствия ошибок
     *
     * @param className   класс страницы pageObject из которой нужно
     *                    получить роут. Класс страницы проставляется в шаг
     *                    автоматически при генерации сценариев
     * @param replacement список замещений (параметров) для роута. Список
     *                    замещений проставляется вручную в зависимости от
     *                    используемой учётки и конкретных
     *                    предустановленных данных
     */
    @SneakyThrows
    public static void evaluate(final String className, final List<String> replacement) {
        final List<String> roleTagList = (List<String>) Environment.getScenario().getSourceTagNames();
        roleTagList.removeIf(tag -> !tag.startsWith("@R_") && !tag.startsWith("@LTR_"));
        if (roleTagList.isEmpty()) {
            throw new AutotestError("В сценарии отсутствует тег роли, который должен начинаться на @R_");
        } else if (roleTagList.size() > 1) {
            throw new AutotestError("В сценарии обнаружено больше одного тега роли: " + String.join(", ", roleTagList));
        }
        final Roles role = Roles.getRoleByTagName(roleTagList.get(0).replace("_PROD", ""));
        final Class<? extends Page> pageClass = getPageClass(className);
        String route = getRoleToRouteMap(getPageClass(className)).get(role);
        if (route == null) {
            NotCriticalErrorAccumulator.setNotCriticalError(
                    String.format("Для класса '%s' указан параметр ignored, шаг необходимо исключить из сценария", className)
            );
            NotCriticalErrorAccumulator.setStepBroken();
            return;
        }
        final int placeHolderNumber = (route + "a").split("%s").length - 1;
        Assert.assertEquals(
                "Количество плейсхолдеров в параметрах роута не соответствует количеству замещающих параметров",
                placeHolderNumber,
                replacement.size()
        );
        for (final String replaceLine : replacement) {
            if ("%s".equals(replaceLine)) {
                NotCriticalErrorAccumulator.setNotCriticalError("Не указано замещение для %s");
            }
            route = route.replaceFirst("%s", replaceLine);
        }
        NotCriticalErrorAccumulator.setStepBroken();
        UrlProcessing.goToUrl(route);
        final Page page = pageClass.getConstructor().newInstance();
        PageContext.setCurrentPage(page);
        AllureUtils.attachScreenShotToAllure("Снимок страницы после перехода по url");
    }

    // Метод возвращает мапу из соответствия enum Roles к роуту, которые
    // прописаны в используемом pageObject в аннотации @EndPoints
    @SneakyThrows
    public static Map<Roles, String> getRoleToRouteMap(final Class<? extends Page> pageClass) {
        final Map<Roles, String> roleToRouteMap = new EnumMap<>(Roles.class);
        final EndPoints endPoints = pageClass.getAnnotation(EndPoints.class);
        final Method[] methods = endPoints.getClass().getDeclaredMethods();
        for (final Method method : methods) {
            if (Boolean.class.isAssignableFrom(method.getReturnType())) {
                final boolean ignoredState = (boolean) method.invoke(endPoints);
                if (ignoredState) {
                    return new EnumMap<>(Roles.class);
                }
            }
            if (!String.class.isAssignableFrom(method.getReturnType())
                || "toString".equals(method.getName())) {
                continue;
            }
            final String endPoint = (String) method.invoke(endPoints);
            if (!endPoint.isEmpty()) {
                roleToRouteMap.put(Roles.getRoleByEndPointName(method.getName()), endPoint);
            }
        }
        return roleToRouteMap;
    }
    @SneakyThrows
    public static String getTeamToPage(final Class<? extends Page> pageClass) {
        try {
            return Arrays.stream(pageClass.getAnnotation(Team.class).value()).findFirst().get().name();
        }
        catch (NullPointerException e){
            log.info(String.format("Требуется указать команду для страницы \"%s\"",pageClass.getSimpleName()));
            return "T_undefined";
        }
    }

    // Метод возвращает класс pageObject по его simple name из сценария
    static Class<? extends Page> getPageClass(final String className) {
        final Map<Class<? extends Page>, Map<Field, String>> pageRepository = PageManager.getPageRepository();
        final List<Class<? extends Page>> pageList = pageRepository
                .keySet()
                .stream()
                .filter(page -> page.getSimpleName().equals(className))
                .collect(Collectors.toList());
        if (pageList.isEmpty()) {
            throw new AutotestError("Не найдена страница с классом " + className);
        }
        if (pageList.size() > 1) {
            throw new AutotestError("Найдено больше одной страницы с названием " + className);
        }
        return pageList.get(0);
    }

}
