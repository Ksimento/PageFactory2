package ru.sbt.edu_power.e2e_core.remote_access;


import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Moon {
    private static final String CLIPBOARD = "{MOON_URL}/clipboard/{SESSION_ID}";
    private static final String FILE_DOWNLOAD = "{MOON_URL}/download/{SESSION_ID}/";
    private static final String MOON_URL = "http://bigqa:CQMo7RmpKP@45.89.225.234:30234";

    // Метод получает идентификатор сессии Moon
    private static String getSessionId() {
        final WebDriver driver = Environment.getDriverService().getDriver();
        if (driver instanceof RemoteWebDriver) {
            try {
                final Field field = driver.getClass().getDeclaredField("sessionId");
                field.setAccessible(true);
                return field.get(driver).toString();
            } catch (final NoSuchFieldException e) {
                throw new AutotestError("Драйвер не содержит поля sessionId", e);
            } catch (final IllegalAccessException e) {
                throw new AutotestError("Ошибка доступа к полю sessionId", e);
            }
        }
        return "";
    }

    // Метод устанавливает контент в буфер обмена на стороне браузера в муне
    public static void setClipBoard(final String value) {
        final String url = CLIPBOARD
                .replace("{MOON_URL}", MOON_URL)
                .replace("{SESSION_ID}", getSessionId());
        final HttpResponse<?> httpResponse = Unirest.post(url)
                                                    .body(DataProcessing.decodeValue(value))
                                                    .asEmpty();
        if (!httpResponse.isSuccess()) {
            throw new AutotestError(
                    "Не удалось установить содержимое буфера обмена",
                    httpResponse.getParsingError().orElse(null)
            );
        }
    }

    //    Метод считывает содержимое буфера обмена на стороне браузера в муне
    public static String getClipboard() {
        final String url = CLIPBOARD
                .replace("{MOON_URL}", MOON_URL)
                .replace("{SESSION_ID}", getSessionId());
        final HttpResponse<String> httpResponse = Unirest.get(url).asString();
        if (!httpResponse.isSuccess()) {
            throw new AutotestError(
                    "Не удалось получить сорежимое буфера обмена",
                    httpResponse.getParsingError().orElse(null)
            );
        }
        return httpResponse.getBody();
    }

    /**
     * Метод скачивает файл из контейнера из папки загрузки браузера
     * В браузере все файлы скачиваются с именем download (следующие идут с индексом download(1) и т.д.)
     *
     * @param fileName      Имя файла на стороне браузера
     * @param fileExtension Ожидаемое расширение файла
     * @return Путь до сохранённого файла в локальной ФС
     */
    public static String downloadFile(final String fileName, final String fileExtension) {
        final String fileUrl = FILE_DOWNLOAD
                .replace("{MOON_URL}", MOON_URL)
                .replace("{SESSION_ID}", getSessionId())
                .concat(fileName);
        final String localFileName = Paths.get(
                System.getProperty("target.directory"),
                "browser_download",
                DataProcessing.generator("word10"),
                attachFileExtension(fileName, fileExtension)
        ).toString();
        try {
            FileUtils.copyURLToFile(
                    new URL(fileUrl),
                    new File(localFileName),
                    1000,
                    1000
            );
            return localFileName;
        } catch (final MalformedURLException e) {
            throw new AutotestError("URL не доступен: " + fileUrl, e);
        } catch (final IOException e) {
            throw new AutotestError("Ошибка IO" + fileUrl, e);
        }
    }

    private static String attachFileExtension(final String fileName, final String fileExtension) {
        final String cleanExtension = fileExtension.replace(".", "");
        if (fileName.contains(".")) {
            final String[] parts = fileName.split("\\.");
            if (parts[parts.length - 1].equalsIgnoreCase(cleanExtension)) {
                return fileName;
            }
        }
        return fileName + "." + cleanExtension;
    }

//    Метод получает список файлов из папки загрузки в муне
    public static List<String> getFileList() {
        final String fileListUrl = FILE_DOWNLOAD
                .replace("{MOON_URL}", MOON_URL)
                .replace("{SESSION_ID}", getSessionId());
        final HttpResponse<String> httpResponse = Unirest.get(fileListUrl)
                                                           .asString();
        return Jsoup.parse(httpResponse.getBody())
                .getElementsByTag("a")
                .stream()
                .map(n -> n.attr("href"))
                .collect(Collectors.toList());
    }
}
