package ru.sbt.edu_power.assist_bot.runner;

import com.slack.api.methods.response.views.ViewsPublishResponse;
import com.slack.api.model.User;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.event.MessageChangedEvent;
import com.slack.api.model.view.View;
import com.slack.api.model.view.Views;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.roles.roles.SlackRoles;
import ru.sbt.edu_power.assist_bot.slack.users.SlackUsers;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackDemonRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.AbstractSlackRegistration;
import ru.sbt.edu_power.assist_bot.task_flow.SlackRegistered;
import ru.sbt.edu_power.assist_bot.task_flow.StartSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ServiceRegistration {
    private final Map<SlackRegistered, SlackRoles[]> buttons = new HashMap<>();

    public void register() {
        final Set<Class<? extends SlackRegistered>> registrationClassSet =
                new Reflections(new ConfigurationBuilder().forPackages("ru.sbt.edu_power.assist_bot.tasks")).getSubTypesOf(
                        SlackRegistered.class);
        registrationClassSet.forEach(clazz -> {
            if (
                    clazz.isAssignableFrom(AbstractSlackRegistration.class)
                    || clazz.isAssignableFrom(AbstractSlackDemonRegistration.class)
            ) {
                return;
            }
            final SlackRegistered instance;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (final Throwable e) {
                throw new AssistBotException(e);
            }
            if (instance.isDemon()) {
                new Thread(instance.getDemon()::init, instance.getName()).start();
            } else {
                buttons.put(instance, instance.acceptedRoles());
                for (final SlackRoles role : instance.acceptedRoles()) {
                    Main.USER_ROLES_REPOSITORY.acceptModule(role, instance.getName());
                }
                instance.registerStartButton();
            }

        });
    }

    public void registerInitCommand() {
        Main.SLACK_DISPATCHER.getApp().event(AppHomeOpenedEvent.class, (req, ctx) -> {
            final View appHomeView = Views.view(v -> v
                    .type("home")
                    .blocks(getAcceptedBlocks(req.getEvent().getUser()))
            );
            final ViewsPublishResponse res = ctx.client().viewsPublish(r -> r
                    .userId(req.getEvent().getUser())
                    .token(Main.BOT_TOKEN)
                    .view(appHomeView)
            );
            if (!res.isOk()) {
                log.error("{}", res);
            }
            return ctx.ack();
        });
    }

    private List<LayoutBlock> getAcceptedBlocks(final String userId) {
        final User user = SlackUsers.getInstance().getUser(userId);
        final EnumMap<StartSection, List<SlackRegistered>> splitBySections = new EnumMap<>(StartSection.class);

        buttons.entrySet()
               .stream()
               .filter(e -> Main.USER_ROLES_REPOSITORY.hasAny(user, e.getValue()))
               .map(Map.Entry::getKey)
               .forEach(sr -> {
                   if (!splitBySections.containsKey(sr.getStartSection())) {
                       splitBySections.put(sr.getStartSection(), new ArrayList<>());
                   }
                   splitBySections.get(sr.getStartSection()).add(sr);
               });

        final List<LayoutBlock> blocks = new ArrayList<>();

        splitBySections.forEach((k, v) -> {
            v.sort(Comparator.comparing(SlackRegistered::getName));
            blocks.add(
                    SectionBlock.builder()
                                .text(MarkdownTextObject.builder().text("*" + k.getDesc() + "*").build())
                                .build()
            );
            v.forEach(sr -> blocks.add(sr.getStartButton()));
        });

        return blocks;
    }

    public void registerEmptyMessageChangeEvent() {
        Main.SLACK_DISPATCHER.getApp().event(MessageChangedEvent.class, (req, ctx) -> ctx.ack());
    }
}
