package ru.sbt.edu_power.external_services.version_releases;

import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.bitbucket.BBConnection;
import ru.sbt.edu_power.external_services.bitbucket.models.BBRepos;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchModel;
import ru.sbt.edu_power.external_services.bitbucket.models.BranchRestrictionModel;
import ru.sbt.edu_power.external_services.jira.JiraConnect;
import ru.sbt.edu_power.external_services.jira.agile.model.JiraUser;
import ru.sbt.edu_power.external_services.validator.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Загрузчик релизных веток из битбакета плюс парсинг прав на мердж в эти ветки
public class ReleaseBranchHelper {
    // все релизные ветки, кроме -rc* веток
    private final List<BranchModel> mainReleaseBranches;
    // правила доступа к веткам
    private final List<BranchRestrictionModel> branchRestrictions;
    private final VersionType versionType;

    public ReleaseBranchHelper(final BBRepos repo) {
        versionType = determineVersionType(repo);
        mainReleaseBranches = BBConnection
                .getBranches(repo, "r/")
                .stream()
                .filter(b -> b.getDisplayId().startsWith("r/"))
                .filter(b -> {
                    switch (versionType) {
                        case SBERCLASS:
                            return !b.getDisplayId().contains("-rc");
                        case MFE:
                            final Pattern pattern1 = Pattern.compile("^r/\\d*\\.\\d*$");
                            final Pattern pattern2 = Pattern.compile("^r/\\d*\\.\\d*-mfe\\d*$");
                            return pattern1.matcher(b.getDisplayId()).find() || pattern2.matcher(b.getDisplayId()).find();
                        default:
                            return false;
                    }
                })
                .collect(Collectors.toList());
        branchRestrictions = BBConnection.getBranchRestrictions(repo);
    }

    // Получить все открытые для мерджа ветки со списком пользователей, для кого они открыты. Если список пользователей пустой - открыты для всех
    public Map<BranchModel, List<JiraUser>> getAllOpenedMainBranches() {
        final Map<BranchModel, List<JiraUser>> openedBranches = new HashMap<>();
        mainReleaseBranches.forEach(b -> {
            if (isBranchNotReadOnly(b)) {
                openedBranches.put(b, new ArrayList<>());
            } else {
                final List<JiraUser> users = getAllowedUserForReadOnlyBranch(b);
                if (!users.isEmpty()) {
                    openedBranches.put(b, users);
                }
            }
        });
        return openedBranches;
    }

    // Получить список веток, в которые нужно сделать черипик относительно указанной версии, куда делается мердж
    public String getBranchesForCherryPick(final String version) {
        final ReleaseVersion releaseVersion = new ReleaseVersion(version);
        final List<String> branchesForCherryPick = mainReleaseBranches
                .stream()
                .filter(b -> {
                    final ReleaseVersion branchVersion = new ReleaseVersion(b.getDisplayId());
                    return branchVersion.compareTo(releaseVersion) > 0;
                })
                .sorted(Comparator.comparing(b -> new ReleaseVersion(b.getDisplayId())))
                .map(this::formatBranchWithStatus)
                .collect(Collectors.toList());
        branchesForCherryPick.add("*master*");
        return String.join("\n", branchesForCherryPick);
    }

    private String formatBranchWithStatus(final BranchModel branch) {
        final boolean readOnly = !isBranchNotReadOnly(branch);
        final List<JiraUser> users = new ArrayList<>();
        if (readOnly) {
            users.addAll(getAllowedUserForReadOnlyBranch(branch));
        }
        final String restriction;
        if (readOnly) {
            if (users.isEmpty()) {
                restriction = " заблокирована на влитие";
            } else {
                restriction = " влитие разрешено только: " +
                              users.stream().map(JiraUser::getDisplayName)
                                   .collect(Collectors.joining(", "));
            }
        } else {
            restriction = "";
        }
        return String.format(
                "*%s*%s",
                branch.getDisplayId(),
                restriction
        );
    }

    private boolean isBranchNotReadOnly(final BranchModel branch) {
        return branchRestrictions.stream()
                                 .filter(br -> match(br, branch))
                                 .noneMatch(br -> br.getType() == BranchRestrictionModel.RestrictionType.READ_ONLY);
    }

    private List<JiraUser> getAllowedUserForReadOnlyBranch(final BranchModel branch) {
        return branchRestrictions.stream()
                                 .filter(br -> match(br, branch))
                                 .filter(br -> br.getType() == BranchRestrictionModel.RestrictionType.READ_ONLY)
                                 .filter(br -> !br.getUsers().isEmpty())
                                 .map(BranchRestrictionModel::getUsers)
                                 .flatMap(Set::stream)
                                 .map(u -> JiraConnect.jiraGetUser(u.getName()))
                                 .collect(Collectors.toList());
    }

    private boolean match(final BranchRestrictionModel branchRestriction, final BranchModel branch) {
        return branchRestriction.isBranch() && branchRestriction.getBranchName().equals(branch.getDisplayId()) ||
               branchRestriction.isPattern() &&
               Validator.matchValues(branch.getDisplayId(), branchRestriction.getBranchName());
    }

    private VersionType determineVersionType(final BBRepos repo) {
        switch (repo) {
            case MFE_DEPLOY_VERSION:
                return VersionType.MFE;
            case EDU_BACK:
            case EDU_FRONT:
            case S21_APPLICATION:
            case S21_E2E_JAVA_APP:
            case S21_APPLICATION_EXAM:
                return VersionType.SBERCLASS;
            default:
                throw new ExternalServicesException("Не удалось распознать тип версионирования для репозитория " +
                                             repo.name());
        }
    }
}
