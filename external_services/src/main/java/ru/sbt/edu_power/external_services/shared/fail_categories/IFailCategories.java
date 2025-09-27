package ru.sbt.edu_power.external_services.shared.fail_categories;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public interface IFailCategories {
    String getName();
    String getStep();
    String getTrace();
    String getColor();
    FailCategoryShape getShape();

    default IFailCategories determine(final IFailCategories[] values, final String step, final String trace) {
        if (!trace.isEmpty()) {
            // Определяем категорию ошибки, если её название уже есть в тексте ошибки
            final Optional<IFailCategories> predefinedCategory = Stream.of(values)
                    .filter(c -> trace.contains(c.getName()))
                    .findFirst();
            if (predefinedCategory.isPresent()) {
                return predefinedCategory.get();
            }
            // Определяем категорию по наличию ключевых слов в тексте ошибки
            final Optional<IFailCategories> categoryByErrorMessage = Stream.of(values)
                    .filter(c -> !c.getTrace().isEmpty())
                    .filter(c -> Pattern.compile(c.getTrace()).matcher(trace).find())
                    .findFirst();
            if (categoryByErrorMessage.isPresent()) {
                return categoryByErrorMessage.get();
            }
        }
        if (!step.isEmpty()) {
            // Определяем категорию по названию шага
            return Stream.of(values)
                    .filter(c -> !c.getStep().isEmpty())
                    .filter(c -> Pattern.compile(c.getStep()).matcher(step).find())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
