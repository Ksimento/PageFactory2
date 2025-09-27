package ru.sbt.edu_power.assist_bot.slack.views.fields.base;

import com.slack.api.model.block.element.DatePickerElement;
import com.slack.api.model.view.ViewState;

import java.util.Objects;

public abstract class AbstractDatePickerFormField extends AbstractFormField {

    @Override
    public void setState(final ViewState.Value value) {
        if (
                Objects.isNull(value) ||
                Objects.isNull(value.getSelectedDate())
        ) {
            clearState();
        } else {
            setValue(value.getSelectedDate());
        }
    }

    protected DatePickerElement getDatePickerElement() {
        return DatePickerElement.builder().actionId(getId()).build();
    }
}
