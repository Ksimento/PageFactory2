package ru.sbt.edu_power.assist_bot.services.channel_reader;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.external_services.version_releases.Services;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Getter
public class ReleaseMessage implements Comparable<ReleaseMessage> {
    private boolean isStartService;
    private Services service;
    private String rowServiceName;
    private String version;
    private String env;
    private Status status;
    private String author;
    private String buildId;
    private final Long ts;

    public ReleaseMessage(final String ts) {
        this.ts = Long.valueOf(ts.split("\\.")[0]);
    }


    public void parse(final String message) {
        final List<String> rows = Arrays.asList(message.split("\n"));
        rows.forEach(row ->
                Stream.of(RowName.values()).forEach(rn -> {
                    if (row.startsWith(rn.getRowName())) {
                        setValue(row, rn);
                    }
                })
        );
    }

    private void setValue(final String row, final RowName rowName) {
        switch (rowName) {
            case STATUS:
                if (row.contains(Status.SUCCESS.name())) {
                    status = Status.SUCCESS;
                } else {
                    status = Status.FAILURE;
                }
                final String[] parts = row.split(" ");
                env = parts[parts.length - 1];
                break;
            case IS_START_SERVICE:
                isStartService = true;
                break;
            case DEPLOY_COMPLETE:
                final String[] partsDeployComplete = row.split(" ");
                env = partsDeployComplete[partsDeployComplete.length - 1];
                break;
            case SERVICE:
                rowServiceName = row.replace(RowName.SERVICE.getRowName(), "").trim();
                service = Services.getBySlackName(rowServiceName);
                break;
            case VERSION:
                version = row.replace(RowName.VERSION.getRowName(), "").trim();
                break;
            case AUTHOR:
                author = row.split(rowName.rowName)[1].trim();
                break;
            case BUILD_ID:
                buildId = row.split(rowName.rowName)[1].replace(" - ", "").replace("[:<>]", "").trim();
                break;
            default:
                throw new AssistBotException("Обработака поля не реализована " + rowName.name());
        }
    }

    @Override
    public int compareTo(@NotNull final ReleaseMessage o) {
        return o.getTs().compareTo(this.getTs());
    }

    public enum Status {
        SUCCESS,
        FAILURE
    }

    private enum RowName {
        IS_START_SERVICE("Произведён запуск установки"),
        STATUS("Статус"),
        SERVICE("Сервис:"),
        VERSION("Версия:"),
        DEPLOY_COMPLETE("Установка завершена на "),
        AUTHOR("Запустил пользователь:"),
        BUILD_ID("Ссылка на Jenkins JOB")
        ;
        private final String rowName;

        RowName(final String rowName) {
            this.rowName = rowName;
        }

        public String getRowName() {
            return rowName;
        }
    }
}
