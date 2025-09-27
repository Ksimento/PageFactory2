package ru.sbt.edu_power.assist_bot.tasks.releases.deploy_statistics;

import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.sections.DatePickerSection;
import ru.sbt.edu_power.assist_bot.tasks.releases.deploy_statistics.elements.FilterStandMultiSelectSection;

public class DeployStatView extends AbstractModal {
    private final DatePickerSection startDate = new DatePickerSection("Дата начала");

    private final DatePickerSection endDate = new DatePickerSection(
            "Дата завершения",
            true,
            () -> startDate.getAccessory().isFilled()
    );

    private final FilterStandMultiSelectSection filterStandMultiSelectSection = new FilterStandMultiSelectSection(
            true,
            () -> endDate.getAccessory().isFilled()
    );

    @Override
    public String getName() {
        return "Успешные установки";
    }

    @Override
    public void registerViewSubmit() {
        registerViewSubmit(
                () -> {
                    final DeployStatMessageCollector messageCollector = new DeployStatMessageCollector(getUserId());
                    messageCollector.setPeriod(
                            startDate.getAccessory().getValue(),
                            endDate.getAccessory().getValue()
                    );
                    messageCollector.setFilterByStands(filterStandMultiSelectSection.getAccessory().getValues());
                    messageCollector.generateSortedCollections();
                }
        );
    }
}
