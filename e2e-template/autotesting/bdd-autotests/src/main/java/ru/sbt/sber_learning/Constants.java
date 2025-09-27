package ru.sbt.sber_learning;

import ru.sbt.edu_power.e2e_core.driver_utils.DriverConstants;
import ru.sbtqa.tag.qautils.properties.Props;

public class Constants {
    public static final long FREEZE_250_MS = DriverConstants.FREEZE_250_MS;
    public static final int TIMEOUT = DriverConstants.TIMEOUT;
    public static final int ELEMENT_WAIT_5SEC = DriverConstants.ELEMENT_WAIT_5SEC;
    public static final long FREEZE_500_MS = DriverConstants.FREEZE_500_MS;
    public static final long CONVERT_TO_MILLISECONDS = DriverConstants.CONVERT_TO_MILLISECONDS;
    public static final String BURGER_MENU_CONTAINER = "//div[@data-testid = 'WIDGETS.Sidebar']";
    public static final String POPOVER_CONTAINER = "//div[@class = 'tippy-content']";
    public static final String PRELOADER = "//*[name() = 'svg' and (contains(@class, 'Spinner') or @data-qa = 'preloader')]";
    public static final String MODAL_WINDOW = "//div[contains(@class, '_FullScreenPageLayout') " +
                                              "or @id = 'curtainContainer' " +
                                              "or @data-testid = 'UIKIT.Portal.ModalContent' " +
                                              "or @data-testid = 'UIKIT.Portal.ModalContent.Modal' " +
                                              "or contains(@class, '_ConfirmDialogContainer') " +
                                              "or contains(@class, 'MuiDialog-container') " +
                                              "or contains(@class, '_ModalWrapperStyled') " +
                                              "or @data-testid='UIKit.Curtain.CurtainContent' " +
                                              "or @id='image-crop-portal' " +
                                              "or @data-testid='CurtainV3' " +
                                              "or @data-testid='UIKIT.CurtainV3' " +
                                              "or contains(@class, 'MuiDrawer-modal') " +
                                              "or contains(@class,'MuiDialog-paper')]";
    public static final String NOTIFICATION_WRAPPER = "//div[contains(@class, 'NotificationPanel')]";
    public static final String USER_CONTEXT_MENU = "//div[contains(@class, '__ContextMenuStyled') or " +
                                                   "@data-testid = 'WIDGETS.Sidebar.user.context.menu']";
    public static final String FAIL_LOGIN_TOAST = Props.get("FAIL_LOGIN_TOAST");
    //  Информация о профиле ученика
    public static final String STUDENT_PUBLIC_INFO = "//div[contains(@class,'StudentPublicInfo')]";
    //  Список учетелей на форме профиля
    public static final String TEACHERS_MODAL_LIST = "//div[contains(@class,'StudentInfoBlock')]//div[contains(@class,'TeacherPersonalInfo')]";
    //  Виджет Помощь
    public static final String WIDGET_CONTEXT_HELP = "//button[contains(@data-testid,'Widgets.ContextHelp')]";
}