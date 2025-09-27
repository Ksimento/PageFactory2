package ru.sbt.edu_power.assist_bot.slack;

import ru.sbt.edu_power.assist_bot.AssistBotException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class MessagesRepository {
    private static final Map<String, String> MAP = new HashMap<>();

    static {
        loadData();
    }

    public static String get(final String item, final Replacement... replacements) {
        if (replacements.length > 0) {
            final AtomicReference<String> message = new AtomicReference<>(MAP.get(item));
            for (final Replacement r : replacements) {
                message.set(
                        message.get()
                               .replace("{%" + r.getKey() + "%}", r.getValue())
                );
            }
            return message.get();
        }
        return MAP.get(item);
    }

    public static boolean has(final String item) {
        return MAP.containsKey(item);
    }

    private static void loadData() {
        final List<String> lines = new ArrayList<>();
        try (final InputStream is = MessagesRepository.class.getClassLoader().getResourceAsStream("messages.txt");
             final InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(is));
             final BufferedReader bufferedReader = new BufferedReader(reader)) {
            bufferedReader.lines()
                          .map(String::trim)
                          .filter(l -> !l.startsWith("#"))
                          .forEachOrdered(lines::add);

        } catch (final IOException e) {
            throw new AssistBotException(e);
        }
        final List<String> messageLines = new ArrayList<>();
        final Iterator<String> iterator = lines.iterator();
        final AtomicReference<String> item = new AtomicReference<>("");
        while (iterator.hasNext()) {
            final String line = iterator.next();
            if (line.startsWith("@")) {
                if (!item.get().isEmpty()) {
                    if (messageLines.get(messageLines.size() - 1).isEmpty()) {
                        messageLines.remove(messageLines.size() - 1);
                    }
                    MAP.put(item.get(), String.join("\n", messageLines));
                    messageLines.clear();
                }
                final String[] parts = line.split("=");
                item.set(parts[0].replace("@", "").trim());
                messageLines.clear();
                if (!parts[1].trim().isEmpty()) {
                    messageLines.add(parts[1].trim());
                }
            } else {
                messageLines.add(line.trim());
            }
        }
        MAP.put(item.get(), String.join("\n", messageLines));
    }

}
