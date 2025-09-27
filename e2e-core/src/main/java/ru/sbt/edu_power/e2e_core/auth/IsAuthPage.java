package ru.sbt.edu_power.e2e_core.auth;

import ru.sbt.edu_power.e2e_core.elements.Button;
import ru.sbt.edu_power.e2e_core.elements.text_input.TextInput;
import ru.sbtqa.tag.pagefactory.Page;

public interface IsAuthPage extends Page {
    TextInput getLoginField();
    TextInput getPasswordField();
    Button getSubmitButton();
    boolean isAuthPageDisplayed();
}
