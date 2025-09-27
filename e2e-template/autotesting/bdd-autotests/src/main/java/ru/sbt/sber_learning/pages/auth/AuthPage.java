package ru.sbt.sber_learning.pages.auth;

import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.actions.PageControls;
import ru.sbt.edu_power.e2e_core.auth.IsAuthPage;
import ru.sbt.edu_power.e2e_core.elements.Button;
import ru.sbt.edu_power.e2e_core.elements.LanguageSwitcher;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbt.edu_power.e2e_core.page_tags.ParentPage;
import ru.sbt.edu_power.e2e_core.smoke.EndPoints;
import ru.sbt.sber_learning.pages.TechPage;
import ru.sbt.sber_learning.pages.root_pages.RootEDU;
import ru.sbtqa.tag.pagefactory.annotations.ActionTitle;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.annotations.PageEntry;
import ru.sbtqa.tag.pagefactory.environment.Environment;
import ru.yandex.qatools.htmlelements.element.Link;
import ru.yandex.qatools.htmlelements.element.TextBlock;

@PageEntry(title = "Страница авторизации")
@ParentPage(pageClass = {
        RootEDU.class
})
@EndPoints(ignored = true)
public class AuthPage extends TechPage implements IsAuthPage {

    @ElementTitle("Переключатель языка")
    @FindBy(xpath = "//div[@id = 'locale-selector']")
    public LanguageSwitcher languageSwitcher;

    @ElementTitle("Логин")
    @FindBy(xpath = "//input[@id = 'username']")
    public TextInput loginTextInput;

    @ElementTitle("Пароль")
    @FindBy(xpath = "//input[@id = 'password']")
    public TextInput passwordTextInput;

    @ElementTitle("Войти")
    @FindBy(xpath = "//*[@id = 'kc-login' or @id = 'submit']")
    public Button submitButton;

    @ElementTitle("Забыли пароль?")
    @FindBy(xpath = "//a[contains(text(), 'Забыли пароль?')]")
    public TextBlock remindPasswordTextBlock;

    @ElementTitle("Восстановить пароль")
    @FindBy(xpath = "//button[@id = 'reset-password-btn']")
    public Button resetPasswordButton;

    @ElementTitle("Вернуться к авторизации")
    @FindBy(xpath = "//div[@id = 'back']")
    public TextBlock returnAuthTextBlock;

    @ElementTitle("Область контента")
    @FindBy(xpath = "//div[@id = 'login-screen']")
    public TextBlock contentArea;

    @ElementTitle("Блок Авторизация")
    @FindBy(xpath = "//div[contains(@class, 'container')]")
    public TextBlock authBlockTextBlock;

    @ElementTitle("Новый пароль")
    @FindBy(xpath = "//input[@id = 'password-new']")
    public TextInput passwordNewTextInput;

    @ElementTitle("Подтверждение пароля")
    @FindBy(xpath = "//input[@id = 'password-confirm']")
    public TextInput passwordConfirmTextInput;

    @ElementTitle("Подтвердить")
    @FindBy(xpath = "//button[@id = 'password-update-submit']")
    public Button acceptNewPasswordButton;

    @ElementTitle("Телеграм")
    @FindBy(xpath = "//a[@class='support-telegram-icon']")
    public Link telegramLink;

    @ElementTitle("WhatsApp")
    @FindBy(xpath = "//a[@class='support-whatsapp-icon']")
    public Link whatsAppLink;

    @ElementTitle("Сообщение об ошибке")
    @FindBy(xpath = "//div[contains(@class, 'alert_error')]")
    public TextBlock failLoginToastTextBlock;

    @ElementTitle("Неподдерживаемая версия Safari")
    @FindBy(xpath = "//div[@id = 'eduUnsupportedSafariVersion']")
    public TextBlock unsupportableSafariTextBlock;

    @ElementTitle("Электронная почта техподдержки")
    @FindBy(xpath = "//div[@class='support-data']//a[@class='support-mail']")
    public TextBlock supportEmailTextBlock;

    @ElementTitle("Время работы техподдержки")
    @FindBy(xpath = "//div[@class='contacts-footer']//div[@class='addition-info']")
    public TextBlock supportWorkingHoursTextBlock;

    @ElementTitle("Проверьте почту")
    @FindBy(xpath = "//div[@id='login-email-sent-unified']")
    public TextBlock checkEmailTextBlock;

    @ActionTitle("переходит на страницу авторизации Sberbox")
    public void goToSberBoxAuthPage() {
        final String authUrl = Environment
                .getDriverService()
                .getDriver()
                .getCurrentUrl()
                .replace("-auth", "-sberclass");
        Environment.getDriverService().getDriver().get(authUrl);
        PageControls.waitWhileNetworkActive();
    }

    @Override
    public TextInput getLoginField() {
        return loginTextInput;
    }

    @Override
    public TextInput getPasswordField() {
        return passwordTextInput;
    }

    @Override
    public Button getSubmitButton() {
        return submitButton;
    }

    @Override
    public boolean isAuthPageDisplayed() {
        return loginTextInput.isDisplayed() && passwordTextInput.isDisplayed();
    }
}
