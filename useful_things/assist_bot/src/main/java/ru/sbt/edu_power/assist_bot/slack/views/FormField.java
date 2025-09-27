package ru.sbt.edu_power.assist_bot.slack.views;

import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.view.ViewState;

import java.util.List;

public interface FormField {
    boolean isFilled();
    String getValue();
    List<String> getValues();
    void setState(ViewState.Value value);
    BlockElement getElement();
    String getId();
    String getName();
    void clearState();
    boolean getBoolean();
}
