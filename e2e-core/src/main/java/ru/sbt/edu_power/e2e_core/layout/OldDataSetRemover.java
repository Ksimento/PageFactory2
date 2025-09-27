package ru.sbt.edu_power.e2e_core.layout;

import org.apache.commons.io.FileUtils;
import ru.sbt.edu_power.e2e_core.features_utils.FeaturesParser;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.sbt.edu_power.e2e_core.layout.LayoutUtils.badSymbolReplace;

/**
 Класс реализует сканирование фичей на предмет использования шага тестирования верстки
 с последующим удалением файлов дата-сетов не используемых в фичах
 */

public class OldDataSetRemover {
    // Путь до директории с дата-сетами
    private final String dataPath = System.getProperty("layout.dataset.write.storage");
    // краткий путь до файла -> файл
    private final Map<String, File> dataFiles = new HashMap<>();
    // Путь из строки с фичей
    private Path pathFeatures;

    public void execute() {
        updateMapData();
        new FeaturesParser().getFeaturesList()
                            .stream()
                            .map(File::toPath)
                            .map(this::readAllLines)
                            .flatMap(List::stream)
                            .filter(s -> s.contains("И проверяет параметры верстки"))
                            .forEach(this::removeUsedPath);


        dataFiles.values().forEach(File::delete);
    }

     //Метод получения строк из фич
    private List<String> readAllLines(final Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new AutotestError(e);
        }
    }

    /**
     Метод составляет путь до дата-сета исходя из строки фичи
     с последующим удалением из Map
     */
    private void removeUsedPath(final String line) {
        final String[] splitLine = line.split("Путь")[1].split("\"");
        final List<String> detectPath = Stream
                .of(splitLine[1].split(";"))
                .map(String::trim)
                .map(LayoutUtils::badSymbolReplace)
                .collect(Collectors.toList());
        pathFeatures = Paths.get(String.join("/", detectPath), badSymbolReplace(splitLine[3]));
        Arrays.stream(DimensionEnum.values())
              .map(this::getPath)
              .map(Path::toString)
              .forEach(dataFiles::remove);
    }

    //Получение пути до файла учитывая разрешения экрана
    private Path getPath(final DimensionEnum dimension) {
        return Paths.get(
                dimension.name(),
                pathFeatures +
                ".json"
        );
    }

    //Добавляем данные в Map
    private void updateMapData() {
        File file;
        final Iterator<File> iterator = FileUtils.iterateFiles(
                Paths.get(dataPath).toFile(),
                new String[]{"json"},
                true
        );
        while (iterator.hasNext()) {
            file = iterator.next();
            dataFiles.put(file.getPath().split("layouts\\\\")[1].split("\\\\", 2)[1], file);

        }
    }
}
