package ru.sbt.edu_power.external_services.mattermost;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bis5.mattermost.client4.ApiResponse;
import net.bis5.mattermost.client4.MattermostClient;
import net.bis5.mattermost.client4.Pager;
import net.bis5.mattermost.model.User;
import net.bis5.mattermost.model.UserList;
import ru.sbt.edu_power.external_services.ESUtils;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.timer.Timer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Slf4j
public final class MattermostUsers {
    private static MattermostUsers instance;
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, User> usersByEmail = new HashMap<>();
    private final Map<String, User> usersByUsername = new HashMap<>();
    private final MattermostClient client = Regressman.getInstance().getClient();
    private LocalDateTime lastNullCleared = LocalDateTime.now();
    private static List<User> userMMList = new ArrayList<>();

    private MattermostUsers() {
    }

    public static MattermostUsers getInstance() {
        if (instance == null) {
            instance = new MattermostUsers();
        }
        return instance;
    }

    @SneakyThrows
    public User getUser(final String userId) {
        if (!users.containsKey(userId)) {
            final ApiResponse<User> response = client.getUser(userId);
            if (response.hasError()) {
                return null;
            }
            updateCache(response.readEntity());
        }
        return users.get(userId);
    }

    public User getUserByUsername(final String userName) {
        if (!usersByUsername.containsKey(userName)) {
            final AtomicReference<ApiResponse<User>> response = new AtomicReference<>();
            final BooleanSupplier waitWhenUserBeLoaded = () -> {
                response.set(client.getUserByUsername(userName));
                if (response.get().hasError()) {
                    ESUtils.freeze(3000);
                    return false;
                }
                return true;
            };

            if (!Timer.executeTimer(60, waitWhenUserBeLoaded)) {
                final String message = response.get().hasError() ?
                        "Не удалось получить пользователя\n" + response.get().readError().getDetailedError() :
                        "Не удалось получить пользователя";
                throw new ExternalServicesException(message);
            }

            if (response.get().readEntity() == null) {
                return null;
            }
            updateCache(response.get().readEntity());
        }
        return usersByUsername.get(userName);
    }

    public User getUserByEmail(final String email) {
        clearNullUsers();
        final String lowerCaseEmail = email.toLowerCase();
        if (!usersByEmail.containsKey(lowerCaseEmail)) {
            final AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();
            final BooleanSupplier waitWhenUserBeFound = () -> {
                try {
                    final ApiResponse<User> response = client.getUserByEmail(lowerCaseEmail);
                    if (response.hasError()) {
                        ESUtils.freeze(3000);
                        return false;
                    }
                    updateCache(response.readEntity());
                    return true;
                } catch (final Throwable e) {
                    throwableAtomicReference.set(e);
                    ESUtils.freeze(5000);
                    return false;
                }
            };
            final boolean result = Timer.executeTimer(60, waitWhenUserBeFound);
            if (!result && Objects.nonNull(throwableAtomicReference.get())) {
                throw new ExternalServicesException(throwableAtomicReference.get());
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
        loadUserPageable(users, Pager.of(0, 300));
        return users;
    }

    @SneakyThrows
    private void loadUserPageable(final List<User> users, final Pager pager) {
        final AtomicReference<ApiResponse<UserList>> response = new AtomicReference<>();
        final BooleanSupplier waitWhenUsersBeLoaded = () -> {
            response.set(client.getUsers(pager));
            if (response.get().hasError()) {
                ESUtils.freeze(3000);
                return false;
            }
            return true;
        };
        if (!Timer.executeTimer(60, waitWhenUsersBeLoaded)) {
            final String message = response.get().hasError() ?
                    "Не удалось получить пользователей\n" + response.get().readError().getDetailedError() :
                    "Не удалось получить пользователей";
            throw new ExternalServicesException(message);
        }
        if (response.get().readEntity().isEmpty()) {
            return;
        }
        final List<User> notDeleted = response.get()
                                              .readEntity()
                                              .stream()
                                              .filter(u -> u.getDeleteAt() == 0)
                                              .collect(Collectors.toList());
        users.addAll(notDeleted);
        loadUserPageable(users, pager.nextPage());
    }


    private void updateCache(final User user) {
        log.info("Добавлен пользователь {} {}", user.getFirstName(), user.getLastName());
        if (!users.containsKey(user.getId())) {
            users.put(user.getId(), user);
        }
        if (
                Objects.nonNull(user.getEmail()) &&
                !usersByEmail.containsKey(user.getEmail().toLowerCase())
        ) {
            usersByEmail.put(user.getEmail().toLowerCase(), user);
        }
        if (!usersByUsername.containsKey(user.getUsername())) {
            usersByUsername.put(user.getUsername(), user);
        }
    }

    public List<User> getUserMMList(){
        if (userMMList.isEmpty()) {
            userMMList = getUsers();
        }
        return userMMList;
    }
}
