package ru.sbt.edu_power.assist_bot.slack.views.fields.base;

import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.CheckboxesElement;
import com.slack.api.model.view.ViewState;

import java.util.Collections;

public abstract class AbstractSingleCheckBoxFormField extends AbstractFormField {
    private boolean value;

    @Override
    public void setState(final ViewState.Value value) {
        if (value.getSelectedOptions() == null) {
            this.value = false;
        } else {
            this.value = !value.getSelectedOptions().isEmpty();
        }
    }

    @Override
    public boolean isFilled() {
        return true;
    }

    @Override
    public String getValue() {
        return value ? "true" : "false";
    }

    @Override
    public void clearState() {
        value = false;
    }

    @Override
    public boolean getBoolean() {
        return value;
    }

    public CheckboxesElement getCheckBox(final String name, final String description) {
        return CheckboxesElement.builder()
                                .actionId(getId())
                                .options(
                                        Collections.singletonList(OptionObject
                                                .builder()
                                                .text(asText(name, true))
                                                .value("true")
                                                .description(asText(description, true))
                                                .build())
                                ).build();
    }
}
