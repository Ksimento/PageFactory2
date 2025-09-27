package ru.sbt.edu_power.assist_bot.slack.views.fields.base;

import com.slack.api.model.block.element.ExternalSelectElement;
import com.slack.api.model.view.ViewState;
import ru.sbt.edu_power.assist_bot.slack.views.HasExternalLoadField;

public abstract class AbstractHasExternalLoadFormField extends AbstractFormField implements HasExternalLoadField {

    @Override
    public void setState(final ViewState.Value value) {
        if (value.getSelectedOption() != null) {
            setValue(value.getSelectedOption().getValue());
        }
    }

    protected ExternalSelectElement getSelect(
            final String name,
            final int minQueryLength
    ) {
        return ExternalSelectElement.builder()
                                    .actionId(getId())
                                    .placeholder(asText(name, false))
                                    .minQueryLength(minQueryLength)
                                    .build();
    }
}
