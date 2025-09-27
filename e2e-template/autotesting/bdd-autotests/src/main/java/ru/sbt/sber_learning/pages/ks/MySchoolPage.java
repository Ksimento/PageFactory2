package ru.sbt.sber_learning.pages.ks;

import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.elements.Button;
import ru.sbt.edu_power.e2e_core.elements.FileInput;
import ru.sbt.edu_power.e2e_core.elements.dropdown.Select;
import ru.sbt.edu_power.e2e_core.elements.image.Image;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbt.edu_power.e2e_core.smoke.EndPoints;
import ru.sbt.sber_learning.Constants;
import ru.sbt.sber_learning.blocks.main.ConfiguratorProfileListItem;
import ru.sbt.sber_learning.pages.TechPage;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.annotations.PageEntry;
import ru.yandex.qatools.htmlelements.element.TextBlock;

import java.util.List;

/**
 * Бизнес-администратор - Моя школа
 * Абстрактная страница с перечислением пунктов меню, для тестов не использовать.
 * Вместо этого использовать страницу "Моя школа - Здания" так как она первая открывается
 * при переходе в школу
 */
@PageEntry(title = "Конфигуратор системы - Моя школа")
@EndPoints(configuratorEDU = "/my-school")
public class MySchoolPage extends TechPage {
    @ElementTitle("Блок информации о школе")
    @FindBy(xpath = "//div[@data-testid = 'UIKit.Paper']")
    public TextBlock schoolInfoBlock;

    @ElementTitle("Полное наименование")
    @FindBy(xpath = "//div[contains(@data-testid, 'fullName')]/..")
    public TextInput fullNameTextInput;

    @ElementTitle("Краткое наименование")
    @FindBy(xpath = "//div[contains(@data-testid, 'shortName')]/..")
    public TextInput snameTextInput;

    @ElementTitle("Тип образовательного учреждения")
    @FindBy(xpath = "//h3[@data-testid='SchoolInfo.value.organizationType']")
    public TextBlock schoolTypeTextBlock;

    @ElementTitle("Часовой пояс")
    @FindBy(xpath = "//*[text() = 'Часовой пояс']")
    public Select utcSelect;

    @ElementTitle("Регион")
    @FindBy(xpath = "//h3[@data-testid='SchoolInfo.value.region']")
    public TextBlock regionTextBlock;

    @ElementTitle("ИНН")
    @FindBy(xpath = "//p[@data-testid='SchoolInfo.value.inn']")
    public TextBlock innTextBlock;

    @ElementTitle("Идентификатор")
    @FindBy(xpath = "//p[@data-testid='SchoolInfo.value.identifier']")
    public TextBlock identifierTextBlock;

    @ElementTitle("Адрес")
    @FindBy(xpath = "//div[@data-testid='SchoolInfo.TextAreaAutosize.address']/parent::div")
    public TextInput addressTextInput;

    @ElementTitle("Изображение логотипа школы")
    @FindBy(xpath = "//div[contains(@class, 'AvatarStyled')]")
    public Image logoImage;

    @ElementTitle("Подсказка по загрузке логотипа")
    @FindBy(xpath = "//div[contains(@class, 'AvatarStyled')]/parent::div/p")
    public TextBlock logoInfoTextBlock;

    @ElementTitle("Удалить")
    @FindBy(xpath = "//button[node() = 'Удалить']")
    public Button logoDeleteButton;

    @ElementTitle("Да, удалить")
    @FindBy(xpath = Constants.MODAL_WINDOW + "//button[node() = 'Удалить']")
    public Button logoDeleteConfirmButton;

    @ElementTitle("Нет, не удалять")
    @FindBy(xpath = Constants.MODAL_WINDOW + "//button[node() = 'Не удалять']")
    public Button logoDeleteCancelButton;

    @ElementTitle("Загрузить")
    @FindBy(xpath = "//button[node()='Загрузить']")
    public FileInput logoTextInput;

    @ElementTitle("Активировать школу")
    @FindBy(xpath = "//button[node()='Активировать']")
    public Button activateSchoolButton;

    @ElementTitle("Пояснение об активации")
    @FindBy(xpath = "//p[@data-testid='SchoolInfo.text.activateSchoolDetail']")
    public TextBlock activateSchoolTextBlock;

    @ElementTitle("Профиль")
    @FindBy(xpath = "//button[@data-testid='Subject.PopupButton.TeacherProfileButton']")
    public Button csProfileButton;

    @ElementTitle("Аватар конфигуратора в окне")
    @FindBy(xpath = "//div[contains(@class,'TooltipAvatarStyled')]")
    public Image avatarMedia;

    @ElementTitle("ФИО конфигуратора в окне")
    @FindBy(xpath = "//h3[contains(@class,'NameStyled')]")
    public TextBlock roleNameTextBlock;

    @ElementTitle("Список конфигураторов")
    @FindBy(xpath = "//div[contains(@class, '_TeacherProfileLinkS')]")
    public List<ConfiguratorProfileListItem> configuratorProfileList;
}