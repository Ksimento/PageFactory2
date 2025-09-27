package ru.sbt.edu_power.e2e_core.data;

import io.qameta.allure.Allure;
import ru.sbt.edu_power.e2e_core.actions.Mover;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.resource_repository.ResourceRepository;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public class UrlProcessing {

    public static void goToUrl(final String url) {
        urlProcessWithLoadControl(url);
        PageControls.detectConsoleErrors();
        PageControls.detectBackendErrors();
        PageControls.detectUps404Errors();
        Mover.placeMouseEventScript();
        PageControls.waitWhenPreloaderIsGone();
    }

    // переход по URL без проверки фронт/бэк ошибок
    public static void goToUrlWithoutCheck(final String url) {
        urlProcessWithLoadControl(url);
        Mover.placeMouseEventScript();
    }

    // переход по URL без контроля загрузки
    public static void goToUrlWithoutLoadControl(final String url) {
        final String urlString = getUrl(url);
        Environment.getDriverService().getDriver().navigate().to(urlString);
        Mover.placeMouseEventScript();
        Allure.addAttachment("Адрес перехода", urlString);
    }

    private static void urlProcessWithLoadControl(final String draftUrl) {
        final AtomicInteger counter = new AtomicInteger(1);
        final BooleanSupplier waitWhenPageBeLoaded = () -> {
            final String url = getUrl(draftUrl);
            Environment.getDriverService().getDriver().navigate().to(url);
            Allure.addAttachment("Адрес перехода: "+draftUrl, url);
            DriverUtils.freeze(1000);
            if (!Environment.getDriverService().getDriver().getCurrentUrl().contains(url)) {
                Allure.addAttachment("Зафиксирован редирект, повтор перехода", Environment.getDriverService().getDriver().getCurrentUrl());
                if (counter.decrementAndGet() < 0) {
                    Allure.addAttachment("Редирект перманентный, необходимо проверить используемые эндпоинты", "");
                    PageControls.waitWhileNetworkActive();
                    PageControls.waitWhenPreloaderIsGone();
                    return true;
                }
                return false;
            }
            PageControls.waitWhileNetworkActive();
            PageControls.waitWhenPreloaderIsGone();
            return Environment.getDriverService().getDriver().getCurrentUrl().contains(url);
        };
        Timer.executeTimer(DriverConstants.TIMEOUT, waitWhenPageBeLoaded);
    }

    public static String getUrl(final String path) {
        return getUrl(path, true);
    }

    public static String getUrl(final String path, final boolean withSubdomain) {
        final String decodedPath = DataProcessing.decodeValue(path);
        final String url;
        if (decodedPath.startsWith("http")) {
            url = decodedPath;
        } else {
            final Map<String, String> urlRepository = ResourceRepository.getResource(ResourceRepository.AvailableResource.URLS);
            try {
                url = urlBuilder(urlRepository.getOrDefault(decodedPath, decodedPath), withSubdomain);
            } catch (final MalformedURLException e) {
                throw new AutotestError(e);
            }
        }
        return url;
    }

    public static String addUrlParam(final String param){
        final String url = Environment.getDriverService().getDriver().getCurrentUrl();
        return String.format("%s%s%s",url.split("&",2)[0],param+"&",url.split("&",2)[1]);
    }

    public static String urlBuilder(final String path) throws MalformedURLException {
        return urlBuilder(path, true);
    }

    public static String urlBuilderWithSubdomain(final String path, final String subdomain) {
        final String cleanUrl = getUrl(path, false);
        if (subdomain.isEmpty()) {
            return cleanUrl;
        }
        final String[] urlParts = cleanUrl.split("\\.", 2);
        return urlParts[0] + "-" + subdomain.replace("-", "") + "." + urlParts[1];
    }

    public static String urlBuilder(final String path, final boolean withSubdomain) throws MalformedURLException {
        final URL currentUrl = withSubdomain ? new URL(Environment.getDriverService().getDriver().getCurrentUrl()) :
                new URL(System.getProperty("webdriver.starting.url"));
        final String separator = path.startsWith("/") ? "" : "/";

        return String.format(
                "%s://%s%s%s", currentUrl.getProtocol(),
                currentUrl.getHost().startsWith("localhost") ? "localhost:4080" : currentUrl.getHost(),
                separator,
                path);
    }
}
