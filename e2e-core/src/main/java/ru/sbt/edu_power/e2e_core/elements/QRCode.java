package ru.sbt.edu_power.e2e_core.elements;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.qameta.allure.Allure;
import org.apache.commons.codec.binary.Base64InputStream;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.external_services.validator.Validator;
import ru.sbt.edu_power.external_services.validator.Validatable;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.yandex.qatools.htmlelements.element.TypifiedElement;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public class QRCode extends TypifiedElement implements Validatable {
    public QRCode(final WebElement wrappedElement) {
        super(wrappedElement);
    }

    @Override
    public String getText() {
        return getFieldValue();
    }

    @Override
    public String getFieldValue() {
        return decodeBase64();
    }

    @Override
    public boolean validate(final String expected) {
        return Validator.matchValues(getFieldValue(), expected);
    }

    private String getSource() {
        return getWrappedElement().findElement(By.xpath("descendant-or-self::img")).getAttribute("src");
    }

    /**
     * Метод берёт base64 закодированный QR код из src аттрибута изображения и декодирует его в строку
     */
    private String decodeBase64() {
        final byte[] source = getSource().replace("data:image/png;base64,", "").getBytes();
        try (final Base64InputStream base64InputStream = new Base64InputStream(new ByteArrayInputStream(source))) {
            final Result result = new MultiFormatReader().decode(
                    new BinaryBitmap(
                            new HybridBinarizer(
                                    new BufferedImageLuminanceSource(
                                            ImageIO.read(base64InputStream)
                                    )
                            )
                    )
            );
            return result.getText();
        } catch (final IOException | NotFoundException e) {
            throw new AutotestError(e);
        }
    }

    // Запоминаем секретный ключ для получения OTP вторично
    public String getSecret() {
        final AtomicReference<String> secret = new AtomicReference<>();
        final BooleanSupplier waitOTPCode = () -> {
            final URI otpAuthUrl = getURI();
            if (!otpAuthUrl.toString().startsWith("otpauth:")) {
                PageControls.refreshPage();
                return false;
            }
            final String secretKey = Stream.of(otpAuthUrl.getQuery().split("&"))
                                           .filter(query -> query.startsWith("secret"))
                                           .findFirst()
                                           .orElse("")
                                           .replace("secret=", "");
            secret.set(secretKey);
            return true;
        };
        Timer.executeTimerThrowable(DriverConstants.TIMEOUT, "Не удалось войти по одноразовому паролю", waitOTPCode);
        return secret.get();
    }

    // Получаем OTP по секретному ключу
    public static String getOneTimePasswordFromGoogleAuth(final String secret) {
        final GoogleAuthenticator gAuth = new GoogleAuthenticator();
        final AtomicInteger pass = new AtomicInteger();
        final List<String> incorrectPassList = new ArrayList<>();
        final BooleanSupplier waitWhenPasswordBePresent = () -> {
            pass.set(gAuth.getTotpPassword(secret));
            if (pass.get() > 99999) {
                return true;
            }
            DriverUtils.freeze(DriverConstants.FREEZE_500_MS * 4);
            incorrectPassList.add(String.valueOf(pass.get()));
            return false;
        };
        final boolean result = Timer.executeTimer(
                DriverConstants.TIMEOUT,
                waitWhenPasswordBePresent
        );
        if (!incorrectPassList.isEmpty()) {
            Allure.addAttachment("Некорректные пароли", String.join("\n", incorrectPassList));
        }
        if (!result) {
            throw new AutotestError("Не удалось получить шестизначный пароль");
        }
        return String.valueOf(pass.get());
    }

    private URI getURI() {
        try {
            return new URI(getFieldValue());
        } catch (final URISyntaxException e) {
            throw new AutotestError(e);
        }
    }
}
