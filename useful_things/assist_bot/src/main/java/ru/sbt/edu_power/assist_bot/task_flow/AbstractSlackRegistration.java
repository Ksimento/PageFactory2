package ru.sbt.edu_power.assist_bot.task_flow;

import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.ActionContext;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewTitle;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.slack.views.Element;
import ru.sbt.edu_power.assist_bot.slack.views.Modal;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractSlackRegistration extends Element implements SlackRegistered {
    public static final App APP = Main.SLACK_DISPATCHER.getApp();
    public static final String BOT_TOKEN = Main.BOT_TOKEN;
    private final String buttonId = UUID.randomUUID().toString();
    private String userId;

    @Override
    public String getButtonId() {
        return buttonId;
    }

    protected LayoutBlock getStartButton(
            final String buttonText,
            final String description
    ) {
        final ButtonElement buttonElement = ButtonElement.builder()
                                                         .actionId(getButtonId())
                                                         .text(asText(buttonText, false))
                                                         .build();
        return SectionBlock.builder()
                           .text(asText(
                                   description,
                                   false
                           ))
                           .accessory(buttonElement)
                           .build();
    }

    protected void registerStartButton(final Supplier<AbstractModal> modal) {
        log.info("Регистрация базового модуля '{}' ID: {}", getName(), getButtonId());
        APP.blockAction(getButtonId(), (req, ctx) -> {
            userId = req.getPayload().getUser().getId();
            return registerStartButton(ctx, modal.get());
        });
    }

    private Response registerStartButton(final ActionContext ctx, final Modal modal)
            throws IOException, SlackApiException {
        modal.setUserId(userId);
        modal.registerViewUpdate();
        modal.registerViewSubmit();
        final View view = modal.getView();
        if (view.getTitle().getText().length() > 25) {
            view.setTitle(ViewTitle.builder().type("plain_text").text(view.getTitle().getText().substring(0, 24)).build());
            log.error("Внимание! Название модального окна обрезано до 25 символов");
        }
        final ViewsOpenResponse r = ctx.client()
                                       .viewsOpen(v -> v
                   .triggerId(ctx.getTriggerId())
                   .token(BOT_TOKEN)
                   .view(view)
           );
        if (!r.isOk()) {
            log.error("{}", r);
        }
        return ctx.ack();
    }

    @Override
    public boolean isDemon() {
        return false;
    }

    @Override
    public IDemon getDemon() {
        return null;
    }
}
