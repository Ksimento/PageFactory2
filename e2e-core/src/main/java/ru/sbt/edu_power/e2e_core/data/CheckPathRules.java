package ru.sbt.edu_power.e2e_core.data;

import org.junit.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс выполняет проверку допустимости используемого пути до файла
 * Проверяется длинна пути (что бы не превышала максимальную) и наличие недопустимых символов
 */
public class CheckPathRules {
    private static final int MAX_PATH_LENGTH = 185;

    public static void checkPath(final String path) {
        final int pathLength = getPathLength(path);
        Assert.assertTrue(
                String.format(
                        "Длина пути \"%d\" превышает допустимую \"%d\". " +
                        "Необходимо сократить имена директорий или название файла\n%s",
                        pathLength,
                        MAX_PATH_LENGTH,
                        getInnerPath(path)
                ),
                MAX_PATH_LENGTH > pathLength
        );
        final String illegalSymbol = getIllegalSymbolFromPath(path);
        Assert.assertTrue(
                String.format(
                        "Путь содержит недопустимый символ \"%s\"\n%s",
                        illegalSymbol,
                        getInnerPath(path).replace(illegalSymbol, ">>>" + illegalSymbol + "<<<")
                ), illegalSymbol.isEmpty()
        );
    }

    private static int getPathLength(final String path) {
        return getInnerPath(path).length();
    }

    private static String getIllegalSymbolFromPath(final String path) {
        final String innerPath = getInnerPath(path);
        final Pattern pattern = Pattern.compile(".*([\"'`~\\[\\](){}ёЁ!№#@$^&*?<>=]).*");
        final Matcher matcher = pattern.matcher(innerPath);
        return matcher.find() ? matcher.replaceFirst("$1") : "";

    }

    private static String getInnerPath(final String path) {
        return path.replaceAll(".*(edupower.*)", "$1");
    }
}
