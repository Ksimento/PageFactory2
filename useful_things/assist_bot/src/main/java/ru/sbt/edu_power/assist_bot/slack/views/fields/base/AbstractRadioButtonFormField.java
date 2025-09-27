package ru.sbt.edu_power.assist_bot.slack.views.fields.base;

import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.RadioButtonsElement;
import com.slack.api.model.view.ViewState;

import java.util.List;

public abstract class AbstractRadioButtonFormField extends AbstractFormField {

    @Override
    public void setState(final ViewState.Value value) {
        if (value.getSelectedOption() != null) {
            setValue(value.getSelectedOption().getValue());
        }
    }

    protected RadioButtonsElement getRadioButtons(
            final List<OptionObject> options
    ) {
        return RadioButtonsElement.builder()
                                  .actionId(getId())
                                  .options(options)
                                  .build();
    }
}
