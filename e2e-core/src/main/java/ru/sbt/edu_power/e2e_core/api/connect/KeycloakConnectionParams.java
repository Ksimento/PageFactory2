package ru.sbt.edu_power.e2e_core.api.connect;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.net.URI;

@Getter
public class KeycloakConnectionParams {
    private static final int KEYCLOAK_LOCAL_PORT = 6980;
    private static final int HTTP_STD_PORT = 80;
    private static final int HTTPS_STD_PORT = 443;

    private final String serverUrl;
    private final String realm;
    private final String username;
    private final String password;
    private final String clientId;

    public KeycloakConnectionParams(
            final URI uri,
            final String realm,
            final String username,
            final String password,
            final String clientId
    ) {
        this.serverUrl = makeKeycloakUrl(uri).append("/auth").toString();
        this.realm = realm;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
    }

    private static StringBuilder makeKeycloakUrl(@Nonnull final URI uri) {
        final StringBuilder builder = new StringBuilder();
        final String scheme = uri.getScheme();
        builder.append(scheme);
        builder.append("://");
        final String host = uri.getHost();
        if (host.startsWith("dev")) {
            final int dot = host.indexOf('.');
            final int minus = host.indexOf('-');
            final int separator;
            if (minus > 0) {
                if (dot > 0) {
                    separator = Math.min(minus, dot);
                } else {
                    separator = minus;
                }
            } else {
                separator = dot;
            }
            builder.append(host, 0, separator);
            builder.append("-auth");
            builder.append(host, dot, host.length());
            appendPortIfNeed(builder, scheme, uri.getPort());
        } else if ("localhost".equals(host)) {
            builder.append(host);
            appendPortIfNeed(builder, scheme, KEYCLOAK_LOCAL_PORT);
        } else {
            builder.append(host);
            appendPortIfNeed(builder, scheme, uri.getPort());
        }
        return builder;
    }

    private static void appendPortIfNeed(
            @Nonnull final StringBuilder builder,
            @Nonnull final String scheme,
            final int port
    ) {
        if ((("http".equalsIgnoreCase(scheme) && port != HTTP_STD_PORT) ||
             ("https".equalsIgnoreCase(scheme) && port != HTTPS_STD_PORT)) && port > 0) {
            builder.append(':');
            builder.append(port);
        }
    }

}
