package ru.sbt.edu_power.e2e_core.features_utils;

import org.apache.commons.io.FileUtils;
import ru.sbt.edu_power.external_services.validator.Validator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FeaturesParser {
    private final Path featuresPath;
    private final List<File> files;
    private final List<String> tags = new ArrayList<>();

    public FeaturesParser() {
        featuresPath = Paths.get("src/test/resources/features/UI");
        files = getFeaturesList();
    }

    public FeaturesParser(final Path path) {
        featuresPath = path;
        files = getFeaturesList();
    }

    //    Метод выполняет фильтрацию списка, удаляя из него повторяющиеся значения
    public static <T> Predicate<T> distinctByKey(
            final Function<? super T, ?> keyExtractor
    ) {
        final Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    //    Возвращает список всех тегов
    public List<String> asTagList() {
        if (tags.isEmpty()) {
            collectTags();
        }
        return tags;
    }

    //    Возвращает список тегов состоящий из уникальных значений (дубликаты удалены)
    public List<String> asUniqueTagList() {
        return getUniqueTags(asTagList());
    }

    //    Возвращает список дублирующихся тегов
    public List<String> asDuplicationTagList() {
        final List<String> unique = asUniqueTagList();
        final List<String> tagsClone = tags.stream()
                                           .map(tag -> tag.replace("@", ""))
                                           .collect(Collectors.toList());
        for (final String tag : unique) {
            tagsClone.remove(tag);
        }
        return getUniqueTags(tagsClone);
    }

    //    Возвращает список названий найденных фич
    public List<String> asFileNameList() {
        return files
                .stream()
                .map(File::getName)
                .collect(Collectors.toList());
    }

    //    Возвращает список файлов найденных фичей
    public List<File> asFileList() {
        return files;
    }

    //    Фильтрует имеющиеся теги по значению
    //    Понимает операторы OR и AND а так же маску со звёздочкой
    public FeaturesParser filterTagsByMask(final Operator operator, final String... tagMaskList) {
        asTagList().removeIf(tag -> {
            for (final String tagMask : tagMaskList) {
                if (operator == Operator.OR) {
                    if (Validator.matchValues(tag, tagMask)) {
                        return false;
                    }
                } else {
                    if (!Validator.matchValues(tag, tagMask)) {
                        return true;
                    }
                }
            }
            return operator == Operator.OR;
        });
        return this;
    }

    //    Фильтрует текущий список файлов фич по наличию слов в сценарии.
    //    Понимает операторы OR и AND а так же маску со звёздочкой
    public FeaturesParser filterFeaturesByContent(final Operator operator, final String... searchedStrings) {
        final List<File> files = this.files
                .stream()
                .filter(file -> {
                    final List<String> lines = readFile(file);
                    return filterData(lines, operator, searchedStrings);
                })
                .collect(Collectors.toList());
        this.files.clear();
        this.files.addAll(files);
        return this;
    }

    //    Фильтрует текущий список файлов фич по наличию тегов.
    //    Понимает операторы OR и AND а так же маску со звёздочкой
    public FeaturesParser filterFeaturesByTag(final Operator operator, final String... tags) {
        final List<File> files = this.files
                .stream()
                .filter(file -> {
                    final List<String> fileTags = getTagsFromFile(file);
                    return filterData(fileTags, operator, tags);
                })
                .collect(Collectors.toList());
        this.files.clear();
        this.files.addAll(files);
        return this;
    }

    //    Выбирает из всех найденных файлов все имеющиеся теги
    public FeaturesParser collectTags() {
        final List<String> tags = this.files
                .stream()
                .flatMap(file -> getTagsFromFile(file).stream())
                .collect(Collectors.toList());
        this.tags.addAll(tags);
        return this;
    }

    //    Возвращат список только из уникальных значений
    private List<String> getUniqueTags(final List<String> tags) {
        return tags
                .stream()
                .filter(distinctByKey(t -> t))
                .map(tag -> tag.replace("@", ""))
                .collect(Collectors.toList());
    }

    //    Определяет есть ли в списке строк искомые данные.
    //    Понимает логические операторы OR и AND а так же маску со звёздочкой
    private boolean filterData(
            final List<String> data,
            final Operator operator,
            final String... searchedStrings
    ) {
        for (final String search : searchedStrings) {
            switch (operator) {
                case OR:
                    if (Validator.matchValueInList(data, search)) {
                        return true;
                    }
                    break;
                case AND:
                    if (!Validator.matchValueInList(data, search)) {
                        return false;
                    }
                    break;
                case NOT:
                    if (Validator.matchValueInList(data, search)) {
                        return false;
                    }
                    break;
                default:
                    throw new FeaturesParserException("Неизвестный оператор: " + operator.name());
            }
        }
        return operator == Operator.AND || operator == Operator.NOT;
    }

    //    Возвращает список строк из файла фичи
    private List<String> readFile(final File file) {
        try {
            return FileUtils
                    .readLines(file, StandardCharsets.UTF_8)
                    .stream()
                    .map(String::trim)
                    .collect(Collectors.toList());
        } catch (final IOException e) {
            throw new FeaturesParserException(e);
        }
    }

    //    Возвращает список всех тегов
    public List<String> getTagsFromFile(final File file) {
        final List<String> lines = readFile(file);
        return lines.stream()
                    .map(String::trim)
                    .filter(line -> line.startsWith("@"))
                    .flatMap(line -> Stream.of(line.split("\\s")))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
    }

    /**
     * Записывает в фичу новую строку с тегом
     *
     * @param tagName       Название нового тега со знаком @ вначале
     * @param beforeTagName Название тега, после которого нужно вставить строку с новым тегом
     * @param file          файл фичи
     */
    public void writeTag(final String tagName, final String beforeTagName, final File file) {
        try {
            final Iterator<String> iterator = FileUtils.lineIterator(file, StandardCharsets.UTF_8.name());
            final List<String> newFeatureContent = new ArrayList<>();
            while (iterator.hasNext()) {
                final String line = iterator.next();
                newFeatureContent.add(line);
                if (beforeTagName.equals(line.trim())) {
                    // перед тегом добавляем столько же пробелов сколько перед предыдущим тегом
                    newFeatureContent.add(line.split("\\S", 2)[0] + tagName);
                }
            }
            FileUtils.write(file, String.join("\n", newFeatureContent), StandardCharsets.UTF_8, false);
        } catch (final IOException e) {
            throw new FeaturesParserException(e);
        }
    }

    /**
     * Удаляет строку с указанным тегом полностью
     *
     * @param isContains true если требуется удалить теги по вхождению
     * @param file       файл фичи
     * @param tagList    список тегов, которые нужно удалить. Название каждого тега должно начинаться с @
     */
    public void removeTags(final boolean isContains, final File file, final String... tagList) {
        final List<String> featureTagList = getTagsFromFile(file);
        final List<String> tagsToDelete = Stream.of(tagList)
                                                .filter(tag -> featureTagList
                                                        .stream()
                                                        .anyMatch(featureTag -> featureTag.contains(tag)))
                                                .collect(Collectors.toList());
        if (tagsToDelete.isEmpty()) {
            return;
        }
        try {
            final Iterator<String> iterator = FileUtils.lineIterator(file, StandardCharsets.UTF_8.name());
            final List<String> newFeatureContent = new ArrayList<>();
            while (iterator.hasNext()) {
                final String line = iterator.next();
                boolean remove = false;
                for (final String tagName : tagsToDelete) {
                    if (isContains) {
                        if (line.contains(tagName)) {
                            remove = true;
                        }
                    } else {
                        if (tagName.equals(line.trim())) {
                            remove = true;
                        }
                    }
                }
                if (!remove) {
                    newFeatureContent.add(line);
                }
            }
            FileUtils.write(file, String.join("\n", newFeatureContent), StandardCharsets.UTF_8, false);
        } catch (final IOException e) {
            throw new FeaturesParserException(e);
        }
    }

    //    Возвращает список всех имеющихся фичей в проекте
    public List<File> getFeaturesList() {
        final List<File> files = new ArrayList<>();
        final Iterator<File> iterator = FileUtils.iterateFiles(
                featuresPath.toFile(),
                new String[]{"feature"},
                true
        );
        while (iterator.hasNext()) {
            files.add(iterator.next());
        }
        return files;
    }

    public enum Operator {
        OR,
        AND,
        NOT
    }

}
