package ru.sbt.edu_power.assist_bot.roles.roles;

import com.slack.api.model.User;
import ru.sbt.edu_power.assist_bot.slack.users.SlackUsers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class UserRolesRepository {
    private final Map<User, Set<SlackRoles>> repository = new HashMap<>();
    private final EnumMap<SlackRoles, List<String>> roleToAcceptedModuleMap = new EnumMap<>(SlackRoles.class);
    private static final String[] ADMIN_USERS = new String[]{"U0212V6UQTS", "UHSHZUMRC"};
    private static final String[] REGRESS_USER = new String[]{"ULNMYLX96", "U0171R1CVC1"};

    public UserRolesRepository() {
        updateRolesForProfileList(ADMIN_USERS, SlackRoles.ADMIN);
        updateRolesForProfileList(REGRESS_USER, SlackRoles.REGRESS);
    }

    private void updateRolesForProfileList(final String[] users, final SlackRoles role) {
        for (final String userId : users) {
            final User admin = SlackUsers.getInstance().getUser(userId);
            if (Objects.isNull(admin)) {
                continue;
            }
            repository.put(admin, new HashSet<>());
            repository.get(admin).add(role);
        }
    }

    public void acceptModule(final SlackRoles role, final String name) {
        if (!roleToAcceptedModuleMap.containsKey(role)) {
            roleToAcceptedModuleMap.put(role, new ArrayList<>());
        }
        roleToAcceptedModuleMap.get(role).add(name);
    }

    public List<String> getAcceptedModules(final SlackRoles role) {
        return roleToAcceptedModuleMap.get(role);
    }

    public void clearRoles(final String userId) {
        final User user = SlackUsers.getInstance().getUser(userId);
        repository.get(user).clear();
    }

    public void add(final String userId, final String roleId) {
        add(userId, SlackRoles.valueOf(roleId));
    }

    public void add(final String userId, final SlackRoles role) {
        final User user = SlackUsers.getInstance().getUser(userId);
        add(user, role);
    }

    public void add(final User user, final SlackRoles role) {
        if (!repository.containsKey(user)) {
            repository.put(user, new HashSet<>());
        }
        repository.get(user).add(role);
    }

    public boolean hasAny(final String userId, final SlackRoles... roles) {
        final User user = SlackUsers.getInstance().getUser(userId);
        return hasAny(user, roles);
    }

    public boolean hasAny(final User user, final SlackRoles... roles) {
        if (!repository.containsKey(user)) {
            repository.put(user, new HashSet<>());
        }
        for (final SlackRoles role : roles) {
            if (role == SlackRoles.USER) {
                return true;
            }
            if (repository.get(user).contains(role)) {
                return true;
            }
        }
        return false;
    }

    public static String getAdminUser() {
        return "develop".equals(System.getProperty("botEnv")) ?
                ADMIN_USERS[0] :
                ADMIN_USERS[1];
    }
}
