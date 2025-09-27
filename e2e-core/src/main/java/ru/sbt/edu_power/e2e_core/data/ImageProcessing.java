package ru.sbt.edu_power.e2e_core.data;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;
import ru.sbt.edu_power.e2e_core.driver_utils.DriverUtils;
import ru.sbt.edu_power.e2e_core.layout.LayoutUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbtqa.monte.media.io.ByteArrayImageOutputStream;
import ru.sbtqa.tag.pagefactory.utils.ScreenshotUtils;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Slf4j
public class ImageProcessing {
    public static byte[] takeScreenShot() {
        return ScreenshotUtils.DRIVER.take();
    }

    // Метод выполняет снимок элемента страницы
    public static byte[] takeScreenShotFragment(final WebElement element) {

        DriverUtils.getActions().moveByOffset(10, 10).build().perform();
        final Map rectangle = (Map) DriverUtils.executeJS(
                "return arguments[0].getBoundingClientRect()",
                element
        );
        final int devicePixelRatio = ((Number) DriverUtils.executeJS("return window.devicePixelRatio")).intValue();
        final int left = ((Number) rectangle.get("left")).intValue();
        final int top = ((Number) rectangle.get("top")).intValue();
        final int width = ((Number) rectangle.get("width")).intValue();
        final int height = ((Number) rectangle.get("height")).intValue();
        // высчитываю размеры части элемента, находящейся во вьюпорте, если элемент частично вне экрана
        final int normalizedWidth;
        if ((left + width) < LayoutUtils.getBodyWidth()) {
            normalizedWidth = width;
        } else {
            normalizedWidth = LayoutUtils.getBodyWidth() - left - 1;
        }
        final int normalizedHeight;
        if ((top + height) < LayoutUtils.getBodyHeight()) {
            normalizedHeight = height;
        } else {
            normalizedHeight = LayoutUtils.getBodyHeight() - top - 1;
        }
        if (normalizedWidth < 0 || normalizedHeight < 0) {
            return new byte[]{};
        }
        final byte[] image = takeScreenShot();
        try {
            final BufferedImage bufferedImage = ImageIO
                    .read(new ByteArrayInputStream(image))
                    .getSubimage(
                            left * devicePixelRatio,
                            top * devicePixelRatio,
                            normalizedWidth * devicePixelRatio,
                            normalizedHeight * devicePixelRatio
                    );
            final BufferedImage resizedBufferedImage = resize(normalizedWidth, normalizedHeight, bufferedImage);
            try (final ByteArrayImageOutputStream outputStream = new ByteArrayImageOutputStream()) {
                final JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
                jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                final float COMPRESSION_LEVEL_50_PERCENT = 0.5f;
                jpegParams.setCompressionQuality(COMPRESSION_LEVEL_50_PERCENT);
                final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                writer.setOutput(outputStream);
                writer.write(null, new IIOImage(resizedBufferedImage, null, null), jpegParams);
                return outputStream.toByteArray();
            }

        } catch (final IOException e) {
            throw new AutotestError("Ошибка при получении скриншота", e);
        }
    }

    // Ресайз изображения
    public static BufferedImage resize(final int newWidth, final int newHeight, final BufferedImage image) {
        final Image scaledInstance = image.getScaledInstance(newWidth, newHeight, Image.SCALE_AREA_AVERAGING);
        final BufferedImage bufferedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.drawImage(scaledInstance, 0, 0, null);
        graphics2D.dispose();
        return bufferedImage;
    }

    // Генерация тестового изображения
    public static BufferedImage generateImage() {
        // Делаем квадратную картинку со стороной 500px
        final int IMAGE_SIZE = 500;
        // Размер шрифта в 4 раза меньше
        final int FONT_SIZE = IMAGE_SIZE / 4;
        final BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = image.createGraphics();
        g2.setFont(new Font("SansSerif", Font.PLAIN, FONT_SIZE));
        final Color color1 = new Color(250, 90, 10);
        final Color color2 = new Color(90, 90, 250);
        // Градиентная заливка фона
        final GradientPaint gradientPaint = new GradientPaint(0, 0, color1, IMAGE_SIZE, IMAGE_SIZE, color2);
        g2.setPaint(gradientPaint);
        g2.fillRect(0, 0, IMAGE_SIZE, IMAGE_SIZE);
        g2.setColor(new Color(255, 255, 255, 80));
        // Добавляем 5-ти значное число для красоты
        g2.drawString(
                DataProcessing.generator("number5"),
                IMAGE_SIZE / 10,
                IMAGE_SIZE / 2 + FONT_SIZE / 2
        );
        return image;
    }

    // Добиваем тестовое изображение до нужного веса в байтах
    public static void addBallastToImageAndWright(
            final BufferedImage image,
            final File file,
            final String type,
            final long sizeInBytes
    ) {
        try {
            ImageIO.write(image, type, file);
            final long currentSize = Files.size(file.toPath());
            if (currentSize < sizeInBytes) {
                final int additionalSize = (int) (sizeInBytes - currentSize);
                final ImageWriter writer = ImageIO.getImageWritersByFormatName(type).next();
                final ImageWriteParam writeParam = writer.getDefaultWriteParam();
                final ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
                final IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
                if ("png".equalsIgnoreCase(type)) {
                    // Записываем мусор в метаданные картинки для увеличения веса файла
                    final IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
                    textEntry.setAttribute("keyword", "ballast");
                    Timer.startTimer("generator");
                    textEntry.setAttribute("value", generateBallast(additionalSize));
                    final IIOMetadataNode text = new IIOMetadataNode("tEXt");
                    text.appendChild(textEntry);
                    final IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
                    root.appendChild(text);
                    metadata.mergeTree("javax_imageio_png_1.0", root);
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final ImageOutputStream stream = ImageIO.createImageOutputStream(baos);
                    writer.setOutput(stream);
                    Timer.startTimer("add_metadata");
                    writer.write(metadata, new IIOImage(image, null, metadata), writeParam);
                    stream.close();
                    Files.write(file.toPath(), baos.toByteArray());
                }

            }
        } catch(final IOException e) {
            throw new AutotestError(e);
        }
    }

    // Генерация мусора для добавления веса файлу
    private static String generateBallast(final int size) {
        final byte[] generatedString = new byte[size];
        for (int i = 0; i < size; i++) {
            generatedString[i] = 'a';
        }
        return new String(generatedString);
    }
}
