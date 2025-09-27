package ru.sbt.edu_power.assist_bot.slack.views.fields.base;

import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.element.CheckboxesElement;
import com.slack.api.model.view.ViewState;
import ru.sbt.edu_power.assist_bot.AssistBotException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractCheckBoxFormField extends AbstractFormField {
    private final List<String> values = new ArrayList<>();

    @Override
    public boolean isFilled() {
        return !values.isEmpty();
    }

    @Override
    public String getValue() {
        throw new AssistBotException("Поле не поддерживает получение единичного значения");
    }

    @Override
    public void setState(final ViewState.Value value) {
        values.clear();
        values.addAll(
                value.getSelectedOptions()
                .stream()
                .map(ViewState.SelectedOption::getValue)
                .collect(Collectors.toList())
        );
    }

    @Override
    public void clearState() {
        values.clear();
    }

    @Override
    public List<String> getValues() {
        return values;
    }

    public CheckboxesElement getCheckBoxList(final List<OptionObject> options) {
        return CheckboxesElement.builder()
                .actionId(getId())
                .options(options)
                .build();
    }
}
