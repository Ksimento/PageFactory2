package ru.sbt.edu_power.e2e_core.elements;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidArgumentException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebElement;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Available;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Fillable;
import ru.sbt.edu_power.e2e_core.elements.ifaces.Warning;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.yandex.qatools.htmlelements.utils.HtmlElementUtils.existsInClasspath;
import static ru.yandex.qatools.htmlelements.utils.HtmlElementUtils.getResourceFromClasspath;
import static ru.yandex.qatools.htmlelements.utils.HtmlElementUtils.isOnRemoteWebDriver;

// Тип поля - загрузка файла <input type="file">
// Поддерживается мультизагрузка, для этого поле должно поддерживать мультизагрузку <input type="file" multiple>
// и параметры нужно передавать через запятую
public class FileInput extends AutoIdentElement implements Fillable, Available, Warning {
    public FileInput(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public void fillField(final String fileName, final Boolean validated) throws FieldFillingException {
        if (fileName.contains(",") && !isMultiple()) {
            throw new FieldFillingException("Поле не поддерживает мультизагрзку файлов");
        }
        final List<String> decodedFileNameList = Stream.of(fileName.split(","))
                                                       .map(String::trim)
                                                       .map(DataProcessing::decodeValue)
                                                       .collect(Collectors.toList());
        final WebElement fileInputElement = getInput();
        if (isOnRemoteWebDriver(fileInputElement)) {
            setLocalFileDetector((RemoteWebElement) fileInputElement);
        }
        final List<String> values = new ArrayList<>();

        for (final String f : decodedFileNameList) {
            values.add(prepareData(f));
        }
        uploadFile(String.join("\n", values), fileInputElement);
    }

    private String prepareData(final String fileName) throws FieldFillingException {
        final String filePath;
        final File file = new File(fileName);
        if (file.exists()) {
            filePath = fileName;
        } else {
            filePath = getFilePath(System.getProperty("files.path") + File.separator + fileName);
        }
        try {
            return URLDecoder.decode(filePath, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new FieldFillingException("Не удалось декодировать файл", e);
        }
    }

    private void uploadFile(final String values, final WebElement fileInputElement) {
        DriverUtils.executeJS(
                "arguments[0].setAttribute('style', 'display:block;position:absolute')",
                fileInputElement
        );
        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
        fileInputElement.clear();
        DriverUtils.executeJS("arguments[0].setAttribute('style', 'display:none;')", fileInputElement);
//            Без этих задержек может происходить очистка соседнего поля O_o
        DriverUtils.freeze(DriverConstants.FREEZE_250_MS);
        try {
            fileInputElement.sendKeys(values);
        } catch (InvalidArgumentException e) {
            File f = new File(values);
            Assert.assertTrue(String.format(
                    "Не найдены файл по пути \"%s\" необходимо проверить путь и наименование файла",
                    values), f.exists() && !f.isDirectory());
        }
        DriverUtils.freeze(DriverConstants.FREEZE_500_MS * 4);

    }

    private void setLocalFileDetector(final RemoteWebElement element) {
        element.setFileDetector(new LocalFileDetector());
    }

    private String getFilePath(final String fileName) {
        if (existsInClasspath(fileName)) {
            return getPathForResource(fileName);
        }
        return getPathForSystemFile(fileName);
    }

    private String getPathForResource(final String fileName) {
        return getResourceFromClasspath(fileName).getPath();
    }

    private String getPathForSystemFile(final String fileName) {
        final File file = new File(fileName);
        return file.getAbsolutePath();
    }

    @Override
    protected WebElement getInput() {
        return getSpecifyElement(
                "descendant-or-self::input[@type = 'file']",
                "Не удалось получить элемент ввода"
        );
    }

    @Override
    public String getText() {
        return getInput().getAttribute("value");
    }

    @Override
    public boolean isAvailable() {
        final List<WebElement> buttons = getWrappedElement().findElements(By.xpath(
                "ancestor-or-self::div[contains(@class, '_FileUploader')]//button"));
        if (buttons.isEmpty()) {
            throw new AutotestError("Элемент Button не найден");
        }
        return buttons.get(0).isEnabled();
    }

    @Override
    public Optional<String> getTextWarning() {
        final AtomicReference<WebElement> element = new AtomicReference<>();
        final BooleanSupplier waitWhenWarningBeDisplayed = () -> {
            element.set(getWarningMessage());
            return null != element.get();
        };
        final boolean result = Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenWarningBeDisplayed);
        return result ? Optional.of(element.get().getText()) : Optional.empty();
    }

    private WebElement getWarningMessage() {
        final String warningXpath = ".//div[contains(@class, 'error-msg')] | .//span[contains(@class, '_SubText')] | .//p[contains(@class, '_ErrorLabel')]";
        final List<WebElement> elements = getParentIterator()
                .getCurrentParent()
                .findElements(By.xpath(warningXpath));
        if (elements.size() > 1) {
            throw new AutotestError("Найдено больше одного элемента Warning, проверьте xpath");
        }
        return elements.isEmpty() ? null : elements.get(0);
    }

    @Override
    public void notTextWarning() {
        final BooleanSupplier waitWhenWarningIsGone = () -> getWarningMessage() == null;
        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                "Предупреждение не исчезло по истечению времени",
                waitWhenWarningIsGone
        );
    }

    private boolean isMultiple() {
        return "true".equals(getInput().getAttribute("multiple"));
    }
}
