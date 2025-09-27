package ru.sbt.edu_power.e2e_core.data;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class XlsWriter {
    private final String fileName;
    private final int pageNumber;
    private final String fileType;

    public XlsWriter(final String fileName, final int pageNumber, final String fileType) {
        this.fileName = fileName;
        this.pageNumber = pageNumber - 1;
        this.fileType = fileType;
    }

    public void fileWrite(final List<List<String>> data) {
        final String filePath = getFilePath(System.getProperty("files.path") + File.separator + fileName);

        try (final FileInputStream input = new FileInputStream(filePath)
        ) {
            try(final Workbook book = "xlsx".equals(fileType) ? new XSSFWorkbook(input) : new HSSFWorkbook(input)) {
                final Sheet sheet;
                if ("xlsx".equals(fileType)) {
                    sheet = ((XSSFWorkbook) book).getSheetAt(pageNumber);
                } else {
                    sheet = ((HSSFWorkbook) book).getSheetAt(pageNumber);
                }
                for (final List<String> str : data) {
                    final String[] rowAndCell = str.get(0).split(":");
                    final Row row = sheet.getRow(Integer.parseInt(rowAndCell[0]));
                    final Cell cell = row.createCell(Integer.parseInt(rowAndCell[1]));
                    final String value = DataProcessing.decodeValue(str.get(1));
                    cell.setCellValue(value);
                }
                try (final FileOutputStream output = new FileOutputStream(filePath)) {
                    book.write(output);
                    book.close();
                }
            }
        } catch (final IOException e) {
            throw new DataProcessingException(e);
        }
    }

    private String getFilePath(final String fileName) {
        if (getResourceFromClasspath(fileName) != null) {
            return getResourceFromClasspath(fileName).getPath();
        }
        final File file = new File(fileName);
        return file.getAbsolutePath();
    }

    public static URL getResourceFromClasspath(final String fileName) {
        return Thread.currentThread().getContextClassLoader().getResource(fileName);
    }
}