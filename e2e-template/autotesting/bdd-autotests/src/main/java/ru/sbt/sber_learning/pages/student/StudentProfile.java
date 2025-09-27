package ru.sbt.sber_learning.pages.student;

import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.elements.Button;
import ru.sbt.sber_learning.pages.TechPage;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.annotations.PageEntry;
import ru.yandex.qatools.htmlelements.element.TextBlock;

@PageEntry(title = "Ученик - Профиль")
public class StudentProfile extends TechPage {
    @ElementTitle("Контейнер страницы")
    @FindBy(xpath = "//div[contains(@class, '_ProfileContentWrapper')]")
    public TextBlock pageContainer;

    @ElementTitle("Информация о себе")
    @FindBy(xpath = "//div[@data-testid = 'Profile.ProfileInfo']")
    public TextBlock aboutMeTextBlock;

    @ElementTitle("Ромашка")
    @FindBy(xpath = "//div[contains(@class, '_ChamomileWrapperS')]")
    public TextBlock chamomileTextBlock;

    @ElementTitle("Мягкие навыки")
    @FindBy(xpath = "//button[node() = 'Мягкие навыки']")
    public Button softSkillsTextBlock;

    @ElementTitle("Паутинка")
    @FindBy(xpath = "//div[@data-testid = 'StudentProgress.Container']")
    public TextBlock softSkillsSvgTextBlock;
}
