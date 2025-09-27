package ru.sbt.edu_power.e2e_core.smoke;

import org.junit.Assert;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Класс реализует конструкцию и парсинг шагов проверки роута.
 * Поддерживается два шага - один с передаачей параметров (две строки) и
 * один без параметров (одна строка)
 */
public class RoutFeatureStep implements Comparable<RoutFeatureStep> {
    private static final String STEP_WITH_PARAM = "И проверяет роут \"%s\" с параметрами";
    private static final String SIMPLE_STEP = "И проверяет роут \"%s\"";
    private final String page;
    private final String profile;
    private final String team;
    private final List<String> params;

    public static RoutFeatureStep generateStep(final String page, final Roles role, final String profile) {
        final Map<Roles, String> routes = RoutEvaluator.getRoleToRouteMap(RoutEvaluator.getPageClass(page));
        if (routes.isEmpty()) {
            throw new AutotestError(String.format("Для страницы \"%s\" не объявлены EndPoints", page));
        }
        final String rout = routes.get(role);
        if (rout.contains("%s")) {
            final int paramCount = (rout + "a").split("%s").length - 1;
            final List<String> param = IntStream
                    .range(0, paramCount)
                    .mapToObj(i -> "%s")
                    .collect(Collectors.toList());
            return new RoutFeatureStep(page, param, profile);
        }
        return new RoutFeatureStep(page, new ArrayList<>(), profile);
    }


    public RoutFeatureStep(final String page, final List<String> params, final String profile) {
        this.page = page;
        this.params = params;
        this.profile = profile;
        this.team = RoutEvaluator.getTeamToPage(RoutEvaluator.getPageClass(page));
    }

    public String getTeam() {
        return this.team;
    }

    public String getProfile() {
        return this.profile.equals("") ? "%s" : this.profile;
    }

    // Получение строки шага (одна или две строки)
    public String get() {
        if (params.isEmpty()) {
            return SIMPLE_STEP.replace("%s", page);
        } else {
            return STEP_WITH_PARAM.replace("%s", page) + "\n" + "| " + String.join(" | ", params) + " |";
        }
    }

    // Парсинг строк содержащих шаги. Метод принимает список из одного или
    // двух строк в зависимости от отсутствия или наличия параметров шага
    public static RoutFeatureStep parse(final List<String> rows, final String profile) {
        final String page;
        final List<String> params = new ArrayList<>();
        if (rows.isEmpty()) {
            throw new AutotestError("Нет строк для парсинга");
        } else if (rows.size() == 1) {
            final Pattern simplePattern = Pattern.compile(SIMPLE_STEP.replace("%s", "(.*)"));
            page = simplePattern.matcher(rows.get(0)).replaceAll("$1");
        } else if (rows.size() == 2) {
            final Pattern patternWithParam = Pattern.compile(STEP_WITH_PARAM.replace("%s", "(.*)"));
            page = patternWithParam.matcher(rows.get(0)).replaceAll("$1");
            params.addAll(Stream
                    .of(rows.get(1).split("\\|"))
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .collect(Collectors.toList()));
        } else {
            throw new AutotestError("Передано больше двух строк для парсинга");
        }
        return new RoutFeatureStep(page, params, profile);
    }

    // Метод опрделяет является ли шаг простым (без параметров)
    public static boolean isSimpleStep(final String step) {
        return !step.endsWith("параметрами");
    }

    public boolean isSimpleStep() {
        return params.isEmpty();
    }

    // Метод определяет является ли строка шагом (иначе это параметры)
    public static boolean isStep(final String row) {
        return row.contains("проверяет");
    }

    public String getPage() {
        return page;
    }

    @Override
    public int hashCode() {
        final int s = isSimpleStep() ? 0 : 1;
        return page.hashCode() + s;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!obj.getClass().equals(this.getClass())) {
            return false;
        }
        return ((RoutFeatureStep) obj).getPage().equals(this.getPage())
               && (((RoutFeatureStep) obj).isSimpleStep() == this.isSimpleStep());
    }

    @Override
    public int compareTo(final RoutFeatureStep o) {
        Assert.assertNotNull(o);
        if (this.params.isEmpty() && o.params.isEmpty()) {
            return this.page.compareTo(o.getPage());
        }
        if (this.params.isEmpty()) {
            return -1;
        }
        if (o.params.isEmpty()) {
            return 1;
        }
        if (!this.params.get(0).contains("%s") && o.params.get(0).contains("%s")) {
            return -1;
        }
        if (this.params.get(0).contains("%s") && !o.params.get(0).contains("%s")) {
            return 1;
        }
        return this.page.compareTo(o.getPage());
    }
}
