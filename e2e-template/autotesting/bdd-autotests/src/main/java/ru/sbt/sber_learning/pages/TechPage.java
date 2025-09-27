package ru.sbt.sber_learning.pages;

import org.openqa.selenium.support.FindBy;
import ru.sbt.edu_power.e2e_core.blocks.system.SystemNotificationListItem;
import ru.sbt.edu_power.e2e_core.page_tags.ParentPage;
import ru.sbt.edu_power.e2e_core.smoke.EndPoints;
import ru.sbt.sber_learning.Constants;
import ru.sbt.sber_learning.blocks.main.ToastListItem;
import ru.sbt.sber_learning.pages.widgets.WUPS404;
import ru.sbtqa.tag.pagefactory.HTMLPage;
import ru.sbtqa.tag.pagefactory.annotations.ElementTitle;
import ru.sbtqa.tag.pagefactory.annotations.PageEntry;
import ru.yandex.qatools.htmlelements.element.TextBlock;

import java.util.List;

@PageEntry(title = "Техническая страница")
@ParentPage(ignored = true)
@EndPoints(ignored = true)
public class TechPage extends HTMLPage {
    @ElementTitle("Список всплывающих уведомлений")
    @FindBy(xpath = "//div[contains(@class, 'Toastify__toast-container')]/div")
    public List<ToastListItem> toastList;

    @ElementTitle("Панели вне контейнера контента")
    @FindBy(xpath = Constants.MODAL_WINDOW +
                    " | " +
                    Constants.NOTIFICATION_WRAPPER +
                    " | " +
                    Constants.USER_CONTEXT_MENU)
    public TextBlock extraContentContainerPanels;

    @ElementTitle("Модальное окно")
    @FindBy(xpath = Constants.MODAL_WINDOW)
    public TextBlock modalWrapper;

    // Оранжевые оповещения с пчёлкой в топе экрана
    @ElementTitle("Список оповещений")
    @FindBy(xpath = "//div[@id = 'notificationBlock']/div")
    public List<SystemNotificationListItem> systemNotificationListItem;

    @ElementTitle("Виджет УПС 404")
    @FindBy(xpath = "//div[contains(@class, '_ErrorMessageContainer') or contains(@class, '_PageNotFoundContainer')]")
    public WUPS404 wups404;
}
