package ru.sbt.edu_power.e2e_core.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CsvReader implements HasRows {
    private final File csvSource;
    private final char delimiter;

    public CsvReader(final String fileName, final char delimiter) {
        this.csvSource = new File(fileName);
        this.delimiter = delimiter;
    }

    @Override
    public boolean findFirstRow(final List<String> data) {
        return initiateReader((document) -> {
            for (final CSVRecord row : document) {
                final List<String> expected = new ArrayList<>(data);
                for (final String value : row) {
                    if (value.replaceAll("\\ufeff", "").equals(expected.get(0))) {
                        expected.remove(0);
                    }
                    if (expected.isEmpty()) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    @Override
    public int getRowsSize() {
        return initiateReader(document -> {
            int counter = 0;
            for (final CSVRecord strings : document) {
                counter++;
            }
            return counter;
        });
    }

    private <T> T initiateReader(final Function<CSVParser, T> function) {
        try (final CSVParser parser = CSVParser.parse(csvSource, StandardCharsets.UTF_8, CSVFormat.newFormat(delimiter))) {
            return function.apply(parser);
        } catch (final IOException e) {
            throw new DataProcessingException(e);
        }
    }
}
