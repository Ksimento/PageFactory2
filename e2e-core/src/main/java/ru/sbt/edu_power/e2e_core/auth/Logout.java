package ru.sbt.edu_power.e2e_core.auth;

import lombok.SneakyThrows;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.blocks.BlockExtractor;
import ru.sbt.edu_power.e2e_core.data.UrlProcessing;
import ru.sbt.edu_power.e2e_core.devtools.network.NetworkControl;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.pagefactory.environment.Environment;

import java.net.URL;
import java.util.function.BooleanSupplier;

public class Logout {
    private final boolean isBeta;
    private final IsAuthPage authPage;
    private final URL url;
    private final String rootDomain;
    private final StandType standType;
    private final String logoutHost;
    private final int port;
    private final String redirectQuery;

    @SneakyThrows
    public Logout(final IsAuthPage authPage) {
        this.authPage = authPage;
        url = new URL(Environment.getDriverService().getDriver().getCurrentUrl());
        isBeta = url.getHost().contains("-beta");
        standType = StandType.determineStandType(url.getHost());
        port = url.getPort();
        rootDomain = new URL(url.getProtocol(), url.getHost(), port, "").toString();
        logoutHost = getLogoutHost();
        redirectQuery = "?redirect_uri=" + rootDomain;
    }

    /**
     * Метод позволяет определить является ли пользователь методологом
     * @return правильную часть url logout
     */
    private String getLogoutMethodology() {
        final String[] urlPath = url.getFile().split("/");
        if (urlPath.length > 1) {
            return urlPath[1].equals("admin") ? "/admin" : "";
        }
        return "";
    }

    public void logout() {
        final String logoutUrl = logoutHost + standType.getLogoutService() + redirectQuery;
        final BooleanSupplier waitWhenUserLogOut = () -> {
            if (authPage.isAuthPageDisplayed()) {
                return true;
            }
            Environment.getDriverService().getDriver().navigate().to(logoutUrl);
            if (isBeta) {
                Environment.getDriverService().getDriver().navigate().to(rootDomain);
            }
            PageControls.waitWhileNetworkActive();
            final BooleanSupplier waitLoginForm = () -> DriverUtils.isElementDisplayed(authPage.getLoginField());
            final boolean result = Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitLoginForm);
            if (result) {
                return true;
            }
            UrlProcessing.goToUrlWithoutLoadControl(rootDomain);
            PageControls.waitWhileNetworkActive();
            return false;
        };
        Timer.executeTimerThrowable(DriverConstants.TIMEOUT * 2, "Не удалось выполнить выход из системы", waitWhenUserLogOut);
        NetworkControl.getNetworkActiveConnectionInstance().purge();
    }

    public static void logoutByMenu() {
        final BlockExtractor block = new BlockExtractor("Список переключателей профиля->last->Фото");
        final BooleanSupplier waitWhenUserBeLogout = () -> {
            ClickActions.clickElementIfExist("Кнопка меню", true);
            try {
                final WebElement button = block.getElement();
                ClickActions.safeClick("Смена роли", button);
                ClickActions.clickWithChangeControl("Выйти");
                return true;
            } catch (final RuntimeException e) {
                return false;
            }
        };
        Timer.executeTimerThrowable(DriverConstants.TIMEOUT, "Не удалось выйти через меню", waitWhenUserBeLogout);
    }

    @SneakyThrows
    private String getLogoutHost() {
        switch (standType) {
            case LOCALHOST:
                return new URL(url.getProtocol(), url.getHost(), port, "").toString();
            case PROD_S21_ADMIN:
            case DEMO_DATA_S21_ADMIN:
            case STAGE_S21_ADMIN:
            case DEV_S21_ADMIN:
            case STAGE_S21_STUDENT:
            case PROD_S21_STUDENT:
            case DEV_S21_STUDENT:
            case DEMO_DATA_S21_STUDENT:
                return new URL(url.getProtocol(), url.getHost(), "") + getLogoutMethodology();
            case PROD_EDU:
            case DEMO:
            case DEV_EDU:
            case STAGE_EDU:
            case DEMO_DATA:
            default:
                return new URL(url.getProtocol(), url.getHost(), "").toString();

        }
    }

}
