package ru.sbt.edu_power.e2e_core.smoke;

import com.google.gson.JsonObject;
import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.features_utils.Feature;
import ru.sbtqa.tag.pagefactory.PageManager;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс генерирует наборы фичей для выполнения smart smoke по данным из @EndPoints аннотации в pageObjects
 * Все фичи находятся в директории, которая указана в проперти "smart.smoke.path"
 * Фичи разбиваются сначала по директориям с названием проекта, потом по директориям с названием роли из Roles
 * Для каждой роли генерируется несколько фичей, так, чтобы в каждой фиче было
 * от MIN_STEPS_PER_FEATURE до MAX_STEP_PER_FEATURE шагов.
 * Фичи именуются в виде 00.feature, 01.feature и т.д.
 * Внутри фичей порядок шагов определяется следующим образом: сначала идут все шаги без параметров, потом все шаги
 * с заполненными параметрами, потом шаги с заполнителем вместо параметров
 * При повторной генерации учитываются ранее добавленные данные - названия профилей пользователей в соответсвии с ролью,
 * параметры для роутов
 */
public class SmokeFeaturesGenerator {
    private static final int MIN_STEPS_PER_FEATURE = 4;
    private static final int MAX_STEP_PER_FEATURE = 8;
    private final Map<Roles, List<RoutFeatureStep>> featureStepListByRolesMap = new EnumMap<>(Roles.class);
    private final Map<Roles, String> userProfileByRoleMap = new EnumMap<>(Roles.class);
    private final boolean forProd;
    private final String testPathRoot;
    private final String team;
    private final String authStep = "И авторизуется на сайте под учетной записью \"%s\"";
    private final String authStepS21Student = "И авторизуется под студентом \"%s\"";
    private final String authStepS21Admin = "И авторизуется под учетной записью админки \"%s\"";
    private final String authStepS21Methodology = "И авторизуется под учетной записью методолога \"%s\"";

    public SmokeFeaturesGenerator(final boolean forProd) {
        this.forProd = forProd;
        testPathRoot = forProd ? Props.get("smart.smoke.path.prod") : Props.get("smart.smoke.path");
        team = "";
    }

    public SmokeFeaturesGenerator(final boolean forProd, final String team) {
        this.forProd = forProd;
        this.team = team;
        testPathRoot = forProd ? Props.get("smart.smoke.path.prod") : Props.get("smart.smoke.path");
    }

    private void checkAndGenerateSmokes() {
        final ConsistenceCheck check = new ConsistenceCheck();
        check.matchingRoles();
        check.matchingRoleFunctions();
        check.searchPagesWithoutEndPoints();
        check.matchEndPointsAndParentPageProjects();
        check.matchEndPointsAndParentPageRoles();
        parseFeatures();
        generateStepMap();
    }

    public void generateFeaturesInTeam() {
        checkAndGenerateSmokes();
        deleteTeamStepMap();
        deleteDirectoryInTeam();
        featureStepListByRolesMap.keySet().forEach(this::generateFeaturesByRole);
    }

    /**
     * Проверяет наличие "смоков" с нулевыми параметрами и генерирует новые "смоки", если таковые отсутствуют.
     *
     * @return строку с описанием найденных и сгенерированных "смоков"
     */
    public StringBuilder checkSmokeInNullParams() {
        checkAndGenerateSmokes();
        List<RoutFeatureStep> allStepInNullParams = new ArrayList<>();
        List<RoutFeatureStep> deleteSteps = new ArrayList<>();
        List<String> teamInNullProfileInFeatures = new ArrayList<>();
        featureStepListByRolesMap.values()
                                 .forEach(routFeatureSteps -> routFeatureSteps
                                         .stream()
                                         .filter(feature -> feature.getProfile().contains("%s"))
                                         .forEach(
                                                 step -> {
                                                     teamInNullProfileInFeatures.add(step.getTeam());
                                                     deleteSteps.add(step);
                                                 }));
        featureStepListByRolesMap.values()
                                 .forEach(routFeatureSteps -> routFeatureSteps
                                         .stream()
                                         .filter(feature -> feature.get().contains("%s"))
                                         .forEach(step -> {
                                             allStepInNullParams.add(step);
                                             deleteSteps.add(step);
                                         }));
        deleteSteps.forEach(this::deleteStepMap);
        StringBuilder stringBuilder = new StringBuilder();
        if (!allStepInNullParams.isEmpty() || !teamInNullProfileInFeatures.isEmpty()) {
            stringBuilder = getTextSmokeInNullParams(allStepInNullParams, teamInNullProfileInFeatures);
        } else {
            stringBuilder
                    .append("Ураа!!!!! Все смоки имеют параметры для ")
                    .append(forProd ? "PROD" : "DEV")
                    .append(" контура\n\n");
            new SmokeFeaturesGenerator(forProd).generateFeatures();
        }
        final Path path = Paths.get(testPathRoot);
        removeFile(path.toFile());
        generateFeaturePath();
        featureStepListByRolesMap.keySet().forEach(this::generateFeaturesByRole);
        return stringBuilder;
    }

    private StringBuilder getTextSmokeInNullParams(
            List<RoutFeatureStep> allStepInNullParams,
            List<String> teamInNullProfileInFeatures
    ) {
        StringBuilder stringBuilder = new StringBuilder();
        if (!allStepInNullParams.isEmpty()) {
            final String textNotification = "\nПредставителей команд необходимо завести тестовые данные и выполнить генерацию тестов по командам. \n P.S.\n Новые smoke-тестов необходимо добавлять на основе уже существующей функциональности версии прода!\n\n";
            stringBuilder
                    .append("**При генерации смоков будут созданы следующие пейджи без параметров для ")
                    .append(forProd ? "PROD" : "DEV")
                    .append(" контура**\n\n");
            Arrays.stream(Teams.values()).forEach(teams -> {
                allStepInNullParams
                        .stream()
                        .distinct()
                        .filter(stepInTeam -> stepInTeam.getTeam().endsWith(teams.name()))
                        .forEach(step -> {
                            stringBuilder
                                    .append("```")
                                    .append(step.getTeam())
                                    .append(" : ")
                                    .append(step.getPage())
                                    .append("```")
                                    .append("\n");
                        });
            });
            stringBuilder.append(textNotification);
        }
        if (!teamInNullProfileInFeatures.isEmpty()) {
            stringBuilder
                    .append("**При генерации смоков будут созданы фичи без пользователей для ")
                    .append(forProd ? "PROD" : "DEV")
                    .append(" контура для команд:**\n\n");
            teamInNullProfileInFeatures
                    .stream()
                    .distinct()
                    .forEach(e -> stringBuilder.append("```").append(e).append("```").append("\n"));
        }
        return stringBuilder;
    }

    public void generateFeatures() {
        checkAndGenerateSmokes();
        final Path path = Paths.get(testPathRoot);
        removeFile(path.toFile());
        generateFeaturePath();
        featureStepListByRolesMap.keySet().forEach(this::generateFeaturesByRole);
    }

    // выполняется парсинг существующих фичей для сохранения данных и переиспользования их
    private void parseFeatures() {
        final SmokeFeaturesParser parser = new SmokeFeaturesParser(forProd);
        parser.parse();
        featureStepListByRolesMap.putAll(parser.getFeatureStepListByRolesMap());
        userProfileByRoleMap.putAll(parser.getUserProfileByRoleMap());
    }

    // удаление всех фичей smart smoke
    private void removeFile(final File file) {
        if (!file.isFile()) {
            final File[] files = file.listFiles();
            if (files != null) {
                for (final File file1 : files) {
                    removeFile(file1);
                }
            }
        }
        file.delete();
    }

    // генерация всех features файлов для роли
    private void generateFeaturesByRole(final Roles role) {
        final List<RoutFeatureStep> allSteps = new ArrayList<>(featureStepListByRolesMap.get(role))
                .stream()
                .distinct()
                .collect(
                        Collectors.toList());
        final Map<String, List<RoutFeatureStep>> teamRoutFeatureStep = getTeamRoutFeatureStep(allSteps);
        final Map<String, Map<String, List<RoutFeatureStep>>> teamAndUserRoutFeatureStep = filterUserRoutFeatureStep(
                teamRoutFeatureStep);
        AtomicReference<Integer> countNumber = new AtomicReference<>();
        teamAndUserRoutFeatureStep.forEach((team, featureStepUser) -> {
            countNumber.set(0);
            featureStepUser.forEach((key1, featureStepTeam) -> {
                final int numberOfSteps = calculateStepNumberPerFeature(featureStepTeam.size());
                Collections.sort(featureStepTeam);
                final int numberOfFeatures = (int) Math.ceil(featureStepTeam.size() /
                                                             (double) numberOfSteps);
                generateFeatures(
                        numberOfFeatures,
                        numberOfSteps,
                        featureStepTeam,
                        role,
                        team,
                        countNumber.get()
                );
                countNumber.getAndSet(1 + numberOfFeatures);
            });
        });
    }

    private Map<String, List<RoutFeatureStep>> getTeamRoutFeatureStep(final List<RoutFeatureStep> allSteps) {
        final Map<String, List<RoutFeatureStep>> teamRoutFeatureStep = new HashMap<>();
        for (RoutFeatureStep step : allSteps) {
            if (teamRoutFeatureStep.containsKey(step.getTeam())) {
                teamRoutFeatureStep.get(step.getTeam()).add(step);
            } else {
                teamRoutFeatureStep.put(step.getTeam(), new ArrayList<>(Collections.singleton(step)));
            }
        }
        return teamRoutFeatureStep;
    }

    private Map<String, Map<String, List<RoutFeatureStep>>> filterUserRoutFeatureStep(final Map<String, List<RoutFeatureStep>> steps) {
        final Map<String, Map<String, List<RoutFeatureStep>>> userRoutFeatureStep = new HashMap<>();
        for (String team : steps.keySet()) {
            for (RoutFeatureStep step : steps.get(team)) {
                if (userRoutFeatureStep.containsKey(team)) {
                    if (userRoutFeatureStep.get(team).containsKey(step.getProfile())) {
                        userRoutFeatureStep.get(team).get(step.getProfile()).add(step);
                    } else {

                        userRoutFeatureStep.get(team).put(
                                step.getProfile(), new ArrayList<>(Collections.singleton(step)));

                    }
                } else {
                    final Map<String, List<RoutFeatureStep>> mapProfileAndStep = new HashMap<>();
                    mapProfileAndStep.put(step.getProfile(), new ArrayList<>(Collections.singleton(step)));
                    userRoutFeatureStep.put(
                            team, mapProfileAndStep
                    );

                }
            }
        }
        return userRoutFeatureStep;
    }

    private void generateFeatures(
            final int numberOfFeatures,
            final int numberOfSteps,
            final List<RoutFeatureStep> allSteps,
            final Roles role,
            final String team,
            final int countNumber
    ) {
        for (int i = 0; i < numberOfFeatures; i++) {
            final int maxSteps = Math.min(numberOfSteps, allSteps.size());
            final List<RoutFeatureStep> routFeatureSteps = new ArrayList<>(allSteps.subList(0, maxSteps));
            final int featureNumber = i + countNumber;

            generateFeature(role, routFeatureSteps, String.valueOf(featureNumber), team);
            routFeatureSteps.forEach(routFeatureStep -> {
                featureStepListByRolesMap.get(role).removeIf(step -> step.getPage().equals(routFeatureStep.getPage()));
                allSteps.removeIf(step -> step.getPage().equals(routFeatureStep.getPage()));
            });
        }
    }


    // создание и наполнение файла сценария шагами и всеми необходимыми атрибутами
    private void generateFeature(
            final Roles role,
            final List<RoutFeatureStep> steps,
            final String featureNumber,
            final String team
    ) {
        final String prodTagSuffix = forProd ? "_PROD" : "";
        final String projectTag = "@PROJECT_" + role.getProjectName() + prodTagSuffix;
        final String roleTag = role.getRoleTag() + prodTagSuffix;
        final Path featurePath = Paths.get(
                testPathRoot,
                role.getProjectName(),
                role.getEndPointName(), team,
                featureNumber + team +
                ".feature"
        );
        final Feature feature = new Feature(featurePath);
        feature.addFeatureSection(Feature.Section.HEADER, "#language:ru");
        feature.addFeatureSection(
                Feature.Section.FUNCTIONAL,
                "Функционал: Smart Smoke auto generate for project " + role.getProjectName() + " " + team
        );
        final JsonObject scenario = feature.createScenario();
        feature.addFeatureSection(Feature.Section.SCENARIOS, scenario);
        feature.addScenarioSection(scenario, Feature.ScenarioSection.TAGS, "@" + team);
        feature.addScenarioSection(scenario, Feature.ScenarioSection.TAGS, projectTag);
        feature.addScenarioSection(scenario, Feature.ScenarioSection.TAGS, roleTag);
        final String title = "Сценарий: auto generate scenario for role " +
                             role.getEndPointName() +
                             ". Feature number " +
                             featureNumber;
        feature.addScenarioSection(scenario, Feature.ScenarioSection.SCENARIO_NAME, title);
        // добавляем специфичные шаги авторизации
        final List<String> scenarioSteps = new ArrayList<>();
        specifiedAuthStepsAdd(scenarioSteps, role, steps.get(0).getProfile());
        scenarioSteps.addAll(
                steps.stream()
                     .map(RoutFeatureStep::get)
                     .flatMap(row -> Stream.of(row.split("\\n")))
                     .collect(Collectors.toList())
        );
        scenarioSteps.forEach(s -> feature.addScenarioSection(scenario, Feature.ScenarioSection.STEPS, s));
        feature.saveToDisk();
    }

    private void specifiedAuthStepsAdd(final List<String> rows, final Roles role, final String profile) {
        rows.add("И находится на странице \"Страница авторизации\"");
        String authStepByProject;

        if (role.getProjectName().equals(Projects.S21.name())) {
            switch (role) {
                case STUDENT_S21:
                    authStepByProject = getAuthStepByProject(role, profile, this.authStepS21Student);
                    break;
                case METHODOLOGY_S21:
                    authStepByProject = getAuthStepByProject(role, profile, this.authStepS21Methodology);
                    break;
                default:
                    authStepByProject = getAuthStepByProject(role, profile, this.authStepS21Admin);
            }
        } else {
            authStepByProject = getAuthStepByProject(role, profile, this.authStep);
        }
        rows.add("И обновляет страницу с контролем загрузки");
        rows.add(authStepByProject);
    }

    // создание дерева каталогов для проектов и ролей
    private void generateFeaturePath() {
        featureStepListByRolesMap
                .keySet()
                .forEach(this::createDirectory);
    }

    private String getAuthStepByProject(final Roles role, final String profile, final String authStep) {
        return userProfileByRoleMap.containsKey(role) ?
                authStep.replace("%s", profile) : authStep;
    }

    private void deleteDirectoryInTeam() {
        featureStepListByRolesMap
                .keySet()
                .forEach(e -> removeFile(getFileInTeam(e).toFile()));
    }

    private Path getFileInTeam(final Roles role) {
        return Paths.get(testPathRoot,
                role.getProjectName(),
                role.getEndPointName(), team
        );
    }

    private void createDirectory(final Roles role) {
        try {
            Files.createDirectories(Paths.get(
                    testPathRoot,
                    role.getProjectName(),
                    role.getEndPointName()
            ));
        } catch (final IOException e) {
            throw new AutotestError(e);
        }
    }

    // рассчёт количества шагов в одной фиче для равномерного распределения по сценариям (что-бы в последнем сценарии
    // не оставалось 1-2 шага при том что в прочих сценариях по 10 шагов)
    private int calculateStepNumberPerFeature(final int allStepNumber) {
        int minDeviation = 10;
        int stepPerNumber = 0;
        for (int i = MIN_STEPS_PER_FEATURE; i <= MAX_STEP_PER_FEATURE; i++) {
            final int features = (int) Math.ceil(allStepNumber / (double) i);
            final int deviation = features * i - allStepNumber;
            if (deviation < minDeviation) {
                minDeviation = deviation;
                stepPerNumber = i;
            }
        }
        return stepPerNumber;
    }

    // получение из аннотации @EndPoints роутов и запись их в мапу по соответствующей роли
    // Если в мапе уже существуют данные, они будут пропущены
    private void generateStepMap() {
        final Map<Roles, List<RoutFeatureStep>> clearedRolesToStepsMap = new EnumMap<>(Roles.class);
        PageManager.getPageRepository().keySet()
                   .stream()
                   .filter(page -> page.isAnnotationPresent(EndPoints.class))
                   .forEach(pageClass -> {
                       final Set<Roles> roles = RoutEvaluator.getRoleToRouteMap(pageClass).keySet();
                       roles.forEach(role -> {
                           if (forProd && !role.isReadyForProd()) {
                               return;
                           }
                           if (!clearedRolesToStepsMap.containsKey(role)) {
                               clearedRolesToStepsMap.put(role, new ArrayList<>());
                           }
                           clearedRolesToStepsMap
                                   .get(role)
                                   .add(RoutFeatureStep.generateStep(pageClass.getSimpleName(), role, ""));
                       });
                   });
        removeNotUsedEndPoints(clearedRolesToStepsMap);
        updateStepMap(clearedRolesToStepsMap);
    }

    // если в текущем состоянии эндпоинт пропал из pageObject - удаляем шаг из featureStepListByRolesMap
    // так же удаляем шаг если он изменил структуру (появились или исчезли параметры для эндпоинта)
    private void removeNotUsedEndPoints(final Map<Roles, List<RoutFeatureStep>> clearedRolesToStepsMap) {
        clearedRolesToStepsMap.forEach((role, stepList) -> {
                    if (featureStepListByRolesMap.containsKey(role)) {
                        new ArrayList<>(featureStepListByRolesMap.get(role)).forEach(step -> {
                            if (!stepList.contains(step)) {
                                featureStepListByRolesMap.get(role).remove(step);
                            }
                        });
                    }
                }
        );
    }

    private void deleteStepMap(final RoutFeatureStep step) {
        featureStepListByRolesMap.forEach((role, routFeatureSteps) -> {
            routFeatureSteps.removeIf(s -> s.equals(step));
        });
    }

    private void deleteTeamStepMap() {
        checkTeam();
        featureStepListByRolesMap.forEach((role, routFeatureSteps) -> {
            routFeatureSteps.removeIf(step -> !step.getTeam().equals(team));
        });
    }

    private void checkTeam() {
        Assert.assertTrue(
                "Имя команды не соответствует перечислению в классе Teams",
                Arrays.stream(Teams.values()).anyMatch(e -> e.name().equals(team))
        );
    }

    private void removeFileInTeam() {

    }

    // добавляем к распарсенному списку шагов те, что получены из pageObject
    private void updateStepMap(final Map<Roles, List<RoutFeatureStep>> clearedRolesToStepsMap) {
        clearedRolesToStepsMap.forEach((role, stepList) -> {
            if (featureStepListByRolesMap.containsKey(role)) {
                stepList.forEach(step -> {
                    if (!featureStepListByRolesMap.get(role).contains(step)) {
                        featureStepListByRolesMap.get(role).add(step);
                    }
                });
            } else {
                featureStepListByRolesMap.put(role, stepList);
            }
        });
    }

}
