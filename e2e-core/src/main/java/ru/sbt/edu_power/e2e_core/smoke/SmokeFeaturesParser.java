package ru.sbt.edu_power.e2e_core.smoke;

import lombok.extern.slf4j.Slf4j;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Класс выполняет парсинг существующих файлов smart smoke перед их повторной генерацией
 * Это позволяет сохранить ранее добавленные данные (параметры замены для роутов и названия профилей пользователей)
 */
@Slf4j
public class SmokeFeaturesParser {
    private final Map<Roles, List<RoutFeatureStep>> featureStepListByRolesMap = new EnumMap<>(Roles.class);
    private final Map<Roles, String> userProfileByRoleMap = new EnumMap<>(Roles.class);
    private final String testPathRoot;

    public SmokeFeaturesParser(final boolean forProd) {
        testPathRoot = forProd ? Props.get("smart.smoke.path.prod") : Props.get("smart.smoke.path");
    }

    public void parse() {
        final File file = new File(testPathRoot);
        parseSpider(file);
    }

    public Map<Roles, List<RoutFeatureStep>> getFeatureStepListByRolesMap() {
        return featureStepListByRolesMap;
    }

    public Map<Roles, String> getUserProfileByRoleMap() {
        return userProfileByRoleMap;
    }

//    Метод рекурсивно проходит по директориям с фичами и парсит все фичи
    private void parseSpider(final File file) {
        if (file.isFile()) {
            parseFeature(file.toPath());
        } else {
            final File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (final File innerFile : files) {
                if (innerFile.isFile() && ".DS_Store".equals(innerFile.getName())) {
                    continue;
                }
                parseSpider(innerFile);
            }
        }
    }

    /**
     * Метод парсит фичу:
     *  - определяет роль из списка Roles
     *  - определяет имя профиля под которым выполняется фича
     *  - определяет простые шаги и шаги с параметрами, создавая их них экземпляры FeatureStep
     * @param feature путь до файла фичи
     */
    private void parseFeature(final Path feature) {
        final List<String> rows;
        try {
            rows = Files.readAllLines(feature);
        } catch (final IOException e) {
            log.error("Ошибка при парсинге файла {}", feature);
            throw new AutotestError(e);
        }
        final AtomicReference<Roles> role = new AtomicReference<>(null);
        final AtomicReference<String> profile = new AtomicReference<>(null);
        final AtomicReference<String> allProfile = new AtomicReference<>(null);
        final Iterator<String> iterator = rows.iterator();
        while (iterator.hasNext()) {
            final String row = iterator.next();
            if (role.get() == null && isRoleTag(row)) {
                role.set(getRoleFromTag(row));
                if (!featureStepListByRolesMap.containsKey(role.get())) {
                    featureStepListByRolesMap.put(role.get(), new ArrayList<>());
                }
            }
            if (profile.get() == null && isAuthStep(row)) {
                final String authStepProfile = getProfileName(row);
                allProfile.set(authStepProfile);
                if ("%s".equals(authStepProfile)) {
                    continue;
                }
                profile.set(authStepProfile);
                if (!userProfileByRoleMap.containsKey(role.get())) {
                    userProfileByRoleMap.put(role.get(), profile.get());
                }
            }
            if (RoutFeatureStep.isStep(row)) {
                final List<String> stepLines = new ArrayList<>();
                stepLines.add(row.trim());
                if (!RoutFeatureStep.isSimpleStep(row)) {
                    if (iterator.hasNext()) {
                        final String paramLine = iterator.next();
                        if (RoutFeatureStep.isStep(paramLine)) {
                            throw new AutotestError(String.format("В строке '%d' ожидаются параметры, но их там нет", rows.indexOf(row) + 2 ));
                        }
                        stepLines.add(paramLine);
                    } else {
                        throw new AutotestError(String.format("После строки '%d' ожидается строка с параметрами, но она отсутствует", rows.indexOf(row) + 1));
                    }
                }
                featureStepListByRolesMap.get(role.get()).add(RoutFeatureStep.parse(stepLines,allProfile.get()));
            }
        }
    }

    private boolean isAuthStep(final String row) {
        return row.contains("авторизуется");
    }

    private boolean isRoleTag(final String row) {
        return row.contains("@R_");
    }

    private Roles getRoleFromTag(final String row) {
        return Roles.getRoleByTagName(row.trim().replace("_PROD", ""));
    }

    private String getProfileName(final String step) {
        return step.split("\"")[1];
    }
}
