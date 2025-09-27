package ru.sbt.edu_power.assist_bot.slack.views;

import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractFormField;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public abstract class AbstractSection extends Element implements Section {
    private final String id = UUID.randomUUID().toString();
    private final AbstractFormField accessory;
    private BooleanSupplier[] constructConditions;
    private Boolean isAndCondition;

    protected AbstractSection(final AbstractFormField accessory) {
        this.accessory = accessory;
        if (accessory instanceof HasExternalLoadField) {
            ((HasExternalLoadField) accessory).callback();
        }
    }

    protected AbstractSection(
            final AbstractFormField accessory,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        this.accessory = accessory;
        if (accessory instanceof HasExternalLoadField) {
            ((HasExternalLoadField) accessory).callback();
        }
        this.isAndCondition = isAndCondition;
        this.constructConditions = constructConditions;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean constructCondition() {
        if (isAndCondition == null) {
            return true;
        }
        return isAndCondition ? Stream.of(constructConditions).allMatch(BooleanSupplier::getAsBoolean) :
                Stream.of(constructConditions).anyMatch(BooleanSupplier::getAsBoolean);
    }

    @Override
    public FormField getAccessory() {
        return accessory;
    }
}
