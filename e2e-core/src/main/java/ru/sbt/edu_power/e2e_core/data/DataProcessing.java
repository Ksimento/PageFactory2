package ru.sbt.edu_power.e2e_core.data;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import io.qameta.allure.Allure;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.allure.AllureUtils;
import ru.sbt.edu_power.e2e_core.error_processing.ErrorCollector;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbtqa.tag.datajack.Stash;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class DataProcessing {
    /**
     * Метод декодирует данные через управляющие параметры.
     * method#argument1#argument2
     * <p>
     * Допустимые методы:
     * generate генерирует строку из N знаков в зависимости от аргумента. Если к аргументу добавить &prefix
     * то этот префикс будет добавлен перед сгенерированной строкой;
     * stash забирает из стэша значение по имени аргумента. Если к аргументу добавить &prefix
     * то этот префикс будет добавлен перед сгенерированной строкой;
     * generate-stash генерирует строку из N знаков в зависимости от аргумента и сохраняет её в стэш
     * с ключом переданным в predefined секции
     * maven получает значение из строки запуска по названию параметра
     * <p>
     * Допустимые аргументы метода generate:
     * wordN (строка из N букв), numberN (строка из N цифр), date (дата в формате дд.мм.гггг)
     * Если не указать N то будет сгенерирована строка из 10 знаков
     * <p>
     * generate#word10
     * Сгенерирует строку вида "kwptjshyis"
     * <p>
     * generate#word10&Тестовый -
     * Сгенерирует строку вида "Тестовый - kwptjshyis"
     * <p>
     * generate#number2&Модуль -
     * Сгенерирует строку вида "Модуль - 23"
     * <p>
     * generate#formatday;dd.MM.yyyy;day;1 (прибавление к текущей дате 1 день)
     * где
     * dd.MM.yyyy - формат даты
     * day/year/moth/hour/minute/second/ - интервал времени
     * 1 - значение которое нужно прибавить к интервалу
     * при этом можно вычитать значение из интервала указав отрицательное значение
     * generate#formatday;yyyy;year;-20 (отнимаем от текущей даты 1 год)
     * Для форматировании времни можно использовать формат HH:mm
     * generate#formatday;HH:mm;hour;10 (прибавляем к текущему времени 10 часов)
     * Генерация даты в нужной тайм зоне, регистр важен
     * generate#formatday locale:RU;d MMM TimeZone=+3
     * Генерация даты с маской (звёздочки вокруг итогового значения)
     * generate#formatday;*HH:mm*;hour;10 (сгенерирует значение типа *18:20*)
     * <p>
     * generate#fromfile;filename - возьмет текст из файла filename
     * <p>
     * Генерация реквизитов ИНН ФЛ, ИНН ЮЛ, КПП, СНИЛС
     * generate#innfl
     * generate#innul
     * generate#kpp
     * generate#snils
     * <p>
     * generate-stash#number3&Задача -#task
     * Сгенерирует и запишет в стэш с ключем "task" строку вида "Задача - 398"
     * при этом сгенерированное значение так же будет использовано в шаге, где оно было создано
     * <p>
     * stash#task
     * Достанет из стэша значение по ключу "task"
     * <p>
     * stash#task&Задача -
     * Достанет из стэша значение по ключу "task" и добавит перед ним строку "Задача -"
     * <p>
     * Генерация строки включающую в себя все символы, удовлетворяющие regexp выражению
     * generate#list;[0-9a-zA-Zа-яА-ЯёЁ!-\\/:-@\\[-`{-~№]
     * <p>
     * Генерация пароля по regexp выражению
     * generate#password;[0-9]{2}[a-z]{2}[A-Z]{2}[!@#$%=+]{2}
     * <p>
     * maven#admin_login
     * Получит значение testadmin01 из параметра запуска maven -Dadmin_login=testadmin01
     *
     * @param value строка для декодирования
     * @return декодированное значение или оригинальное значение если знака решётки нет
     */
    public static String decodeValue(final String value) {
        if (value.contains("#")) {
            final String[] args = value.split("#");

            String thirdArg = "";
            final String method;
            final String secondArg;
            switch (args.length) {
                case (3):
                    thirdArg = args[2].trim();
                case (2):
                    method = args[0].trim();
                    secondArg = args[1].trim();
                    break;
                default:
                    return value.replaceAll("\\\\s", " ");
            }
            final String result;
            switch (method) {
                case ("generate"):
                    result = generator(secondArg);
                    break;
                case ("stash"):
                    final String prefix;
                    final String stashKey;
                    if (secondArg.contains("&")) {
                        final String[] items = secondArg.split("&");
                        stashKey = items[0].trim();
                        prefix = items[1] + " ";
                    } else {
                        stashKey = secondArg;
                        prefix = "";
                    }
                    final String maskBefore = stashKey.startsWith("*") ? "*" : "";
                    final String maskAfter = stashKey.endsWith("*") ? "*" : "";
                    result = maskBefore + prefix + Stash.getValue(stashKey.replaceAll("\\*", "")) + maskAfter;
                    break;
                case ("generate-stash"):
                    result = generator(secondArg);
                    if ("".equals(thirdArg)) {
                        throw new AutotestError(
                                "Для сохранения сгенерированного значения в стэш укажите третий параметр - ключ значения");
                    }
                    Stash.put(thirdArg, result);
                    break;
                case ("maven"):
                    result = System.getProperty(args[1]);
                    Assert.assertNotNull(
                            String.format(
                                    "Добавьте в строку запуска maven параметр -D%s=value где value - значение для использования в тесте",
                                    args[1]
                            ), result
                    );
                    return result;
                default:
                    result = value;
            }
            log.info(String.format("Результат операции \"%s\": \"%s\"", value, result));
            AllureUtils.attachMessageToAllureStep(String.format("Результат операции %s", value), result);
            return result;
        }
        return value.trim();
    }

    /**
     * Метод генерирует последовательность из 10 символов
     *
     * @param argument тип генерируемой строки, допустимые значения "word" и "number" соответственно буквы и цыфры
     * @return сгенерированная строка
     */
    public static String generator(final String argument) {
        final String type;
        final String prefix;
        if (argument.contains("&")) {
            final String[] items = argument.split("&");
            type = items[0].toLowerCase().trim();
            prefix = items[1].trim().replaceAll("\\\\s", " ");
        } else {
            type = argument.toLowerCase().trim();
            prefix = "";
        }
        if ("date".equals(type)) {
            final long MAX_DATE = 1451509200000L;
            final Date date = new Date((long) (Math.random() * MAX_DATE));
            final DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
            return df.format(date);
        } else if (type.startsWith("formatday")) {
            return getFormatDay(argument);
        } else if (type.startsWith("file")) {
            return createFile(type);
        } else if (type.startsWith("fromfile")) {
            return getStringFromFile(argument);
        } else if (type.startsWith("list")) {
            return listGeneration(argument);
        } else if (type.startsWith("password")) {
            return passwordGeneration(argument);
        } else {
            final String[] allowed = new String[]{"innfl", "innul", "kpp", "snils"};
            Arrays.sort(allowed);
            if (Arrays.binarySearch(allowed, argument) > -1) {
                return requisitesGenerator(argument);
            }
        }
        return getString(type, prefix);
    }

    /**
     * Генерация строки включающую в себя все символы, удовлетворяющие regexp выражению
     * Все буквы латинского алфавита
     * generate#list;[a-zA-Z]
     * Цифры
     * generate#list;[0-9]
     * Спецсимволы
     * generate#list;[!-\/:-@\[-`{-~№]
     * Все буквы русского алфавита
     * generate#list;[а-яА-ЯёЁ]
     * Необходимо следить за длиной получаемой строки, она может оказаться длиньше, чем позволяет поле.
     * В этом случае нужно генерацию разделить на несколько частей
     */
    public static String listGeneration(final String argument) {
        return getStringFromRegExp(argument.split(";")[1]);
    }

    /**
     * Генерация пароля по regexp выражению
     * В аргументе нужно передать набор символов в квадратных скобках и количество символов в фигурных скобках
     * generate#password;[0-9]{2}[a-z]{2}[A-Z]{2}[!@#$%=+]{2}
     */
    public static String passwordGeneration(final String argument) {
        final List<String> allMatches = new ArrayList<>();
        final Matcher m = Pattern.compile("(\\[.*?]\\{\\d+})")
                .matcher(argument.split(";")[1]);
        while (m.find()) {
            allMatches.add(m.group());
        }
        final List<String> candidates = new ArrayList<>();
        allMatches.forEach(group -> {
            final Matcher dataMatcher = Pattern.compile("(\\[.*?])").matcher(group);
            final Matcher qtyMatcher = Pattern.compile("\\{(\\d+)}").matcher(group);
            if (dataMatcher.find() && qtyMatcher.find()) {
                final String exp = dataMatcher.group();
                final List<String> allSymbols = Arrays.asList(getStringFromRegExp(exp).split(""));
                for (int i = 0; i < Integer.parseInt(qtyMatcher.group(1)); i++) {
                    candidates.add(allSymbols.get((int) (Math.random() * allSymbols.size())));
                }
            } else {
                throw new AutotestError("Некорректное выражение для генерации: " + group);
            }
        });
        Collections.shuffle(candidates);
        return String.join("", candidates);
    }

    private static String getStringFromRegExp(final String regExp) {
        final Pattern pattern = Pattern.compile(regExp, Pattern.UNICODE_CASE);
        return IntStream.range(32, 2000)
                .mapToObj(s -> String.valueOf((char) s))
                .filter(s -> pattern.matcher(s).find())
                .collect(Collectors.joining());
    }

    /**
     * Генерация реквизитов ИНН ФЛ, ИНН ЮЛ, КПП, СНИЛС
     * generate#innfl
     * generate#innul
     * generate#kpp
     * generate#snils
     */
    @SneakyThrows
    private static String requisitesGenerator(final String argument) {
        final Document document = Jsoup.parse(new URL("https://radar4site.ru/pages/innkpp.html"), 5000);
        return document.selectFirst("#" + argument).attr("value");
    }

    private static String getString(final String type, final String prefix) {
        final int range;
        final char firstChar;
        final int count;
        final StringBuilder stringBuilder = new StringBuilder();
        if (type.startsWith("phrase")) {
            range = 26;
            firstChar = 'a';
            final String number = type.substring("phrase".length());
            count = Integer.parseInt(number);
            final Random wordLength = new Random();
            final Random letter = new Random();
            for (int i = 0; i < count; i++) {
                final int letterCount = wordLength.nextInt(8) + 3;
                for (int j = 0; j < letterCount; j++) {
                    final char symbol = (char) (letter.nextInt(range) + firstChar);
                    stringBuilder.append(symbol);
                }
                if (i < (count - 1)) {
                    stringBuilder.append(" ");
                }
            }
        } else {
            if (type.startsWith("word")) {
                range = 26;
                firstChar = 'a';
                final String number = type.substring("word".length());
                count = "".equals(number) ? 10 : Integer.parseInt(number);
            } else if (type.startsWith("number")) {
                range = 10;
                firstChar = '0';
                final String number = type.substring("number".length());
                count = "".equals(number) ? 10 : Integer.parseInt(number);
            } else {
                throw new AutotestError(String.format("Неизвестный аргумент генератора \"%s\"", type));
            }
            if (!"".equals(prefix)) {
                stringBuilder.append(prefix);
            }
            for (int i = 0; i < count; i++) {
                final Random random = new Random();
                final char symbol = (char) (random.nextInt(range) + firstChar);
                stringBuilder.append(symbol);
            }
        }
        return stringBuilder.toString();
    }

    @SneakyThrows
    private static String getStringFromFile(final String argument) {
        final String fileName = argument.split(";")[1].trim();
        try (final Scanner scanner = new Scanner(new File(System.getProperty("files.path") +
                                                          File.separator +
                                                          fileName))
        ) {
            return scanner.useDelimiter("\\Z").next();
        }
    }

    /**
     * Генерация файла произвольного типа с произвольным размером (указывается в мб)
     * Примеры:
     * generate#file;mp3;8 (mp3 файл с произвольным именем размером 8мб)
     * generate#file;txt;0.2;some_name (some_name.txt файл размером 0.2мб или 204.8кб)
     * generate-stash#file;mov;25#key (mov файл с произвольным названием размером 25мб, в стэш сохранит полный путь до файла)
     * <p>
     * Для типа PNG будет сгенерировано реальное изображение
     */
    private static String createFile(final String type) {
        final String[] parts = type.split(";");
        final String extension = parts[1];
        final String fileName = parts.length > 3 ? parts[3] : DataProcessing.generator("word5");
        final long sizeInBytes = (long) (Double.parseDouble(parts[2]) * 1024 * 1024);
        final File tempDirectory = new File(System.getProperty("target.directory")
                                            + File.separator + DataProcessing.generator("word5"));
        final File file = new File(tempDirectory.getAbsolutePath() + File.separator +
                                   fileName + "." + extension);
        try {
            Files.createDirectories(file.getParentFile().toPath());
        } catch (final IOException e) {
            throw new AutotestError(e);
        }
        final List<String> imageExtensions = Arrays.asList("png", "PNG");
        if (imageExtensions.contains(extension)) {
            final BufferedImage bufferedImage = ImageProcessing.generateImage();
            ImageProcessing.addBallastToImageAndWright(
                    bufferedImage,
                    file,
                    extension,
                    sizeInBytes
            );
        } else {
            generateFile(file, sizeInBytes);
        }
        try {
            Allure.addAttachment(fileName, extension, Files.newInputStream(file.toPath()), extension);
        } catch (final IOException e) {
            throw new AutotestError(e);
        }
        return file.getPath();
    }

    private static void generateFile(final File file, final long sizeInBytes) {
        try {
            Assert.assertTrue(
                    "\nНе удалось создать файл",
                    (file.getParentFile().exists() || file.getParentFile().mkdir()) && file.createNewFile()
            );
            try (final RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
                raf.setLength(sizeInBytes);
            }
        } catch (final IOException e) {
            throw new AutotestError(e);
        }
    }

    private static String getFormatDay(final String argument) {
        final String[] items = argument.split(";");
        final String locale = items[0].contains("locale:") ? items[0].split("locale:")[1] : "ru";
        final String format = items[1].split("TimeZone=")[0].trim();
        final String maskBefore = format.startsWith("*") ? "*" : "";
        final String maskAfter = format.endsWith("*") ? "*" : "";
        final DateFormatSymbols oldShortMonths = Validator.getNewSortMonths(locale);
        final DateFormat df = new SimpleDateFormat(format.replaceAll("\\*", ""), oldShortMonths);
        final TimeZone tz;
        if (items[1].contains("TimeZone")) {
            final String etc = items[1].split("TimeZone=")[1];
            final Pattern pattern = Pattern.compile("^[-+](1[0-2]|[1-9])$");
            Assert.assertTrue("Не верно введена тайм зона, шаблон TimeZone=+1", pattern.matcher(etc).find());
            final String zone;
            if (etc.contains("-")) {
                zone = etc.replace("-", "+");
            } else {
                zone = etc.replace("+", "-");
            }
            tz = TimeZone.getTimeZone("Etc/GMT" + zone);
        } else {
            tz = TimeZone.getDefault();
        }
        df.setTimeZone(tz);
        final Calendar calendar = new GregorianCalendar();
        if (items.length > 2) {
            final String timeInterval = items[2].toLowerCase().trim();
            final int timeOffset = Integer.parseInt(items[3].toLowerCase().trim());
            switch (timeInterval) {
                case "year":
                    calendar.add(Calendar.YEAR, timeOffset);
                    break;
                case "month":
                    calendar.add(Calendar.MONTH, timeOffset);
                    break;
                case "day":
                    calendar.add(Calendar.DAY_OF_MONTH, timeOffset);
                    break;
                case "hour":
                    calendar.add(Calendar.HOUR, timeOffset);
                    break;
                case "minute":
                    calendar.add(Calendar.MINUTE, timeOffset);
                    break;
                case "second":
                    calendar.add(Calendar.SECOND, timeOffset);
                    break;
            }
        }
        return maskBefore + df.format(calendar.getTime()) + maskAfter;
    }


    public static void writeInFile(
            final String fileType,
            final String fileName,
            final int pageNumber,
            final List<List<String>> draftData
    ) {
        final String actualFileName = DataProcessing.decodeValue(fileName);
        final String actualFileType = FilenameUtils.getExtension(actualFileName);
        Assert.assertEquals(
                String.format("Ожидаемый тип файла \"%s\" не сходится с фактическим \"%s\"", fileType, actualFileType),
                fileType,
                actualFileType
        );
        final XlsWriter writer = new XlsWriter(actualFileName, pageNumber, fileType);
        writer.fileWrite(draftData);
    }

    public static void matchDataInFile(
            final String fileType,
            final String fileName,
            final int pageNumber,
            final List<List<String>> draftData,
            final boolean dataBePresent
    ) {
        final ErrorCollector errorCollector = new ErrorCollector();
        final String actualFileName = DataProcessing.decodeValue(fileName);
        final String actualFileType = FilenameUtils.getExtension(actualFileName);
        Assert.assertEquals(
                String.format("Ожидаемый тип файла \"%s\" не сходится с фактическим \"%s\"", fileType, actualFileType),
                fileType,
                actualFileType
        );
        final List<List<String>> data = draftData
                .stream()
                .map(list -> list
                        .stream()
                        .map(DataProcessing::decodeValue)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
        final Object reader;
        switch (fileType) {
            case "xls":
            case "xlsx":
                reader = new XlsReader(actualFileName, pageNumber);
                for (final List<String> list : data) {
                    if (dataBePresent) {
                        errorCollector.assertTrue(
                                "Строка не найдена:\n" + String.join("; ", list),
                                ((XlsReader) reader).findFirstRow(list)
                        );
                    } else {
                        errorCollector.assertFalse(
                                "Строка не должна присутствовать в файле:\n" + String.join("; ", list),
                                ((XlsReader) reader).findFirstRow(list)
                        );
                    }
                }
                break;
            case "csv":
                // Если будут попадаться csv файлы с разделителем "точка с запятой", тогда
                // нужно добавить в шаг вариативность что бы выбирать между csv-comma и csv-semicolon
                reader = new CsvReader(actualFileName, ',');
                for (final List<String> list : data) {
                    if (dataBePresent) {
                        errorCollector.assertTrue(
                                "Строка не найдена:\n" + String.join("; ", list),
                                ((CsvReader) reader).findFirstRow(list)
                        );
                    } else {
                        errorCollector.assertFalse(
                                "Строка не должна присутствовать в файле:\n" + String.join("; ", list),
                                ((CsvReader) reader).findFirstRow(list)
                        );
                    }
                }
                break;
            case "json":
                reader = new JsonReader(actualFileName);
                for (final List<String> list : data) {
                    errorCollector.assertTrue(
                            "Для типа проверки JSON должно быть более одного параметра в строке",
                            list.size() > 1
                    );
                    final boolean result;
                    if (list.size() == 2) {
                        result = ((JsonReader) reader).verifyValueByKey(list.get(0), list.get(1));
                    } else {
                        result = ((JsonReader) reader).verifyValueByPath(list);
                    }
                    if (dataBePresent) {
                        errorCollector.assertTrue(
                                "Строка не найдена:\n" + String.join("; ", list),
                                result
                        );
                    } else {
                        errorCollector.assertFalse(
                                "Строка не должна присутствовать в файле:\n" + String.join("; ", list),
                                result
                        );
                    }
                }
                break;
            case "txt":
            case "yaml":
            case "yml":
                reader = new TxtReader(actualFileName);
                for (final List<String> list : data) {
                    if (dataBePresent) {
                        errorCollector.assertTrue(
                                "Строка не найдена:\n" + String.join("; ", list),
                                ((TxtReader) reader).findFirstRow(list)
                        );
                    } else {
                        errorCollector.assertFalse(
                                "Строка не должна присутствовать в файле:\n" + String.join("; ", list),
                                ((TxtReader) reader).findFirstRow(list)
                        );
                    }
                }
                break;
            case "pdf":

                try {
                    final PdfReader pdfReader = new PdfReader(actualFileName);
                    final int documentCount = pdfReader.getNumberOfPages();
                    String text = Objects.isNull(pdfReader.getInfo().get("Title")) ? "" : pdfReader.getInfo().get("Title");
                    for (int i = 1; i < documentCount+1; i++) {
                        text = text + PdfTextExtractor.getTextFromPage(pdfReader, i).replace("\n", "");
                    }
                    for (final List<String> list : data) {
                        if (dataBePresent) {
                            errorCollector.assertTrue(
                                    "Строка должна присутствовать в файле:\n" + String.join("; ", list),
                                    text.contains(list.get(0))
                            );
                        } else {
                            errorCollector.assertFalse(
                                    "Строка должна отсутствовать в файле:\n" + String.join("; ", list),
                                    text.contains(list.get(0))
                            );
                        }
                    }
                    pdfReader.close();
                } catch (IOException e) {
                    throw new AutotestError(String.format("Ошибка чтения файла \"%s\"", actualFileName), e);
                }
                break;
            default:
                throw new AutotestError(String.format("Для типа \"%s\" проверка не реализована", fileType));
        }
        errorCollector.assertAll();
    }

    public static void matchRowNumberInFile(final String fileType, final String fileName, final int rowNumber) {
        final String actualFileName = DataProcessing.decodeValue(fileName);
        final String actualFileType = FilenameUtils.getExtension(actualFileName);
        Assert.assertEquals(
                String.format("Ожидаемый тип файла \"%s\" не сходится с фактическим \"%s\"", fileType, actualFileType),
                fileType,
                actualFileType
        );
        final int actualRowNumber;
        final HasRows reader;
        switch (fileType) {
            case "xls":
            case "xlsx":
                reader = new XlsReader(actualFileName, 1);
                actualRowNumber = reader.getRowsSize();
                break;
            case "csv":
                reader = new CsvReader(actualFileName, ',');
                actualRowNumber = reader.getRowsSize();
                break;
            case "txt":
                reader = new TxtReader(actualFileName);
                actualRowNumber = reader.getRowsSize();
                break;
            default:
                throw new AutotestError(String.format("Для типа \"%s\" проверка не реализована", fileType));
        }
        Assert.assertEquals(
                String.format(
                        "Ожидаемое количество строк \"%d\" не сходится с фактическим \"%d\"",
                        rowNumber,
                        actualRowNumber
                ),
                rowNumber,
                actualRowNumber
        );
    }

    public static HashCode computeCrc32(final String path) throws IOException {
        final byte[] hash = Files.readAllBytes(Paths.get(path));
        return Hashing.crc32().hashBytes(hash);
    }
}
