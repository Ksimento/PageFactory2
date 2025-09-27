package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.app_backend.interactive_components.response.Option;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElement;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractHasExternalLoadFormField;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Секция для модалки с возможностью выбора стенда для деплоя
 */
public class StandSelectSection extends AbstractSection {
    private final String name;

    public StandSelectSection(final String name) {
        super(new Accessory());
        this.name = name;
    }

    public StandSelectSection(
            final String name,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
        this.name = name;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .text(asText(name, false))
                           .blockId(getId())
                           .accessory(getAccessory().getElement())
                           .build();
    }

    private static class Accessory extends AbstractHasExternalLoadFormField {
        private final String name = "dev0-0";

        @Override
        public BlockElement getElement() {
            return getSelect(name, 1);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void callback() {
            final List<Option> stands = getStandList();
            APP.blockSuggestion(getId(), (req, ctx) -> {
                final String keyWord = req.getPayload().getValue();
                final List<Option> suggestion = stands.stream()
                        .filter(s -> ((PlainTextObject) s.getText()).getText().contains(keyWord))
                        .limit(20)
                        .collect(Collectors.toList());

                return ctx.ack(r -> r.options(suggestion));
            });
        }

        private List<Option> getStandList() {
            return IntStream.of(1, 2)
                            .mapToObj(i -> "dev" + i + "-")
                            .flatMap(s ->
                                    IntStream.range(1, 100)
                                             .mapToObj(i -> s + i)
                            )
                            .map(s -> Option.builder().text(asText(s,false)).value(s).build())
                            .collect(Collectors.toList());
        }
    }
}
