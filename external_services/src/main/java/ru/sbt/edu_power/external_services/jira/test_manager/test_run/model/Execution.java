package ru.sbt.edu_power.external_services.jira.test_manager.test_run.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

// Класс описывает модель данных для execution тест-кейса
@Getter
@Setter
public class Execution {
    private String status;
    private Date actualStartDate;
    private Date actualEndDate;
    private String executedBy;
    private int executionTime;
    private String testCaseKey;
    private String assignedTo;
    private String version;
    private Integer id;
    private List<String> issueLinks;
    private final transient String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private final transient SimpleDateFormat format = new SimpleDateFormat(pattern);
    // для десериализации JSON в объект нужно использовать этот GSON объект, иначе даты будут некорректные
    public static final transient Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new GsonDateFormatAdapter())
            .create();

    public Execution() {
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @SneakyThrows
    public Execution(
            final String status,
            final String actualStartDate,
            final String actualEndDate,
            final String executedBy,
            final int executionTime,
            final String version
    ) {
        this.status = status;
        this.actualStartDate = format.parse(actualStartDate);
        this.actualEndDate = format.parse(actualEndDate);
        this.executedBy = executedBy;
        this.executionTime = executionTime;
        this.version = Objects.isNull(version) || version.isEmpty() ? null : version;
    }

    // очень важно для сериализации экзекушена использовать этот метод, иначе даты будут некорректные
    @Override
    public String toString() {
        return GSON.toJson(this);
    }

    public void setActualEndDate(final Date actualEndDate) {
        this.actualEndDate = actualEndDate;
    }

    public static class GsonDateFormatAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        private final DateFormat dateFormat;

        public GsonDateFormatAdapter() {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public synchronized JsonElement serialize(
                final Date date,
                final Type type,
                final JsonSerializationContext jsonSerializationContext
        ) {
            return new JsonPrimitive(dateFormat.format(date));
        }

        @Override
        public synchronized Date deserialize(
                final JsonElement jsonElement, final Type type,
                final JsonDeserializationContext jsonDeserializationContext
        ) {
            try {
                return dateFormat.parse(jsonElement.getAsString());
            } catch (final ParseException e) {
                throw new JsonParseException(e);
            }
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Execution execution = (Execution) o;

        if (executionTime != execution.executionTime) {
            return false;
        }
        if (!status.equals(execution.status)) {
            return false;
        }
        if (!Objects.equals(actualStartDate, execution.actualStartDate)) {
            return false;
        }
        if (!Objects.equals(actualEndDate, execution.actualEndDate)) {
            return false;
        }
        if (!Objects.equals(executedBy, execution.executedBy)) {
            return false;
        }
        if (!testCaseKey.equals(execution.testCaseKey)) {
            return false;
        }
        if (!Objects.equals(assignedTo, execution.assignedTo)) {
            return false;
        }
        if (!Objects.equals(version, execution.version)) {
            return false;
        }
        if (!id.equals(execution.id)) {
            return false;
        }
        return Objects.equals(issueLinks, execution.issueLinks);
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + (actualStartDate != null ? actualStartDate.hashCode() : 0);
        result = 31 * result + (actualEndDate != null ? actualEndDate.hashCode() : 0);
        result = 31 * result + (executedBy != null ? executedBy.hashCode() : 0);
        result = 31 * result + executionTime;
        result = 31 * result + testCaseKey.hashCode();
        result = 31 * result + (assignedTo != null ? assignedTo.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + id.hashCode();
        result = 31 * result + (issueLinks != null ? issueLinks.hashCode() : 0);
        return result;
    }
}
