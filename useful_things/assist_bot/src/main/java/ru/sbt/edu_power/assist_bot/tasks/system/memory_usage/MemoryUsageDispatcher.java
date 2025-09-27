package ru.sbt.edu_power.assist_bot.tasks.system.memory_usage;

import com.slack.api.methods.response.files.FilesUploadResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.style.Styler;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.task_flow.IDispatcher;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MemoryUsageDispatcher implements IDispatcher {
    private final MemoryUsageView view;

    public MemoryUsageDispatcher(final MemoryUsageView view) {
        this.view = view;
    }

    @SneakyThrows
    @Override
    public void dispatch() {
        final LinkedHashMap<LocalDateTime, MemoryUsageSlice> data =
                MemoryUsageDemon
                        .getInstance()
                        .getTask()
                        .getMemoryUsageStorage()
                        .getTimeBasedData();
        final LocalDateTime start = data.keySet().iterator().next();
        final int[] xData = data.keySet()
                                   .stream()
                                   .map(t -> Duration.between(start, t))
                                   .map(Duration::toMinutes)
                                   .mapToInt(Long::intValue)
                                   .toArray();
        final int[] yDataUsage = data.values()
                                        .stream()
                                        .mapToInt(l -> (int)(l.get(0) / 1048576))
                                        .toArray();
        final int[] yDataTotal = data.values()
                                        .stream()
                                        .mapToInt(l -> (int)(l.get(1) / 1048576))
                                        .toArray();

        final List<List<Double>> allData = new ArrayList<>();

        final XYChart xyChart = new XYChart(800, 600, Styler.ChartTheme.Matlab);
        xyChart.addSeries("Mem total", xData, yDataTotal);
        xyChart.addSeries("Mem usage", xData, yDataUsage);
        xyChart.getStyler().setMarkerSize(1);

        final byte[] bitMap = BitmapEncoder.getBitmapBytes(xyChart, BitmapEncoder.BitmapFormat.PNG);
        final FilesUploadResponse response = Main.SLACK_DISPATCHER.getApp().client().filesUpload(r -> r
                .fileData(bitMap)
                .filename("memory_usage.png")
                .filetype("png")
                .token(Main.BOT_TOKEN)
                .channels(Collections.singletonList(view.getUserId()))
        );
        if (!response.isOk()) {
            log.error("{}", response);
        }
    }
}
