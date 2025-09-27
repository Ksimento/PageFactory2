package ru.sbt.edu_power.e2e_core.auth;

enum StandType {
    LOCALHOST("/logout"),
    DEV_EDU("/logout"),
    DEV_MFE("/services/auth/logout"),
    DEV_S21_ADMIN("/logout"),
    DEV_S21_STUDENT("/logout"),
    STAGE_EDU("/logout"),
    STAGE_MFE("/services/auth/logout"),
    STAGE_S21_ADMIN("/logout"),
    STAGE_S21_STUDENT("/logout"),
    PROD_EDU("/logout"),
    PROD_MFE("/services/auth/logout"),
    PROD_S21_ADMIN("/logout"),
    PROD_S21_STUDENT("/logout"),
    DEMO_DATA("/services/auth/logout"),
    DEMO_DATA_MFE("/services/auth/logout"),
    DEMO_DATA_S21_STUDENT("/logout"),
    DEMO_DATA_S21_ADMIN("/logout"),
    DEMO("/services/auth/logout"),
    DEMO_MFE("/services/auth/logout"),
    ;

    private final String logoutService;

    StandType(final String logoutService) {
        this.logoutService = logoutService;
    }

    public String getLogoutService() {
        return logoutService;
    }

    public static StandType determineStandType(final String host) {
        if (host.contains("newschool") && host.contains("sberclass.ru")) {
            return host.contains("-beta") ? PROD_MFE : PROD_EDU;
        }
        if (host.contains("21-school.ru")) {
            if (host.contains("-admin")) {
                if (host.startsWith("sp")) {
                    return STAGE_S21_ADMIN;
                } else {
                    return PROD_S21_ADMIN;
                }
            } else if (host.startsWith("sp")) {
                return STAGE_S21_STUDENT;
            } else {
                return PROD_S21_STUDENT;
            }
        }
        if (host.contains("demo-data")) {
            if (host.contains("-admin")) {
                return DEMO_DATA_S21_ADMIN;
            } else if (host.contains("s21")) {
                return DEMO_DATA_S21_STUDENT;
            } else {
                return host.contains("-beta") ? DEMO_DATA_MFE : DEMO_DATA;
            }
        }
        if (host.contains("demo.")) {
            return host.contains("-beta") ? DEMO_MFE : DEMO;
        }
        if (host.startsWith("sp")) {
            return host.contains("-beta") ? STAGE_MFE : STAGE_EDU;
        }
        if (host.contains("-s21-admin")) {
            return DEV_S21_ADMIN;
        }
        if (host.contains("-s21")) {
            return DEV_S21_STUDENT;
        }
        if (host.contains("localhost")) {
            return LOCALHOST;
        }
        return host.contains("-beta") ? DEV_MFE : DEV_EDU;
    }
}
