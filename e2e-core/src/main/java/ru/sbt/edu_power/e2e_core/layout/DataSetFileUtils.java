package ru.sbt.edu_power.e2e_core.layout;

import ru.sbt.edu_power.e2e_core.data.CheckPathRules;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

class DataSetFileUtils {
    private static final String DATA_SET_PATH_READ = System.getProperty("layout.dataset.read.storage");
    private static final String DATA_SET_PATH_WRITE = System.getProperty("layout.dataset.write.storage");
    private static final String SEPARATOR = File.separator;
    private static final String JSON = ".json";
    private static final String BROWSER = System.getProperty("webdriver.browser.name");

    /**
     * Метод выполняет запись датасета в папку data/layouts
     *
     * @param dimension  Разрешение экрана
     * @param data       Данны для записи
     * @param screenPath Путь до экрана, пееречисляется через точку с запятой
     * @param dataSetId  Название сценария
     */
    static void dataSetWrite(
            final DimensionEnum dimension,
            final String data,
            final String screenPath,
            final String dataSetId
    ) throws IOException {
        final Path dimensionPath = dimensionPathCreate(dimension);
        final Path pathToDataSet = screenPathCreate(dimensionPath, screenPath);
        final Path fileToWrite = Paths.get(pathToDataSet + SEPARATOR + dataSetId + JSON);
        CheckPathRules.checkPath(fileToWrite.toString());
        Files.deleteIfExists(fileToWrite);
        Files.createFile(fileToWrite);
        Files.write(fileToWrite, data.getBytes());
    }

    /**
     * Метод выполняет чтение из JSON файла в стрингу
     *
     * @param dimension  Разрешение экрана
     * @param screenPath Путь до экрана, пееречисляется через точку с запятой
     * @param dataSetId  Название сценария
     * @return Строка с данными
     */
    static String dataSetRead(
            final DimensionEnum dimension,
            final String screenPath,
            final String dataSetId
    ) throws IOException {
        final Path dataSetPathToRead = dataSetPathToRead(dimension, screenPath, dataSetId);
        CheckPathRules.checkPath(dataSetPathToRead.toString());
        if (Files.notExists(dataSetPathToRead)) {
            throw new AutotestError(String.format(
                    "Файл с данными не создан:\nФормат: \"%s\"\nПуть до экрана: \"%s\"\nID сценария: \"%s\"",
                    dimension.getDimensionName(),
                    screenPath,
                    dataSetId
            ));
        }
        final byte[] data = Files.readAllBytes(dataSetPathToRead);
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Метод проверяет наличие датасета
     *
     * @param dimension  Разрешение экрана
     * @param screenPath Путь до экрана, пееречисляется через точку с запятой
     * @param dataSetId  Название сценария
     */
    static boolean isDataSetExists(
            final DimensionEnum dimension,
            final String screenPath,
            final String dataSetId
    ) {
        return Files.exists(dataSetPathToRead(dimension, screenPath, dataSetId));
    }

    /**
     * Метод генерирует путь до папки с нужным разрешением экрана. Если папки нет, она будет создана
     *
     * @param dimension Разрешение экрана
     * @return путь до директории с нужным разрешением экрана (по имени пресета разрешения)
     */
    private static Path dimensionPathCreate(final DimensionEnum dimension) throws IOException {
        final Path dimensionPath = Paths.get(
                DATA_SET_PATH_WRITE +
                SEPARATOR +
                BROWSER +
                SEPARATOR +
                dimension.name()
        );
        if (Files.notExists(dimensionPath)) {
            Files.createDirectories(dimensionPath);
        }
        return dimensionPath;
    }

    /**
     * Метод генерирует путь до папки сценария. Если нужных директорий нет, они будут созданы
     *
     * @param dimensionPath Разрешение экрана
     * @param screenPath    Перечисление структуры экранов до места проведения тестового сценарий
     * @return путь до директории сценария
     */
    private static Path screenPathCreate(final Path dimensionPath, final String screenPath) throws IOException {
        final Path path = Paths.get(dimensionPath +
                                    SEPARATOR +
                                    convertToPath(screenPath));
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    /**
     * Метод генерирует путь до сценария
     *
     * @param dimension  Разрешение экрана
     * @param screenPath Перечисление структуры экранов до места проведения тестового сценарий
     * @param dataSetId  Название сценария
     * @return Путь до файла сценария
     */
    private static Path dataSetPathToRead(
            final DimensionEnum dimension,
            final String screenPath,
            final String dataSetId
    ) {
        return Paths.get(DATA_SET_PATH_READ +
                         SEPARATOR +
                         BROWSER +
                         SEPARATOR +
                         dimension.name() +
                         SEPARATOR +
                         convertToPath(screenPath) +
                         SEPARATOR +
                         dataSetId +
                         ".json");

    }

    /**
     * Метод конвертирует перечень через точку с запятой в путь для ОС
     *
     * @param semicolonList перечень через точку с запятой
     * @return путь до директории
     */
    private static String convertToPath(final String semicolonList) {
        return Arrays
                .stream(semicolonList.split(";"))
                .map(String::trim)
                .collect(Collectors.joining(SEPARATOR));
    }
}
