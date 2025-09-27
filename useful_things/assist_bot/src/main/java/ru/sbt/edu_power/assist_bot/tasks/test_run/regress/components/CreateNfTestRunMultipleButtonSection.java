package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components;

import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.tasks.default_tasks.ifaces.IHasProjectAndVersions;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.RegressStartView;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.NfTestRunCollectionCreate;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractButtonFormField;

import java.util.function.BooleanSupplier;

public class CreateNfTestRunMultipleButtonSection extends AbstractSection {
    private final IHasProjectAndVersions view;

    public CreateNfTestRunMultipleButtonSection(
            final IHasProjectAndVersions view
    ) {
        super(new Accessory(view));
        this.view = view;
    }

    public CreateNfTestRunMultipleButtonSection(
            final IHasProjectAndVersions view,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(
                new Accessory(view),
                isAndCondition,
                constructConditions
        );
        this.view = view;
    }

    @Override
    public SectionBlock construct() {
        final String version = view.getTestRunVersion().getName();

        return SectionBlock.builder()
                           .text(asText("Создать наборы пустых тест-сетов на все стори из версии " + version, false))
                           .accessory(getAccessory().getElement())
                           .blockId(getId())
                           .build();
    }

    private static class Accessory extends AbstractButtonFormField {
        private final String name = "Создать набор тест-сетов";
        private final IHasProjectAndVersions view;

        Accessory(final IHasProjectAndVersions view) {
            this.view = view;
        }

        @Override
        public BlockElement getElement() {
            return getButton(name, "create");
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void buttonCallback() {

            final NfTestRunCollectionCreate nfTestRunCollectionCreate = new NfTestRunCollectionCreate(
                    view.getJiraProjectId(),
                    view.getTestRunVersion()
            );
            ((RegressStartView) view).getNfTestRunCollectionCreateContainer().setObject(nfTestRunCollectionCreate);
            nfTestRunCollectionCreate.create();
        }
    }
}
