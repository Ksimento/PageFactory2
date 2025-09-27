package ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.test_run;

import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.ButtonElement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.analitycs.AnalyticUtils;
import ru.sbt.edu_power.external_services.PropReader;
import ru.sbt.edu_power.external_services.shared.TaskExecutionStatus;
import ru.sbt.edu_power.external_services.jira.test_manager.test_run.model.Execution;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.slack.views.AbstractModal;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.regress_control.RegressControlView;
import ru.sbt.edu_power.assist_bot.tasks.test_run.regress.components.test_case_repartition.TestCaseRepartitionView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class TestRunReports {
    private final TestRunStorage testRunStorage;
    private final String repartitionButtonId = UUID.randomUUID().toString();
    private final String regressControlButtonId = UUID.randomUUID().toString();
    private int completedPercent;
    private final Supplier<String> autotestProgress;
    private final AtomicBoolean generateFinalReport;

    public TestRunReports(
            final TestRunStorage testRunStorage,
            final Supplier<String> autotestProgress,
            final AtomicBoolean generateFinalReport
    ) {
        this.testRunStorage = testRunStorage;
        registerRepartitionButton();
        this.autotestProgress = autotestProgress;
        this.generateFinalReport = generateFinalReport;
        registerRegressControlButton();
    }

    public String getReportColor() {
        final int red = (100 - (completedPercent * 2)) * 200 / 100;
        final int green = completedPercent < 50 ? 0 : ((completedPercent - 50) * 2) * 200 / 100;
        final int blue = 200 - (Math.abs(completedPercent - 50)) * 2;
        return String.format(
                "#%s%s%s",
                String.format("%2s", Integer.toHexString(Math.max(0, red))),
                String.format("%2s", Integer.toHexString(Math.min(255, green))),
                String.format("%2s", Integer.toHexString(blue))
        ).replace(" ", "0");
    }

    public List<LayoutBlock> getFullReport() {
        final List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(getCurrentProgress());
        blocks.add(getTestRunInfo());
        setAutotestProgress(blocks);
        blocks.add(new DividerBlock());
        blocks.add(getTeamData());
        if (testRunStorage.getStatus() != TaskExecutionStatus.SUCCESS) {
            blocks.add(new DividerBlock());
            if (getActiveQa() > 5) {
                blocks.add(getQa("Тестировщики на финишной прямой", 10));
                blocks.add(getOverloadedUsers());
            } else {
                blocks.add(getQa("Оставшиеся участники регресса", 10000));
            }
            blocks.add(getRepartitionButtonBlock());
            blocks.add(getRegressControlButtonBlock());
        }
        return blocks;
    }

    // Подсчёт текущего прогресса прохождения тест-сета
    private LayoutBlock getCurrentProgress() {
        final int total = testRunStorage.getLastSlice().size();
        final int completed = testRunStorage.getLastSlice()
                                            .getStatusToExecutionMap()
                                            .entrySet()
                                            .stream()
                                            .filter(e -> !AnalyticUtils.isInNotCompleteStatus(e.getKey()))
                                            .map(Map.Entry::getValue)
                                            .mapToInt(List::size)
                                            .reduce(Integer::sum)
                                            .orElse(0);
        completedPercent = total == 0 ? 0 : completed * 100 / total;
        final String progress = String.format(
                "Всего: %d; Завершено: %d (%d%%); Осталось пройти: %d",
                total,
                completed,
                completedPercent,
                total - completed
        );
        return SectionBlock.builder()
                           .text(MarkdownTextObject.builder().text(progress).build())
                           .build();
    }

    private LayoutBlock getTestRunInfo() {
        final String message = String.format(
                "<%s%s|%s> (%s); Версия: %s; Проект: %s",
                PropReader.get("jira.ui.testrun.endpoint"),
                testRunStorage.getTestRunModel().getKey(),
                testRunStorage.getTestRunModel().getName(),
                testRunStorage.getTestRunModel().getKey(),
                testRunStorage.getTestRunModel().getJiraVersionModel().getName(),
                testRunStorage.getTestRunModel().getProject().name()
        );
        return SectionBlock.builder()
                           .text(MarkdownTextObject.builder().text(message).build())
                           .build();
    }

    private void setAutotestProgress(final List<LayoutBlock> blocks) {
        final String progress = autotestProgress.get();
        if (Objects.nonNull(progress) && !progress.isEmpty()) {
            blocks.add(SectionBlock.builder()
                                   .text(MarkdownTextObject.builder().text(progress).build())
                                   .build());
        }
    }

    // формирование блока со списком команд и количеством непройденных тест-кейсов на команде
    private LayoutBlock getTeamData() {
        final List<String> lines = new ArrayList<>();
        if (testRunStorage.getStatus() == TaskExecutionStatus.SUCCESS) {
            lines.add("Все тест-кейсы пройдены!");
        } else {
            lines.add("Осталось пройти тест-кейсов по командам");
            final Map<String, Integer> teamToTestNumberMap = new HashMap<>();
            testRunStorage
                    .getLastSlice()
                    .getTeamToExecutionStatusToToExecutionsMap()
                    .forEach((team, map) ->
                            collectSections(team, map, teamToTestNumberMap, 1000)
                    );
            teamToTestNumberMap.entrySet()
                               .stream()
                               .sorted(Map.Entry.comparingByKey())
                               .forEach(e -> lines.add(String.format("*%s*: _%d_", e.getKey(), e.getValue())));
        }
        return SectionBlock.builder()
                           .text(MarkdownTextObject.builder().text(String.join("\n", lines)).build())
                           .build();
    }

    // формирование списка тестировщиков, которые заканчивают регресс
    private LayoutBlock getQa(final String title, final int lowerThan) {
        final List<String> lines = new ArrayList<>();
        lines.add(title);
        final Map<String, Integer> userToTestNumberMap = new HashMap<>();
        testRunStorage
                .getLastSlice()
                .getUserToExecutionStatusToExecutionsMap()
                .forEach((user, map) -> collectSections(user.getDisplayName(), map, userToTestNumberMap, lowerThan));

        userToTestNumberMap
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(5)
                .forEach(e -> lines.add(
                        String.format("*%s*: _%d_", e.getKey(), e.getValue())
                ));
        return SectionBlock.builder()
                           .text(MarkdownTextObject.builder().text(String.join("\n", lines)).build())
                           .build();
    }


    // генерация списка пользователей или команд из мапы регресса
    // используется фильтр по непройденным тест-кейсам и по максимально допустимому числу этих тест-кейсов
    private void collectSections(
            final String name,
            final Map<String, List<Execution>> data,
            final Map<String, Integer> lines,
            final int lowerThan
    ) {
        final int total = data.keySet()
                              .stream()
                              .filter(AnalyticUtils::isInNotCompleteStatus)
                              .map(data::get)
                              .mapToInt(List::size)
                              .reduce(Integer::sum)
                              .orElse(0);
        if (total == 0 || total > lowerThan) {
            return;
        }
        lines.put(name, total);
    }

    private LayoutBlock getOverloadedUsers() {
        final TestAllocationCalc testAllocationCalc = new TestAllocationCalc();
        testAllocationCalc.load(testRunStorage.getLastSlice());
        final String overloadedUsers = testAllocationCalc.mapToString(testAllocationCalc.getOverloadedUsers());
        final String message;
        if (overloadedUsers.isEmpty()) {
            message = "Тестировщики загружены относительно равномерно";
        } else {
            message = "Перегруженные тестировщики:\n" + overloadedUsers;
        }
        return SectionBlock.builder()
                           .text(MarkdownTextObject.builder().text(message).build())
                           .build();
    }

    private int getActiveQa() {
        return (int) testRunStorage.getLastSlice()
                                   .getUserToExecutionStatusToExecutionsMap()
                                   .entrySet()
                                   .stream()
                                   .filter(e -> e
                                            .getValue()
                                            .keySet()
                                            .stream()
                                            .anyMatch(AnalyticUtils::isInNotCompleteStatus))
                                   .count();
    }

    @Nullable
    private LayoutBlock getRepartitionButtonBlock() {
        return SectionBlock.builder()
                           .accessory(getRepartitionButton())
                           .text(MarkdownTextObject
                                   .builder()
                                   .text("Здесь можно переместить наборы тест-кейсов между тестировщиками")
                                   .build())
                           .build();
    }

    private LayoutBlock getRegressControlButtonBlock() {
        return SectionBlock.builder()
                           .accessory(getRegressControlButton())
                           .text(MarkdownTextObject
                                   .builder()
                                   .text("Управление регрессионным прогоном")
                                   .build())
                           .build();
    }

    private ButtonElement getRepartitionButton() {
        return ButtonElement.builder()
                            .actionId(repartitionButtonId)
                            .text(PlainTextObject.builder().text("Переместить тест-кейсы").build())
                            .value("move")
                            .build();
    }

    private ButtonElement getRegressControlButton() {
        return ButtonElement.builder()
                            .actionId(regressControlButtonId)
                            .text(PlainTextObject.builder().text("Управление").build())
                            .value("regressControl")
                            .build();
    }

    private void registerRepartitionButton() {
        final Function<String, AbstractModal> modalFunction = (channelId) -> new TestCaseRepartitionView(
                testRunStorage.getLastSlice(),
                channelId
        );
        registerButton(modalFunction, repartitionButtonId);
    }

    private void registerRegressControlButton() {
        final Function<String, AbstractModal> modalFunction = (channelId) -> new RegressControlView(generateFinalReport);
        registerButton(modalFunction, regressControlButtonId);
    }

    private void registerButton(final Function<String, AbstractModal> modalFunction, final String buttonId) {
        Main.SLACK_DISPATCHER.getApp().blockAction(buttonId, (req, ctx) -> {
            final AbstractModal modal = modalFunction.apply(req.getPayload().getChannel().getId());
            modal.setUserId(req.getPayload().getUser().getId());
            modal.registerViewUpdate();
            modal.registerViewSubmit();
            final ViewsOpenResponse openResponse = ctx.client().viewsOpen(v -> v
                    .token(Main.BOT_TOKEN)
                    .triggerId(req.getPayload().getTriggerId())
                    .view(modal.getView())
            );
            if (!openResponse.isOk()) {
                log.error("{}", openResponse);
            }
            return ctx.ack();
        });
    }
}
