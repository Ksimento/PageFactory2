package ru.sbt.edu_power.e2e_core.remote_access;

import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.allure.AllureUtils;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.fields.FindUtils;
import ru.sbt.edu_power.e2e_core.table_processing.TableActions;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class BrowserUtils {

    //    Метод определяет тип операционки и возвращает директорию загрузки по умолчанию
    private static String getDefaultDownloadPath() {
        final String downloadFilepath;
        final String browserArgs = System.getProperty("webdriver.chrome.capability.options.args");
        if (browserArgs.contains("headless")) {
            downloadFilepath = System.getProperty("basedir");
        } else {
            final String OSType = getBrowserOSType();
            if ("Windows".equals(OSType)) {
                downloadFilepath = System.getProperty("user.home") + "\\Downloads";
            } else {
                final String executionEnvironment = System.getProperty("execution.environment");
                if ("jenkins".equals(executionEnvironment)) {
                    downloadFilepath = "/home/seluser/Downloads";
                } else {
                    downloadFilepath = System.getProperty("user.home") + "/Downloads";
                }
            }
        }
        log.info("Ожидаемая директория загрузки файлов браузером: {}", downloadFilepath);
        final File path = new File(downloadFilepath);
        if (path.exists() && path.isDirectory()) {
            log.info("This directory right");
        }
        System.setProperty("default.download.directory", downloadFilepath);
        return downloadFilepath;
    }

    //    Метод определяет тип операционной системы в которой запущен браузер
    public static String getBrowserOSType() {
        final String rawOSType = (String) DriverUtils.executeJS("return window.navigator.userAgent;");
        final String OSType;
        if (rawOSType.contains("Windows")) {
            OSType = "Windows";
        } else if (rawOSType.contains("Mac")) {
            OSType = "Mac";
        } else if (rawOSType.contains("Linux")) {
            OSType = "Linux";
        } else {
            throw new AutotestError("Неизвестный тип операционной системы: " + rawOSType);
        }
        return OSType;
    }

    //    Метод возвращает список файлов из директории загрузки локального браузера
    public static List<String> getFileList() {
        final String downloadFilepath = System.getProperty("default.download.directory") != null ?
                System.getProperty("default.download.directory") : getDefaultDownloadPath();
        final File path = new File(downloadFilepath);
        final File[] directoryElements = path.listFiles();
        final List<String> fileList = new ArrayList<>();
        if (directoryElements != null) {
            for (final File file : directoryElements) {
                if (file.exists() && file.isFile()) {
                    fileList.add(file.getAbsolutePath());
                }
            }
        }
        return fileList;
    }

    // Метод выполняет поиск строки в таблице, нажатие на элемент в нужном столбце и контроллирует скачивание файла
    // Работает как в локальном браузере так и в moon
    public static List<String> downloadFiles(
            final String tableName,
            final String colName,
            final List<List<String>> data,
            final String extension
    ) {
        return downloadFiles(
                () -> new TableActions(tableName).findTableElement(data, colName),
                colName,
                extension
        );
    }

    // Метод выполняет нажатие на элемент и контроллирует скачивание файла
    // Работает как в локальном браузере так и в moon
    public static synchronized List<String> downloadFiles(final String downloadButtonName, final String extension) {
        return downloadFiles(
                () -> FindUtils.getElementByNameOrPath(downloadButtonName),
                downloadButtonName,
                extension
        );
    }

    /**
     * Реализация контроллируемой загрузки файла
     *
     * @param getElementFunction Лямбда функция, которая выполняет поиск веб-элемента
     * @param elementName        Название кнопки (для вывода в отчётах об ошибке)
     * @param extension          Ожидаемый тип файла
     * @return
     */
    private static synchronized List<String> downloadFiles(
            final Supplier<WebElement> getElementFunction,
            final String elementName,
            final String extension
    ) {
        final WebElement button = getElementFunction.get();
        log.info(
                "downloadFilesStart:\nTime:{}\nScenario: {}\nbuttonName: {}\n-----",
                new SimpleDateFormat("dd MM yyyy HH:mm:ss.SSS").format(new Date()),
                Environment.getScenario().getName(),
                button.getText()
        );
        final List<String> filesBeforeDownload = BrowserUtils.getFileList();
        final List<String> filesAfterDownload = new ArrayList<>();
        final List<String> downloadedFileNames = new ArrayList<>();
        final BooleanSupplier waitWhenFileBeDownloaded = () -> {
            DriverUtils.freeze(DriverConstants.TIME_1SEC * 6);
            filesAfterDownload.clear();
            filesAfterDownload.addAll(BrowserUtils.getFileList());
            if (filesBeforeDownload.equals(filesAfterDownload)) {
                try {
                    ClickActions.safeClick(elementName, getElementFunction.get());
                    PageControls.waitWhileNetworkActive();
                    PageControls.detectBackendErrors();
                    PageControls.detectConsoleErrors();
                    PageControls.detectUps404Errors();
                } catch (final Throwable e) {
                    AllureUtils.attachMessageToAllureStep(
                            "Возникло исключение",
                            Arrays.toString(e.getStackTrace())
                    );
                }
                return false;
            }
            downloadedFileNames.clear();
            downloadedFileNames.addAll(filesAfterDownload
                    .stream()
                    .filter(fileName -> !filesBeforeDownload.contains(fileName))
                    .collect(Collectors.toList()));
            downloadedFileNames.removeIf(file -> !file.endsWith(extension));
            AllureUtils.attachMessageToAllureStep("Файл", Arrays.toString(downloadedFileNames.toArray()));
            return !downloadedFileNames.isEmpty();
        };

        boolean result = Timer.executeTimer(DriverConstants.TIME_1SEC * 30, waitWhenFileBeDownloaded);
        if (!result) {
            if (downloadedFileNames.isEmpty()) {
                throw new AutotestError("Не удалось загрузить файлы");
            } else {
                throw new AutotestError(String.format(
                        "Ожидаемый тип файла \"%s\". Загруженные файлы не соответствуют:\n\"%s\"",
                        extension, String.join("\n", downloadedFileNames)
                ));
            }
        }
        final List<String> fileNames = downloadedFileNames
                .stream()
                .map(f -> saveDownloadedFileToTarget(f, extension))
                .collect(Collectors.toList());
        log.info(
                "downloadFilesEnd:\nTime:{}\nСкачаны:\n{}\n-----",
                new SimpleDateFormat("dd MM yyyy HH:mm:ss.SSS").format(new Date()),
                String.join("\n", fileNames)
        );
        return fileNames;
    }

    //    Метод забирает скачанный файл в target для дальнейшей работы с файлом
    //    Работает как в локальном браузере так и в moon
    private static String saveDownloadedFileToTarget(final String fileName, final String fileExtension) {
        final boolean IS_MOON = !"".equals(System.getProperty("webdriver.url"));
        if (IS_MOON) {
            return Moon.downloadFile(fileName, fileExtension);
        } else {
            final File sourceFile = Paths.get(fileName).toFile();
            final File destinationFile = Paths.get(
                                                      System.getProperty("target.directory"),
                                                      "browser_download",
                                                      DataProcessing.generator("word10"),
                                                      FilenameUtils.getName(fileName)
                                              )
                                              .toFile();
            try {
                FileUtils.deleteQuietly(destinationFile);
                FileUtils.moveFile(sourceFile, destinationFile);
            } catch (final IOException e) {
                throw new AutotestError("Не удалось выполнить копирование файла", e);
            }
            try (final InputStream inputStream = Files.newInputStream(destinationFile.toPath())) {
                Allure.addAttachment(destinationFile.getName(), fileExtension, inputStream, fileExtension);
            } catch (final IOException e) {
                throw new AutotestError(e);
            }
            return destinationFile.getAbsolutePath();
        }
    }

    // для последжующей работы с файлом, метод скачивает файл по url из локатора, который содержит
    // ссылку на файл в аттрибуте href, который по умолчанию открывает файл в новой вкладке
    public static synchronized String directDownloadFile(final String downloadButtonName)
            throws IOException, InterruptedException {
        final String url = FindUtils.getElementByNameOrPath(downloadButtonName).getAttribute("href");
        final String homeDirectory = System.getProperty("user.dir") + File.separator
                                     + System.getProperty("target.directory") + File.separator
                                     + DataProcessing.generator("word30");
        final File file = new File(homeDirectory);
        Assert.assertTrue(
                "Не удалось создать директории: " + homeDirectory,
                file.mkdirs()
        );
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("curl", "-O", url)
                      .directory(new File(homeDirectory))
                      .start()
                      .waitFor();

        return homeDirectory + File.separator + url.replaceFirst(".*/(\\w+)", "$1").replace("static/", "");
    }

    // Метод записывает в системные переменные тип доступного буфера обмена
    public static ClipboardType getClipboardType() {
        if (null == System.getProperty("environment.clipboard.type")) {
            final boolean IS_MAC = "Mac".equals(BrowserUtils.getBrowserOSType());
            final boolean IS_MOON = !"".equals(System.getProperty("webdriver.url"));
            if (IS_MOON) {
                System.setProperty("environment.clipboard.type", ClipboardType.MOON.name());
            } else if (IS_MAC) {
                System.setProperty("environment.clipboard.type", ClipboardType.NONE.name());
            } else {
                try {
                    Toolkit.getDefaultToolkit()
                           .getSystemClipboard();
                    System.setProperty("environment.clipboard.type", ClipboardType.NATIVE.name());
                } catch (final AWTError e) {
                    System.setProperty("environment.clipboard.type", ClipboardType.NONE.name());
                }
            }
        }
        final ClipboardType clipboardType = ClipboardType.valueOf(
                System.getProperty("environment.clipboard.type")
        );
        log.info("Доступный тип буфера обмена: {}", clipboardType);
        return clipboardType;
    }

    public enum ClipboardType {
        MOON,
        NATIVE,
        NONE
    }
}
