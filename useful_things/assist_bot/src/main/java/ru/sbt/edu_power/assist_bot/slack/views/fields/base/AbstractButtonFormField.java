package ru.sbt.edu_power.assist_bot.slack.views.fields.base;

import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.view.ViewState;
import ru.sbt.edu_power.assist_bot.slack.views.HasButtonField;

public abstract class AbstractButtonFormField extends AbstractFormField implements HasButtonField {
    @Override
    public void setState(final ViewState.Value value) {
        // do nothing
    }

    protected ButtonElement getButton(final String text, final String value) {
        return ButtonElement.builder()
                            .actionId(getId())
                            .text(asText(text, true))
                            .value(value)
                            .build();
    }
}
