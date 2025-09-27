package ru.sbt.edu_power.e2e_core.auth;

import io.qameta.allure.Allure;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.ClickActions;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.allure.AllureUtils;
import ru.sbt.edu_power.e2e_core.api.requests.APIAuth;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbt.edu_power.e2e_core.data.UrlProcessing;
import ru.sbt.edu_power.e2e_core.devtools.network.NetworkControl;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.resource_repository.ResourceRepository;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Slf4j
public class AuthAction {
    private static Class<? extends Page> authPageClass;
    private static final String APP_XPATH = "//div[@id = 'app-root' or @id = 'root' " +
                                            "or @id = 'upload-page' " +
                                            "or contains(@class, 'kc-totp-login-form')]";

    public static void configure(final Class<? extends Page> authPageClass) {
        AuthAction.authPageClass = authPageClass;
    }

    public static void authWithProfileName(final String profileName) {
        authWithProfileName(profileName, "");
    }

    public static void authWithProfileName(final String profileName, final String subdomain) {
        final Map<String, Map<String, Map<String, String>>> resource = getLoginAndPasswordInFile(profileName);
        authWithLoginAndPassword(
                resource.get("predefined_data").get(profileName).get("name"),
                resource.get("predefined_data").get(profileName).get("password"),
                subdomain
        );
    }

    public static Map<String, Map<String, Map<String, String>>> getLoginAndPasswordInFile(final String profileName) {
        final Map<String, Map<String, Map<String, String>>> resource = ResourceRepository
                .getResource(ResourceRepository.AvailableResource.USERS);

        Assert.assertTrue(
                String.format("Нет профиля \"%s\" в файле users.json", profileName),
                resource.get("predefined_data").containsKey(profileName)
        );
        return resource;
    }

    public static void authWithLoginAndPassword(final String userLogin, final String userPassword) {
        authWithLoginAndPassword(userLogin, userPassword, "");
    }

    public static void authWithLoginAndPassword(
            final String userLogin,
            final String userPassword,
            final String subdomain
    ) {
        authWithLoginAndPassword(userLogin, userPassword, DataProcessing.generator("word20"), subdomain);
        Mover.placeMouseEventScript();
    }

    private static boolean authWithLoginAndPassword(
            final String userLogin,
            final String userPassword,
            final String serverDownTimer,
            final String subdomain
    ) {
        Allure.attachment("Учетные данные", String.format("Логин: %s\nПароль: %s", userLogin, userPassword));
        final String login = DataProcessing.decodeValue(userLogin);
        final String password = DataProcessing.decodeValue(userPassword);
        authAction(login, password, subdomain);
        Allure.addAttachment("URL after login", Environment.getDriverService().getDriver().getCurrentUrl());
        final List<WebElement> failLoginElements = new ArrayList<>();
        final BooleanSupplier waitWhenAuthServiceUp = () -> {
            failLoginElements.clear();
            failLoginElements.addAll(DriverUtils.findElementsOnPageByXpath(Props.get("FAIL_LOGIN_TOAST")));
            failLoginElements.addAll(DriverUtils.findElementsOnPageByXpath(
                    "//*[node() = 'Internal Server Error'] | " +
                    "//*[node() = 'no healthy upstream'] | " +
                    "//*[contains(text(), 'К экрану ввода логина и пароля')] | " +
                    "//*[contains(node(), 'Не удалось получить Access Token при аутентификации с IdP сервера в связи с ошибкой')]")
            );
            if (failLoginElements.isEmpty()) {
                return true;
            }
            AllureUtils.attachScreenShotToAllure("FAIL LOGIN");
            return authWithLoginAndPassword(userLogin, userPassword, serverDownTimer, subdomain);
        };
        final boolean result = Timer.executeTimer(serverDownTimer, DriverConstants.TIMEOUT * 4, waitWhenAuthServiceUp);
        if (!result) {
            throw new AutotestError(String.format(
                    "Ошибка авторизации:\n%s",
                    failLoginElements.stream().map(WebElement::getText).collect(Collectors.joining("\n"))
            ));
        }

        if (!waitWhenAppIsPresent()) {
            Allure.addAttachment(
                    "Последние запросы",
                    String.join("\n", NetworkControl
                            .getNetworkActiveConnectionInstance()
                            .getLastRequests(10)
                    )
            );
            Allure.addAttachment("DOM", Environment.getDriverService().getDriver().getPageSource());
            throw new AutotestError(
                    String.format(
                            "Не удалось выполнить вход в систему с учётными данными:\nЛогин: \"%s\"\nПароль: \"%s\"",
                            login,
                            password
                    )
            );
        }
        return true;
    }

    private static boolean waitWhenAppIsPresent() {
        final BooleanSupplier waitWhenAppIsPresent = () -> !DriverUtils.findElementsOnPageByXpath(APP_XPATH).isEmpty();
        return Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenAppIsPresent);
    }

    @SneakyThrows
    public static void logout() {
        final Logout logout = new Logout((IsAuthPage) DriverUtils.getPage(authPageClass));
        logout.logout();
    }

    public static void logoutByMenu() {
        Logout.logoutByMenu();
    }

    public static void setToken(final String profileName) {
        final Map<String, Map<String, Map<String, String>>> resource = AuthAction.getLoginAndPasswordInFile(profileName);
        setToken(resource.get("predefined_data").get(profileName).get("name"),
                resource.get("predefined_data").get(profileName).get("password"));
    }

    public static void setToken(final String userLogin, final String userPassword) {
        Environment
                .getDriverService()
                .getDriver()
                .manage()
                .addCookie(new Cookie("tokenId",
                        APIAuth.getToken(
                                userLogin,
                                userPassword
                        ), ".pcbltools.ru", "", null, false, true
                ));
    }

    @SneakyThrows
    public static String getStartingURLInSubdomain(final String subdomain) {
        DriverUtils.getPage(AuthAction.authPageClass);
        final String urlHost = startingUrl.getHost();
        final String domain = "." + startingUrl.getHost().split("\\.")[1];
        final String host = urlHost.replace(
                domain,
                subdomain + domain
        );
        final String port = startingUrl.getPort() == -1 ? "" : ":" + startingUrl.getPort();
        return String.format("%s://%s%s", startingUrl.getProtocol(), host, port);
    }

    @SneakyThrows
    private static void authAction(final String login, final String password, final String subdomain) {
        Allure.addAttachment("Попытка входа в систему", "");
        final Page authPage = DriverUtils.getPage(AuthAction.authPageClass);
        final URL startingUrl = new URL(System.getProperty("webdriver.starting.url"));
        final String subdomainUrl = getStartingURLInSubdomain(subdomain);
        final boolean subdomainInQuery = Objects.nonNull(startingUrl.getQuery()) &&
                                         startingUrl.getQuery().contains(subdomain);
        if (!Environment.getDriverService().getDriver().findElements(By.xpath(APP_XPATH)).isEmpty()) {
            logout();
        }
        if (!subdomain.isEmpty() && !subdomainInQuery) {
            UrlProcessing.goToUrlWithoutLoadControl(subdomainUrl);
        }

        // Ожидаем форму логина, перезагружаем страницу если форма не появилась
        final BooleanSupplier waitWhenLoginFormBeRendered = () ->
                DriverUtils.isElementDisplayed(((IsAuthPage) authPage).getLoginField()) &&
                DriverUtils.isElementDisplayed(((IsAuthPage) authPage).getPasswordField());

        // Пытаемся заполнить форму логина
        final BooleanSupplier waitWhenAuthFormIsFilled = () -> {
            if (!Timer.executeTimer(DriverConstants.ELEMENT_WAIT_5SEC, waitWhenLoginFormBeRendered)) {
                UrlProcessing.goToUrlWithoutLoadControl(subdomainUrl);
                DriverUtils.freeze(1000);
                Allure.addAttachment(
                        "Элементы формы логина не появились, пробую перезагрузку",
                        String.format(
                                "URL перенаправления: %s\nТекущий URL: %s",
                                subdomainUrl,
                                Environment.getDriverService().getDriver().getCurrentUrl()
                        )
                );
                return false;
            }
            try {
                ((IsAuthPage) authPage).getLoginField().clear();
                ((IsAuthPage) authPage).getPasswordField().clear();
                ((IsAuthPage) authPage).getLoginField().sendKeys(login);
                ((IsAuthPage) authPage).getPasswordField().sendKeys(password);
                return login.equals(((IsAuthPage) authPage).getLoginField().getText()) &&
                       password.equals(((IsAuthPage) authPage).getPasswordField().getText());
            } catch (final NoSuchElementException e) {
                Allure.addAttachment(
                        "Форма логина сломалась при заполнении, пробую перезагрузку",
                        Environment.getDriverService().getDriver().getPageSource()
                );
                PageControls.detectConsoleErrors();
                UrlProcessing.goToUrlWithoutLoadControl(subdomainUrl);
                return false;
            }
        };

        final BooleanSupplier waitWhenAuthButtonBeAvailable = () -> {
            if (!Timer.executeTimer(
                    DriverConstants.TIMEOUT,
                    waitWhenAuthFormIsFilled
            )) {
                return false;
            }
            final boolean buttonIsAvailable = Timer.executeTimerMillis(
                    DriverConstants.FREEZE_250_MS,
                    () -> ((IsAuthPage) authPage).getSubmitButton().isAvailable()
            );
            if (buttonIsAvailable) {
                return true;
            }
            AllureUtils.attachScreenShotToAllure("Кнопка логина не активна");
            UrlProcessing.goToUrlWithoutCheck(subdomainUrl);
            return false;
        };
        if (Timer.executeTimer(
                DriverConstants.TIMEOUT * 2,
                waitWhenAuthButtonBeAvailable
        )) {
            ClickActions.safeClick("Кнопка логина", ((IsAuthPage) authPage).getSubmitButton());
        }
    }
}
