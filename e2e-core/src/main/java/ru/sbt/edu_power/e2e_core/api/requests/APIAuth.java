package ru.sbt.edu_power.e2e_core.api.requests;

import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.api.connect.Stand;
import ru.sbt.edu_power.e2e_core.api.connect.TokenService;
import ru.sbt.edu_power.e2e_core.auth.AuthAction;
import ru.sbt.edu_power.e2e_core.resource_repository.ResourceRepository;
import ru.sbtqa.tag.datajack.Stash;

import java.util.Map;

public class APIAuth {
    public static final String TOKEN_STASH_KEY = "token.stash.key";

    public static void authByProfileName(final String profileName) {
        authByProfileName(Stand.STAND_URL, profileName);
    }

    public static void authWithLoginAndPassword(final String userLogin, final String userPassword) {
        final String token = getToken(userLogin,userPassword);
        Stash.put(TOKEN_STASH_KEY, token);
    }

    public static String getToken (final String userLogin, final String userPassword){
        return new TokenService().getToken(
                Stand.STAND_URL,
                userLogin,
                userPassword
        );
    }

    public static void authByProfileName(final String stand, final String profileName) {
        final Map<String, Map<String, Map<String, String>>> resource = AuthAction.getLoginAndPasswordInFile(profileName);
        final String token = new TokenService().getToken(
                stand,
                resource.get("predefined_data").get(profileName).get("name"),
                resource.get("predefined_data").get(profileName).get("password")
        );
        Stash.put(TOKEN_STASH_KEY, token);
    }
}
