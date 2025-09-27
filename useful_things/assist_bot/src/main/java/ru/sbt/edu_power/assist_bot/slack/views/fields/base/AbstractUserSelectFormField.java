package ru.sbt.edu_power.assist_bot.slack.views.fields.base;

import com.slack.api.model.block.element.UsersSelectElement;
import com.slack.api.model.view.ViewState;

public abstract class AbstractUserSelectFormField extends AbstractFormField {
    @Override
    public void setState(final ViewState.Value value) {
        if (value.getSelectedUser() != null) {
            setValue(value.getSelectedUser());
        }
    }

    protected UsersSelectElement getUserSelect(final String text) {
        return UsersSelectElement.builder()
                .actionId(getId())
                .placeholder(asText(text, false))
                .build();
    }
}
