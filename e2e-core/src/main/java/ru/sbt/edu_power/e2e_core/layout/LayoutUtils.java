package ru.sbt.edu_power.e2e_core.layout;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriverException;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.e2e_core.layout.enums.DimensionEnum;
import ru.sbtqa.tag.pagefactory.environment.Environment;

import java.util.function.BooleanSupplier;

public class LayoutUtils {
    public static Dimension getWindowSize() {
        return Environment.getDriverService().getDriver().manage().window().getSize();
    }

    private static void setWindowSize(final Dimension dimension) {
        Environment.getDriverService().getDriver().manage().window().setSize(dimension);
    }

    public static void setWindowSize(final int width, final int height) {
        setWindowSize(new Dimension(width, height));
    }

    public static int getBodyWidth() {
        final String js = "return document.documentElement.clientWidth";
        return ((Long) DriverUtils.executeJS(js)).intValue();
    }

    public static int getBodyHeight() {
        final String js = "return document.documentElement.clientHeight";
        return ((Long) DriverUtils.executeJS(js)).intValue();
    }

    static String badSymbolReplace(final String value) {
        return value
                .replaceAll("ё", "е")
                .replaceAll("Ё", "Е")
                .replaceAll("[()\"']", "");
    }

    public static void addCompleteButtonForBrokenTest() {
        final String executionEnvironment = System.getProperty("execution.environment");
        if ("jenkins".equals(executionEnvironment) || "headlessLocal".equals(executionEnvironment)) {
            return;
        }
        if ("Safari".equals(System.getProperty("webdriver.browser.name"))) {
            DriverUtils.freeze(DriverConstants.FREEZE_500_MS * 10);
            return;
        }
        DataSetVisualisation.addCompleteButtonToScreen();
        DriverUtils.freeze(DriverConstants.FREEZE_500_MS);
        final BooleanSupplier waitWhenTestBeComplete = () -> !DriverUtils.findElementsOnPageByXpath(
                "//div[@id = 'layout-testing-complete-button']").isEmpty();
        Timer.executeTimer(DriverConstants.TIMEOUT * 10, waitWhenTestBeComplete);
    }

    public static void setWindowSize(final DimensionEnum preset) {
        final BooleanSupplier waitWhenWindowsSizeBeSet = () -> {
            try {
                final Dimension browserSize = LayoutUtils.getWindowSize();

                final int width = (browserSize.getWidth() - LayoutUtils.getBodyWidth()) +
                                  (int) Math.round(preset.getBodyWidth() * preset.getScaleFactor());
                final int height = (browserSize.getHeight() - LayoutUtils.getBodyHeight()) +
                                   (int) Math.round(preset.getBodyHeight() * preset.getScaleFactor());
                LayoutUtils.setWindowSize(width, height);
                if (LayoutUtils.getBodyWidth() != (int) Math.round(preset.getBodyWidth() * preset.getScaleFactor()) ||
                    LayoutUtils.getBodyHeight() != (int) Math.round(preset.getBodyHeight() * preset.getScaleFactor())) {
                    return false;
                }
            } catch (final WebDriverException e) {
                return false;
            }
            return true;
        };
        Timer.executeTimerThrowable(
                DriverConstants.TIMEOUT,
                "Не удалось установить необходимый размер окна браузера",
                waitWhenWindowsSizeBeSet
        );
    }

    //    Возвращает текущее разрешение экрана
    public static DimensionEnum getCurrentDimension() {
        final int bodyWidth = getBodyWidth();
        for (final DimensionEnum dimension : DimensionEnum.values()) {
            final int width = (int) Math.round(bodyWidth / dimension.getScaleFactor());
            if (Math.abs(dimension.getBodyWidth() - width) < 50) {
                return dimension;
            }
        }
        return DimensionEnum.valueOf(System.getProperty(
                "webdriver.browser.size.preset",
                "DEFAULT"
        ));
    }
}
