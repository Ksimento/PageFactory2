package ru.sbt.edu_power.external_services.data;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.validator.Validator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

// Класс выполняет операции по архивированию и разархивированию данных
public class Archiver {
    // исходные данные
    private byte[] source;
    // архивированные данные
    private byte[] archive;

    public Archiver() {
    }

    // указать путь до файла архива
    public Archiver setArchivedFile(final String pathToFile) {
        archive = fileToByteArray(Paths.get(pathToFile));
        return this;
    }

    // указать путь до файла данных
    public Archiver setSourceFile(final String pathToFile) {
        source = fileToByteArray(Paths.get(pathToFile));
        return this;
    }

    // передать массив данных для архивирования
    public Archiver setSourceBytes(final byte[] source) {
        this.source = source;
        return this;
    }

    // передать массив данных для разорхивирования
    public Archiver setArchiveBytes(final byte[] archive) {
        this.archive = archive;
        return this;
    }

    // получить разархивированные данные в виде массива байт
    public byte[] sourceAsBytes() {
        return source;
    }

    // получить архив в виде массива байт
    public byte[] archiveAsBytes() {
        return archive;
    }

    // записать разархивированые данные на диск
    public void writeSource(final Path pathToWrite) {
        write(pathToWrite, source);
    }

    // записать архив на диск
    public void writeArchive(final Path pathToWrite) {
        write(pathToWrite, archive);
    }

    // заархивировать данные в zip
    public Archiver zipSource(final String zippedFileName) {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final ZipOutputStream zos = new ZipOutputStream(baos)
        ) {
            final ZipEntry entry = new ZipEntry(zippedFileName);
            entry.setSize(source.length);
            zos.putNextEntry(entry);
            zos.setLevel(9);
            zos.write(source);
            zos.closeEntry();
            archive = baos.toByteArray();
        } catch (final IOException e) {
            throw new ExternalServicesException(e);
        }
        return this;
    }

    // разархивировать данные из zip
    public Archiver unZipArchive(final String zippedFileName) {
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(archive);
             final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final ZipInputStream zis = new ZipInputStream(bais)
        ) {
            while (true) {
                final ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    throw new ExternalServicesException("В архиве нет нужного файла: " + zippedFileName);
                }
                if (Validator.matchValues(entry.getName(), zippedFileName)) {
                    break;
                }
            }
            int read;
            final byte[] bytesIn = new byte[1024];
            while ((read = zis.read(bytesIn)) != -1) {
                baos.write(bytesIn, 0, read);
            }
            zis.closeEntry();
            source = baos.toByteArray();
        } catch (final IOException e) {
            throw new ExternalServicesException(e);
        }
        return this;
    }

    // заархивировать данные в gzip
    public Archiver gzipSource() {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(baos)
        ) {
            gcos.write(source);
            gcos.close();
            archive = baos.toByteArray();
        } catch (final IOException e) {
            throw new ExternalServicesException(e);
        }
        return this;
    }

    // разархивировать данные из gzip
    public Archiver unGzipArchive() {
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(archive);
             final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final GzipCompressorInputStream gcis = new GzipCompressorInputStream(bais)
        ) {
            int read;
            final byte[] bytesIn = new byte[1024];
            while ((read = gcis.read(bytesIn)) != -1) {
                baos.write(bytesIn, 0, read);
            }
            source = baos.toByteArray();
        } catch (final IOException e) {
            throw new ExternalServicesException(e);
        }
        return this;
    }

    private void write(final Path pathToWrite, final byte[] data) {
        try {
            if (Files.exists(pathToWrite)) {
                Files.delete(pathToWrite);
            }
            Files.createDirectories(pathToWrite.getParent());
            Files.createFile(pathToWrite);
            Files.write(pathToWrite, data);
        } catch (final IOException e) {
            throw new ExternalServicesException(e);
        }
    }

    private byte[] fileToByteArray(final Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (final IOException e) {
            throw new ExternalServicesException(e);
        }
    }
}
