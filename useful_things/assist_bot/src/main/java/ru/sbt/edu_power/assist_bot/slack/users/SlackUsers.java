package ru.sbt.edu_power.assist_bot.slack.users;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;
import com.slack.api.model.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.assist_bot.AssistBotException;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.timer.Timer;
import ru.sbt.edu_power.assist_bot.runner.Main;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Slf4j
public final class SlackUsers {
    private static SlackUsers instance;
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, User> usersByEmail = new HashMap<>();
    private final Map<String, User> usersByName = new HashMap<>();
    private final MethodsClient client = Main.SLACK_DISPATCHER.getApp().client();
    private final String BOT_TOKEN = Main.BOT_TOKEN;
    private LocalDateTime lastNullCleared = LocalDateTime.now();


    private SlackUsers() {
    }

    public static SlackUsers getInstance() {
        if (instance == null) {
            instance = new SlackUsers();
        }
        return instance;
    }

    public ZoneId getUserTz(final String userId) {
        final User user = getUser(userId);
        if (Objects.isNull(user)) {
            return ZoneId.of("Europe/Moscow");
        }
        return ZoneId.of(user.getTz());
    }

    @SneakyThrows
    public User getUser(final String userId) {
        if (!users.containsKey(userId)) {
            final User user = client.usersInfo(r -> r.user(userId).token(BOT_TOKEN)).getUser();
            if (Objects.isNull(user)) {
                return null;
            }
            updateCache(user);
        }
        return users.get(userId);
    }

    public User getUserByName(final String name) {
        if (!usersByName.containsKey(name)) {
            final List<User> users = getUsers()
                    .stream()
                    .filter(u -> u.getName().equals(name))
                    .collect(Collectors.toList());
            if (users.isEmpty()) {
                return null;
            }
            updateCache(users.get(0));
        }
        return usersByName.get(name);
    }

    public User getUserByEmail(final String email) {
        clearNullUsers();
        final String lowerCaseEmail = email.toLowerCase();
        if (!usersByEmail.containsKey(lowerCaseEmail)) {
            final AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();
            final BooleanSupplier waitWhenUserBeFound = () -> {
                try {
                    final UsersLookupByEmailResponse response = client.usersLookupByEmail(r -> r
                            .email(lowerCaseEmail)
                            .token(BOT_TOKEN));
                    if (!response.isOk()) {
                        if ("ratelimited".equals(response.getError())) {
                            // превышено максимально допустимое количество запросов в единицу времени
                            log.warn("Превышено максимальное число запросов в API SLACK");
                            ESUtils.freeze(5000);
                            return false;
                        }
                        if ("users_not_found".equals(response.getError())) {
                            usersByEmail.put(lowerCaseEmail, null);
                            return true;
                        }
                        log.error(
                                "needed: {}\nprovided: {}\nerror: {}",
                                response.getNeeded(),
                                response.getProvided(),
                                response.getError()
                        );
                        return true;
                    }
                    updateCache(response.getUser());
                    return true;
                } catch (final Throwable e) {
                    throwableAtomicReference.set(e);
                    ESUtils.freeze(5000);
                    return false;
                }
            };
            final boolean result = Timer.executeTimer(60, waitWhenUserBeFound);
            if (!result && Objects.nonNull(throwableAtomicReference.get())) {
                throw new AssistBotException(throwableAtomicReference.get());
            }
        }
        return usersByEmail.get(lowerCaseEmail);
    }

    private void clearNullUsers() {
        final LocalDateTime current = LocalDateTime.now();
        if (Duration.between(lastNullCleared, current).toMinutes() > 5L) {
            new ArrayList<>(usersByEmail.keySet())
                    .forEach(u -> {
                        if (Objects.isNull(usersByEmail.get(u))) {
                            usersByEmail.remove(u);
                        }
                    });
            lastNullCleared = LocalDateTime.now();
        }
    }

    public List<User> getUsers() {
        final List<User> users = new ArrayList<>();
        loadUserPageable(users, "");
        return users;
    }

    @SneakyThrows
    private void loadUserPageable(final List<User> users, final String cursor) {
        final UsersListResponse response = client.usersList(r -> r.token(BOT_TOKEN).limit(800).cursor(cursor));
        if (!response.isOk()) {
            log.error("{}", response);
        }
        final List<User> notDeleted = response.getMembers()
                                              .stream()
                                              .filter(u -> !u.isDeleted())
                                              .collect(Collectors.toList());
        users.addAll(notDeleted);
        if (Objects.nonNull(response.getResponseMetadata().getNextCursor())
            && !response.getResponseMetadata().getNextCursor().isEmpty()) {
            loadUserPageable(users, response.getResponseMetadata().getNextCursor());
        }
    }

    public Duration beforeWorkTimeIsDone(final String userId) {
        final ZoneId userTz = getUserTz(userId);
        final LocalDateTime endWorkTime = LocalDateTime.of(LocalDate.now(userTz), LocalTime.of(19, 0));
        final LocalDateTime currentDateTime = LocalDateTime.now(userTz);
        return Duration.between(currentDateTime, endWorkTime);
    }

    private void updateCache(final User user) {
        log.info("Добавлен пользователь {}", user.getProfile().getDisplayName());
        if (!users.containsKey(user.getId())) {
            users.put(user.getId(), user);
        }
        if (
                Objects.nonNull(user.getProfile().getEmail()) &&
                !usersByEmail.containsKey(user.getProfile().getEmail().toLowerCase())
        ) {
            usersByEmail.put(user.getProfile().getEmail().toLowerCase(), user);
        }
        if (!usersByName.containsKey(user.getName())) {
            usersByName.put(user.getName(), user);
        }
    }
}
