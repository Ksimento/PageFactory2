package ru.sbt.edu_power.external_services.version_releases;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.ExternalServicesException;

import java.text.DecimalFormat;
import java.util.Objects;
import java.util.regex.Pattern;

// схема версий релизов. Поддерживает работу со всеми типами из VersionType
@Slf4j
@Getter
public final class ReleaseVersion implements Comparable<ReleaseVersion> {
    private final String prefix;
    private final int major;
    private final int minor;
    private final int hotfix;
    private final String suffix;
    private final double rc;
    private final VersionType versionType;

    public ReleaseVersion(final String version) {
        versionType = determineVersionType(version);
        prefix = getPrefix(version);
        major = getMajor(version);
        minor = getMinor(version);
        hotfix = getHotfix(version);
        suffix = getSuffix(version);
        rc = getRc(version);
    }

    public boolean isUndefined() {
        return versionType == VersionType.UNDEFINED;
    }

    public boolean withoutVersion() {
        return versionType == VersionType.NON_VERSION;
    }

    public String formatWithoutRc() {
        switch (versionType) {
            case SBERCLASS:
            case HASURA:
            case ACC_LANDING:
            case LIT:
                return String.format("%s%d.%d.%d", prefix, major, minor, hotfix);
            case MFE:
                return String.format("%s%d.%d", prefix, major, minor);
            case DASH:
                return String.format("%s.%s.%s", major, minor, hotfix);
            case NON_VERSION:
                return "Версия релиза не указана";
            case UNDEFINED:
                return "Неизвестный формат версии";
            default:
                throw new ExternalServicesException("Нет обработчика для типа " + versionType);
        }
    }

    public boolean equalsByMainVersion(final ReleaseVersion o) {
        return formatWithoutRc().equals(o.formatWithoutRc());
    }

    public String toString() {
        final DecimalFormat decimalFormat = new DecimalFormat("#.#");
        switch (versionType) {
            case SBERCLASS:
            case HASURA:
            case ACC_LANDING:
            case LIT:
                if (rc == 0) {
                    return String.format("%s%d.%d.%d", prefix, major, minor, hotfix);
                } else {
                    return String.format("%s%d.%d.%d-%s%s",prefix, major, minor, hotfix, suffix, decimalFormat.format(rc));
                }
            case MFE:
                if (rc == 0) {
                    return String.format("%s%d.%d", prefix, major, minor);
                } else {
                    return String.format("%s%d.%d-%s%s", prefix, major, minor, suffix, decimalFormat.format(rc));
                }
            case DASH:
                return String.format("%s.%s.%s", major, minor, hotfix);
            case NON_VERSION:
                return "Версия релиза не указана";
            case UNDEFINED:
                return "Неизвестный формат версии";
            default:
                throw new ExternalServicesException("Нет обработчика для типа " + versionType);
        }
    }

    private VersionType determineVersionType(final String version) {
        if (Objects.isNull(version) || version.isEmpty()) {
            return VersionType.NON_VERSION;
        }
        if (version.startsWith("r/acc-")) {
            return VersionType.ACC_LANDING;
        }
        if (version.contains("LIT-")){
            return VersionType.LIT;
        }
        final Pattern pattern = Pattern.compile("^r/\\d*\\.\\d*\\.\\d*");
        if (pattern.matcher(version).find()) {
            return VersionType.SBERCLASS;
        }
        final Pattern patternMfe = Pattern.compile("^r/\\d*\\.\\d*$");
        if (version.contains("-mfe") || patternMfe.matcher(version).find()) {
            return VersionType.MFE;
        }
        final Pattern patternHasura = Pattern.compile("^release/\\d*\\.\\d*\\.\\d*");
        if (patternHasura.matcher(version).find()) {
            return VersionType.HASURA;
        }
        final Pattern patternDashboard = Pattern.compile("^\\d*\\.\\d*\\.\\d*$");
        if (patternDashboard.matcher(version).find()) {
            return VersionType.DASH;
        }
        return VersionType.UNDEFINED;
    }

    private String getPrefix(final String version) {
        switch (versionType) {
            case SBERCLASS:
            case MFE:
                return "r/";
            case ACC_LANDING:
                return "r/acc-";
            case HASURA:
                return "release/";
            case LIT:
                return version.split("-")[0] + "-";
            case NON_VERSION:
            case UNDEFINED:
            case DASH:
                return "";
            default:
                throw new ExternalServicesException("Не реализована поддержка типа версии " + versionType.name());
        }
    }

    private int getMajor(final String version) {
        try {
            switch (versionType) {
                case UNDEFINED:
                case NON_VERSION:
                    return 0;
                case DASH:
                    return Integer.parseInt(version.split("\\.")[0]);
                case MFE:
                case SBERCLASS:
                case HASURA:
                case ACC_LANDING:
                case LIT:
                    return Integer.parseInt(version.replace(getPrefix(version), "").split("\\.")[0]);
                default:
                    throw new ExternalServicesException("Не реализована поддержка типа версии " + versionType.name());
            }
        } catch (final NumberFormatException e) {
            log.error("Произошла ошибка при попытке получить мажор версии {}", version);
        }
        return 0;
    }

    private int getMinor(final String version) {
        try {
            switch (versionType) {
                case SBERCLASS:
                case HASURA:
                case MFE:
                case ACC_LANDING:
                case LIT:
                    return Integer.parseInt(version.replace(getPrefix(version), "").split("[.-]")[1]);
                case DASH:
                    return Integer.parseInt(version.split("\\.")[1]);
                case NON_VERSION:
                case UNDEFINED:
                    return 0;
                default:
                    throw new ExternalServicesException("Не реализована поддержка типа версии " + versionType.name());
            }
        } catch (final NumberFormatException e) {
            log.error("Произошла ошибка при попытке получить мажор версии {}", version);
        }
        return 0;

    }

    private int getHotfix(final String version) {
        try {
            switch (versionType) {
                case SBERCLASS:
                case HASURA:
                case ACC_LANDING:
                case LIT:
                    return Integer.parseInt(version.replace(getPrefix(version), "").split("[.-]")[2]);
                case DASH:
                    return Integer.parseInt(version.split("\\.")[2]);
                case MFE:
                case NON_VERSION:
                case UNDEFINED:
                    return 0;
                default:
                    throw new ExternalServicesException("Не реализована поддержка типа версии " + versionType.name());
            }
        } catch (final NumberFormatException e) {
            log.error("Произошла ошибка при попытке получить мажор версии {}", version);
        }
        return 0;
    }

    private String getSuffix(final String version) {
        switch (versionType) {
            case UNDEFINED:
            case NON_VERSION:
            case HASURA:
            case DASH:
                return "";
            case SBERCLASS:
            case ACC_LANDING:
            case LIT:
                return version.contains("rc") ? "rc" : "";
            case MFE:
                return version.contains("mfe") ? "mfe" : "";
            default:
                throw new ExternalServicesException("Не реализована поддержка типа версии " + versionType.name());
        }
    }

    private double getRc(final String version) {
        try {
            switch (versionType) {
                case SBERCLASS:
                case ACC_LANDING:
                case LIT:
                    return version.contains("-rc") ? Double.parseDouble(version.split("-rc")[1]) : 0d;
                case MFE:
                    final String[] mfeParts = version.split("-mfe");
                    return mfeParts.length > 1 ? Double.parseDouble(mfeParts[1]) : 0d;
                case HASURA:
                case NON_VERSION:
                case UNDEFINED:
                case DASH:
                    return 0;
                default:
                    throw new ExternalServicesException("Не реализована поддержка типа версии " + versionType.name());
            }
        } catch (final NumberFormatException e) {
            log.error("Произошла ошибка при попытке получить мажор версии {}", version);
        }
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ReleaseVersion that = (ReleaseVersion) o;

        if (major != that.major) {
            return false;
        }
        if (minor != that.minor) {
            return false;
        }
        if (hotfix != that.hotfix) {
            return false;
        }
        return Double.compare(that.rc, rc) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        final long temp;
        result = major;
        result = 31 * result + minor;
        result = 31 * result + hotfix;
        temp = Double.doubleToLongBits(rc);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public int compareTo(final ReleaseVersion o) {
        if (major == o.getMajor()) {
            if (minor == o.getMinor()) {
                if (hotfix == o.getHotfix()) {
                    return (int) (rc - o.getRc());
                }
                return hotfix - o.getHotfix();
            }
            return minor - o.getMinor();
        }
        return major - o.getMajor();
    }
}
