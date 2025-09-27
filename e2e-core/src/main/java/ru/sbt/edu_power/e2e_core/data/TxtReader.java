package ru.sbt.edu_power.e2e_core.data;

import org.junit.Assert;
import ru.sbt.edu_power.external_services.validator.Validator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TxtReader implements HasRows {
    private final Path source;

    public TxtReader(final String source) {
        this.source = Paths.get(source);
    }

    @Override
    public boolean findFirstRow(final List<String> data) {
        Assert.assertEquals(
                "Для текстового формата принимается строка только из одного значения",
                data.size(),
                1
        );
        return getRows().stream().anyMatch(row -> Validator.matchValues(row.trim(), data.get(0)));
    }

    @Override
    public int getRowsSize() {
        return getRows().size();
    }

    private List<String> getRows() {
        try {
            return Files.readAllLines(source);
        } catch (final IOException e) {
            throw new DataProcessingException(e);
        }
    }
}
