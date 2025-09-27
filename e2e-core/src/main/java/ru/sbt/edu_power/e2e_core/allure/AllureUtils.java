package ru.sbt.edu_power.e2e_core.allure;

import io.qameta.allure.Allure;
import ru.sbt.edu_power.external_services.data.Archiver;
import ru.sbt.edu_power.e2e_core.data.ImageProcessing;
import ru.sbtqa.tag.pagefactory.allure.Type;

public class AllureUtils {
    public static void attachMessageToAllureStep(final String param, final String value) {
        Allure.attachment(param, value);
    }

    public static void attachScreenShotToAllure(final String imageName) {
        attachScreenShotToAllure(imageName, ImageProcessing.takeScreenShot());
    }

    // Метод прикрепляет изображение к шагу аллюра
    public static void attachScreenShotToAllure(final String imageName, final byte[] image) {
        attachScreenShotToAllure(imageName, image, Type.PNG);
    }

    // Метод прикрепляет изображение к шагу аллюра с указанием типа изображения. Изображение должно быть в виде
    // сжатого массива байтов
    public static void attachCompressedScreenShotToAllure(final String imageName, final byte[] data, final Type type) {
        attachScreenShotToAllure(
                imageName,
                new Archiver().setArchiveBytes(data).unGzipArchive().sourceAsBytes(),
                type
        );
    }

    // Метод прикрепляет изображение к шагу аллюра с указанием типа изображения
    public static void attachScreenShotToAllure(final String imageName, final byte[] image, final Type type) {
        Allure.addByteAttachmentAsync(imageName, type.getType(), type.getExtension(), () -> image);
    }
}
