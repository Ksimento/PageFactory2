package ru.sbt.edu_power.assist_bot.slack.views.sections;

import com.slack.api.app_backend.interactive_components.response.Option;
import com.slack.api.model.User;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.view.ViewState;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.slack.views.fields.base.AbstractHasExternalLoadFormField;
import ru.sbt.edu_power.assist_bot.slack.users.SlackUsers;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractSection;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

// Секция с селектом пользователей слака
@Slf4j
public class SlackUserSelectSection extends AbstractSection {
    private final String text;

    public SlackUserSelectSection(
            final String text
    ) {
        super(new Accessory());
        this.text = text;
    }

    public SlackUserSelectSection(
            final String text,
            final boolean isAndCondition,
            final BooleanSupplier... constructConditions
    ) {
        super(new Accessory(), isAndCondition, constructConditions);
        this.text = text;
    }

    @Override
    public SectionBlock construct() {
        return SectionBlock.builder()
                           .blockId(getId())
                           .accessory(getAccessory().getElement())
                           .text(asText(text, false))
                           .build();
    }

    private static class Accessory extends AbstractHasExternalLoadFormField {
        private final List<User> slackUsers = SlackUsers.getInstance().getUsers();
        private final String EMPTY_USER = "noname";

        @Override
        public void setState(final ViewState.Value value) {
            if (value.getSelectedOption() != null) {
                final String option = EMPTY_USER.equals(value.getSelectedOption().getValue()) ? "" : value
                        .getSelectedOption()
                        .getValue();
                setValue(option);
            }
        }

        @Override
        public BlockElement getElement() {
            return getSelect("Пользователь", 1);
        }

        @Override
        public String getName() {
            return "Выбери пользователя";
        }

        @Override
        public void callback() {
            APP.blockSuggestion(getId(), (req, ctx) -> {
                final String value = req.getPayload().getValue().toLowerCase();
                final String translit = Transliteration.isCyr(value) ? Transliteration.cyr2lat(value) : Transliteration.lat2cyr(
                        value);
                final List<Option> options = slackUsers
                        .stream()
                        .filter(user -> {
                            if (Objects.nonNull(user.getProfile().getRealName())) {
                                final String realName = user.getProfile().getRealName().toLowerCase();
                                return realName.contains(value) ||
                                       realName.contains(translit);
                            }
                            return false;
                        })
                        .limit(20)
                        .map(user -> Option
                                .builder()
                                .text(asText(user.getProfile().getRealName(), false))
                                .value(user.getId())
                                .build())
                        .collect(Collectors.toList());
                if (options.isEmpty()) {
                    options.add(Option.builder().text(asText("Пользователь не найден", false)).value(EMPTY_USER).build());
                }
                return ctx.ack(r -> r.options(options));
            });
        }
    }
}
