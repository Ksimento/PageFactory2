package ru.sbt.edu_power.assist_bot.slack.views.sections;

import ru.sbt.edu_power.assist_bot.AssistBotException;

public class FieldUtils {

    /**
     * метод ограничивает строку до указанного количества символов, добавляя заполнитель в конце или в середине
     * @param text      текст для обработки
     * @param maxLength максимальное число символов для конечного результата
     * @param cutType   тип обрезки (с конца или в середине)
     * @return          укороченная строка с заполнителем в конце или середине текста
     */
    public static String optionCut(final String text, final int maxLength, final CutType cutType) {
        final String result;
        if (text.length() <= maxLength) {
            result = text;
        } else {
            final String FILLER = "~~~";
            switch (cutType) {
                case END:
                    result = text.substring(0, maxLength - 3) + FILLER;
                    break;
                case MIDDLE:
                    final int part = (int) Math.floor((double) (maxLength - 3) / 2);
                    result = text.substring(0, part) + FILLER + text.substring(text.length() - part);
                    break;
                default:
                    throw new AssistBotException("Не поддерживается тип обрезки " + cutType);
            }
        }
        return result;
    }

    public enum CutType {
        MIDDLE,
        END
    }
}
