package ru.sbt.edu_power.e2e_core.data;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.sbt.edu_power.external_services.validator.Validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class XlsReader implements HasRows {
    private final File xlsSource;
    private final int pageNumber;
    private final String fileType;

    public XlsReader(final String fileName, final int pageNumber) {
        this.xlsSource = new File(fileName);
        this.pageNumber = pageNumber - 1;
        fileType = FilenameUtils.getExtension(fileName);
    }

    @Override
    public boolean findFirstRow(final List<String> data) {
        data.removeIf(String::isEmpty);
        return initiateReader(sheet -> {
            for (final Row row : sheet) {
                final List<String> expected = new ArrayList<>(data);
                for (final Cell cell : row) {
                    final String cellValue;
                    final CellType cellType = cell.getCellType();
                    switch (cellType) {
                        case STRING:
                            cellValue = cell.getStringCellValue();
                            break;
                        case NUMERIC:
                            cellValue = Double.toString(cell.getNumericCellValue()).replace(".0", "");
                            break;
                        case BOOLEAN:
                            cellValue = Boolean.toString(cell.getBooleanCellValue());
                            break;
                        case BLANK:
                            cellValue = "";
                            break;
                        default:
                            throw new DataProcessingException("Не поддерживаемый тип ячейки: " + cellType.name());
                    }

                    if (Validator.matchValues(cellValue.replaceAll("\n", " ").trim(), expected.get(0))) {
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
        return initiateReader(sheet -> {
            int counter = 0;
            for (final Row row : sheet) {
                if (row.getCell(row.getFirstCellNum()).getCellType() == CellType.BLANK) {
                    continue;
                }
                counter++;
            }
            return counter;
        });
    }

    private <T> T initiateReader(final Function<Sheet, T> function) {
        try (final FileInputStream fis = new FileInputStream(xlsSource);
             final Workbook workbook = "xlsx".equals(fileType) ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)
        ) {
            final Sheet sheet;
            if ("xlsx".equals(fileType)) {
                sheet = ((XSSFWorkbook) workbook).getSheetAt(pageNumber);
            } else {
                sheet = ((HSSFWorkbook) workbook).getSheetAt(pageNumber);
            }
            return function.apply(sheet);
        } catch (final IOException e) {
            throw new DataProcessingException(e);
        }
    }
}
