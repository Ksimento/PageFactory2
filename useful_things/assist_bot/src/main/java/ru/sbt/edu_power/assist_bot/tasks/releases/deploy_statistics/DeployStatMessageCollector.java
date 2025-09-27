package ru.sbt.edu_power.assist_bot.tasks.releases.deploy_statistics;

import com.slack.api.methods.response.files.FilesUploadResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.assist_bot.runner.Main;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleaseMessage;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ReleasesChannelReader;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.version_releases.Services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
class DeployStatMessageCollector {
    private ReleasesChannelReader releasesReader;
    private ReleasesChannelReader maintenanceReader;
    private final String userId;
    private boolean isDataCollected;
    private final List<ReleaseMessage> startedServices = new ArrayList<>();
    private final List<ReleaseMessage> failedServices = new ArrayList<>();
    private final List<ReleaseMessage> successfulServices = new ArrayList<>();
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> stands = new ArrayList<>();

    DeployStatMessageCollector(final String userId) {
        this.userId = userId;
        new Thread(this::collectData).start();
    }

    public void waitWhenDataBeCollected() {
        while (!isDataCollected) {
            ESUtils.freeze(500);
        }
    }

    public void setPeriod(final String startDate, final String endDate) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.startDate = LocalDate.from(formatter.parse(startDate));
        this.endDate = LocalDate.from(formatter.parse(endDate));
    }

    public void setFilterByStands(final List<String> stands) {
        this.stands.addAll(stands);
    }

    public void generateSortedCollections() {
        waitWhenDataBeCollected();
        log.info("Данные из всех каналов обработаны");
        startedServices.addAll(maintenanceReader.getStartServices());
        if (Objects.nonNull(startDate) && Objects.nonNull(endDate)) {
            startedServices.removeIf(ssm -> {
                final LocalDate messageDate = new Date(ssm.getTs() * 1000)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                return messageDate.isBefore(startDate) || messageDate.isAfter(endDate);
            });
        }
        startedServices.forEach(ssm -> {
            failedServices.addAll(maintenanceReader.getByBuildId(ssm.getBuildId()));
            successfulServices.addAll(releasesReader.getByBuildId(ssm.getBuildId()));
        });
        if (!stands.isEmpty()) {
            failedServices.removeIf(ssm -> !stands.contains(ssm.getEnv()));
            successfulServices.removeIf(ssm -> !stands.contains(ssm.getEnv()));
        }
        final byte[] xlsData = generateXlsFile();
        try {
            final FilesUploadResponse response = Main.SLACK_DISPATCHER.getApp().client().filesUpload(r -> r
                    .fileData(xlsData)
                    .channels(Collections.singletonList(userId))
                    .token(Main.BOT_TOKEN)
                    .filetype("xlsx")
                    .filename("deploy_stat.xlsx")
            );
            if (!response.isOk()) {
                log.error("{}", response);
            }
        } catch (final Throwable e) {
            throw new AssistBotException(e);
        }
    }

    private void collectData() {
        releasesReader = new ReleasesChannelReader(userId, ReleasesChannelReader.Channels.RELEASES, 4000);
        maintenanceReader = new ReleasesChannelReader(userId, ReleasesChannelReader.Channels.MAINTENANCE, 4000);
        releasesReader.updateMessages();
        maintenanceReader.updateMessages();
        isDataCollected = true;
    }

    private byte[] generateXlsFile() {
        try (final Workbook workbook = new XSSFWorkbook()) {
            addTotalStat(workbook);
            addUsersSheet(workbook);
            addServicesSheet(workbook);
            addStandSheet(workbook);
            addWeeklyStat(workbook);

            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                workbook.write(baos);
                return baos.toByteArray();
            }
        } catch (final IOException e) {
            log.error("", e);
            return new byte[]{};
        }
    }

    private void addTotalStat(final Workbook workbook) {
        final Sheet sheet = workbook.createSheet("Общая статистика");
        fillHeader(sheet, "");
        fillSheetData(sheet, getStat(m -> "Все установки"));

        final Row firstServiceRow = sheet.createRow(2);
        firstServiceRow.createCell(0).setCellValue("Установка первого сервиса");
        final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd-MM-yyyy");
        firstServiceRow.createCell(1).setCellValue(
                dateTimeFormatter.format(new Date(startedServices.get(startedServices.size() - 1).getTs() * 1000))
        );

        final Row lastServiceRow = sheet.createRow(3);
        lastServiceRow.createCell(0).setCellValue("Установка последнего сервиса");
        lastServiceRow.createCell(1).setCellValue(
                dateTimeFormatter.format(new Date(startedServices.get(0).getTs() * 1000))
        );
    }

    private void addUsersSheet(final Workbook workbook) {
        final Sheet sheet = workbook.createSheet("Автор деплоя");
        fillHeader(sheet, "Автор");
        fillSheetData(sheet, getStat(ReleaseMessage::getAuthor));
    }

    private void addServicesSheet(final Workbook workbook) {
        final Sheet sheet = workbook.createSheet("Сервис");
        fillHeader(sheet, "Сервис");
        fillSheetData(
                sheet,
                getStat(m -> m.getService() == Services.OTHER ? m.getRowServiceName() : m.getService().getServiceName())
        );
    }

    private void addStandSheet(final Workbook workbook) {
        final Sheet sheet = workbook.createSheet("Стенд");
        fillHeader(sheet, "Стенд");
        fillSheetData(sheet, getStat(ReleaseMessage::getEnv));
    }

    private void addWeeklyStat(final Workbook workbook) {
        final Sheet sheet = workbook.createSheet("Динамика");
        fillHeader(sheet, "Неделя года");
        fillSheetData(sheet, getStat(m ->
                String.valueOf(
                        new Date(m.getTs() * 1000)
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .get(ChronoField.ALIGNED_WEEK_OF_YEAR))
        ));
    }

    private Map<String, Stat> getStat(final Function<ReleaseMessage, String> function) {
        final Map<String, Stat> map = new HashMap<>();
        failedServices.forEach(s -> {
            final String nameFromFunction = function.apply(s);
            final String name = Objects.isNull(nameFromFunction) ? "Пустой параметр" : nameFromFunction;
            if (!map.containsKey(name)) {
                map.put(name, new Stat(name));
            }
            map.get(name).addFail(s);
        });
        successfulServices.forEach(s -> {
            final String nameFromFunction = function.apply(s);
            final String name = Objects.isNull(nameFromFunction) ? "Пустой параметр" : nameFromFunction;
            if (!map.containsKey(name)) {
                map.put(name, new Stat(name));
            }
            map.get(name).addSuccess(s);
        });
        return map;
    }

    private void fillHeader(final Sheet sheet, final String firstHeaderName) {
        sheet.setColumnWidth(0, 8448);
        final Row row = sheet.createRow(0);
        row.createCell(0).setCellValue(firstHeaderName);
        row.createCell(1).setCellValue("Всего");
        row.createCell(2).setCellValue("Успех");
        row.createCell(3).setCellValue("Успех %");
        row.createCell(4).setCellValue("Провал");
        row.createCell(5).setCellValue("Провал %");
    }

    private void fillSheetData(final Sheet sheet, final Map<String, Stat> dataMap) {
        final AtomicInteger rowCounter = new AtomicInteger(1);

        dataMap.entrySet().stream()
               .sorted(Map.Entry.comparingByKey())
               .forEach(e -> {
                   final Row row = sheet.createRow(rowCounter.getAndIncrement());
                   row.createCell(0).setCellValue(e.getKey());
                   row.createCell(1).setCellValue(e.getValue().getAll());
                   row.createCell(2).setCellValue(e.getValue().getSuccessfulServices().size());
                   row.createCell(3).setCellValue(e.getValue().getSuccessfulPercent());
                   row.createCell(4).setCellValue(e.getValue().getFailedServices().size());
                   row.createCell(5).setCellValue(e.getValue().getFailedPercent());
               });
    }

    @Getter
    private static class Stat {
        private final String param;
        private final List<ReleaseMessage> failedServices = new ArrayList<>();
        private final List<ReleaseMessage> successfulServices = new ArrayList<>();

        Stat(final String param) {
            this.param = param;
        }

        public void addSuccess(final ReleaseMessage message) {
            successfulServices.add(message);
        }

        public void addFail(final ReleaseMessage message) {
            failedServices.add(message);
        }

        public int getAll() {
            return failedServices.size() + successfulServices.size();
        }

        public int getSuccessfulPercent() {
            final int total = getAll();
            return total == 0 ? 0 : successfulServices.size() * 100 / total;
        }

        public int getFailedPercent() {
            final int total = getAll();
            return total == 0 ? 0 : failedServices.size() * 100 / total;
        }
    }
}
