package ru.sbt.edu_power.assist_bot.slack.views.fields.base;

import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.StaticSelectElement;
import com.slack.api.model.view.ViewState;

import java.util.List;
import java.util.Objects;

public abstract class AbstractSelectFormField extends AbstractFormField {

    @Override
    public void setState(final ViewState.Value value) {
        if (
                Objects.isNull(value) ||
                Objects.isNull(value.getSelectedOption()) ||
                Objects.isNull(value.getSelectedOption().getValue()) ||
                "none".equals(value.getSelectedOption().getValue())
        ) {
            clearState();
        } else {
            setValue(value.getSelectedOption().getValue());
        }
    }

    protected StaticSelectElement getSelect(
            final String name,
            final List<OptionObject> options
    ) {
        return StaticSelectElement.builder()
                                  .actionId(getId())
                                  .placeholder(asText(name, false))
                                  .options(options)
                                  .build();
    }
}
