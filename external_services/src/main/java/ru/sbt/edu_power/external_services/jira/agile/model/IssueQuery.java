package ru.sbt.edu_power.external_services.jira.agile.model;

import lombok.Getter;
import org.junit.Assert;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Класс реализует конструктор поискового запроса в API JIRA
 */
public class IssueQuery {
    private String jql = "";
    private int startAt;
    private int maxResults;
    private final Set<String> fields = new HashSet<>();

    public IssueQuery(final int startAt, final int maxResults) {
        this.startAt = startAt;
        this.maxResults = maxResults;
    }

    public int getStartAt() {
        return startAt;
    }

    public void setStartAt(final int startAt) {
        this.startAt = startAt;
    }

    public IssueQuery setJql(final String jql) {
        this.jql = jql;
        return this;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(final int maxResults) {
        this.maxResults = maxResults;
    }

    public IssueQuery setFields(final String... fields) {
        this.fields.addAll(Arrays.asList(fields));
        return this;
    }

    public IssueQuery and(final IssueFields.Field field, final Op operator, final String... values) {
        return and(field.getForQuery(), operator, values);
    }

    public IssueQuery and(final String param, final Op operator, final String... values) {
        Assert.assertTrue("Количество значений values должно быть больше нуля", values.length > 0);
        final String connector;
        if (jql.isEmpty()) {
            connector = "";
        } else {
            connector = " AND ";
        }
        return append(connector, param, operator, values);
    }

    public IssueQuery or(final IssueFields.Field field, final Op operator, final String... values) {
        return or(field.getForQuery(), operator, values);
    }

    public IssueQuery or(final String param, final Op operator, final String... values) {
        Assert.assertTrue("Количество значений values должно быть больше нуля", values.length > 0);
        final String connector;
        if (jql.isEmpty()) {
            connector = "";
        } else {
            connector = " OR ";
        }
        return append(connector, param, operator, values);
    }

    private IssueQuery append(final String connector, final String param, Op operator, final String... values) {
        final String value;
        // Если пытаемся выполнить поиск по пустому значению, то нужно использовать конструкцию IN (EMPTY)
        if (values.length == 1 && values[0].isEmpty()) {
            operator = Op.IN;
            value = "(EMPTY)";
        } else {
            // Оператор IN работает только с набором значений в скобках (значение может быть одно)
            if (operator == Op.IN) {
                value = "(" + Stream.of(values).map(this::quot).collect(Collectors.joining(",")) + ")";
            } else {
                if (operator.name().contains("FUNCTION")) {
                    value = values[0];
                } else {
                    value = quot(values[0]);
                }
            }
        }
        jql = jql + connector + param + operator.opName + value;
        return this;
    }

    public String getJql() {
        return jql;
    }

    public Set<String> getFields() {
        return fields;
    }

    public enum Op {
        EQUAL(" = "),
        // используется в случае, если нужно использовать функцию в качестве значения фильтра, например currentUser()
        EQUAL_FUNCTION(" = "),
        NOT_EQUAL(" != "),
        CONTAIN(" ~ "),
        IN(" in "),
        MORE_OR_EQUALS(" >= "),
        IS(" is ");

        private final String opName;

        Op(final String opName) {
            this.opName = opName;
        }

        public String getOpName() {
            return opName;
        }
    }

    private String quot(final String value) {
        boolean isInteger = true;
        for (final byte s : value.getBytes()) {
            if (s < '0' || s > '9') {
                isInteger = false;
                break;
            }
        }
        return isInteger ? value : "\"" + value + "\"";
    }
}
