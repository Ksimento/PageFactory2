package ru.sbt.edu_power.e2e_core.elements.image;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import ru.sbt.edu_power.e2e_core.data.DataProcessing;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Класс реализует получение и валидацию специфических свойств для изображения
 * Доступные названия свойств для использования в сценариях:
 * Высота
 * Ширина
 * Тип
 */
public class ImageProperties {
    private final URL url;
    private final Path localPath;
    private final int width;
    private final int height;
    private final long fileSize;
    private final String fileType;

    public ImageProperties(final String url) {
        try {
            this.url = new URL(url);
            localPath = createLocalPath();
            final int timeout = 20000;
            FileUtils.copyURLToFile(this.url, localPath.toFile(), timeout, timeout);
            fileType = FilenameUtils.getExtension(localPath.toString());
            final BufferedImage image = ImageIO.read(localPath.toFile());
            width = image.getWidth();
            height = image.getHeight();
            fileSize = Files.size(localPath);
        } catch (final IOException e) {
            throw new AutotestError(e);
        }
    }

    private Path createLocalPath() throws IOException {
        final Path path = Paths.get(System.getProperty("target.directory")
                                    + File.separator
                                    + "browser_download"
                                    + File.separator
                                    + DataProcessing.generator("word8")
                                    + File.separator
                                    + getFileNameFromUrl(url)
        );
        Files.createDirectories(path.getParent());
        return path;
    }

    public String getPropertyValue(final String property) {
        return getPropertyValue(ImagePropertiesType.getEnumByName(property));
    }

    public String getPropertyValue(final ImagePropertiesType type) {
        switch (type) {
            case WIDTH:
                return String.valueOf(width);
            case HEIGHT:
                return String.valueOf(height);
            case IMAGE_TYPE:
                return fileType;
            default:
                throw new AutotestError(String.format("Получение значения для типа \"%s\" не реализовано", type.name));
        }
    }

    public double getFileSize() {
        return fileSize;
    }

    public Path getLocalPath() {
        return localPath;
    }

    public boolean validate(final String expected, final String propertyName) {
        switch (ImagePropertiesType.getEnumByName(propertyName)) {
            case WIDTH:
                return width == Integer.parseInt(expected);
            case HEIGHT:
                return height == Integer.parseInt(expected);
            case IMAGE_TYPE:
                return fileType.equalsIgnoreCase(expected);
            default:
                throw new AutotestError(String.format("Проверка для типа \"%s\" не реализована", propertyName));
        }
    }

    private String getFileNameFromUrl(final URL url) {
        final String[] parts = url.getFile().split("/");
        return parts[parts.length - 1].split("&")[0];
    }

    public enum ImagePropertiesType {
        WIDTH("Ширина"),
        HEIGHT("Высота"),
        IMAGE_TYPE("Тип");

        private final String name;

        ImagePropertiesType(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static ImagePropertiesType getEnumByName(final String name) {
            for (final ImagePropertiesType type : ImagePropertiesType.values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            throw new AutotestError("Не найден тип по значению " + name);
        }
    }
}
