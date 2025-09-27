package ru.sbt.edu_power.external_services.jira.tc_verifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import ru.sbt.edu_power.external_services.jira.JiraConnectionException;
import ru.sbt.edu_power.external_services.jira.tc_verifier.model.CustomFields;
import ru.sbt.edu_power.external_services.jira.test_manager.model.TestCaseModel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class NewJiraTCCollector {
    private static NewJiraTCCollector INSTANCE;
    private final Map<String, TestCaseModel> testCaseMap = new HashMap<>();
    private final TCLoader loader = new TCLoader();
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
    // projectId to custom fields map (custom field id to field name)
    private final Map<Integer, Map<Integer, String>> projectToCustomFieldMap = new HashMap<>();
    // custom field ID to options map (option ID to option name)
    private final Map<Integer, Map<Integer, String>> customFieldIdToOptionsMap = new HashMap<>();
    private final Map<Integer, TCFolder> folderMap = new HashMap<>();

    public NewJiraTCCollector() {
    }

    public synchronized TestCaseModel getById(final String testCaseKey) {
        if (testCaseMap.containsKey(testCaseKey)) {
            return testCaseMap.get(testCaseKey);
        }
        updateData(loader.download(testCaseKey));
        return testCaseMap.get(testCaseKey);
    }

    public NewJiraTCCollector collect(final TCQueryBuilder queryBuilder) {
        updateData(loader.download(queryBuilder));
        return this;
    }

    public NewJiraTCCollector collect(final TCFields field, final Object... dataType) {
        updateData(loader.download(field, dataType));
        return this;
    }

    public static NewJiraTCCollector getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NewJiraTCCollector();
        }
        return INSTANCE;
    }

    public final Map<String, TestCaseModel> asMap() {
        return testCaseMap;
    }

    private synchronized void downloadCustomFieldList(final Integer projectId) {
        if (!projectToCustomFieldMap.containsKey(projectId)) {
            projectToCustomFieldMap.put(projectId, new HashMap<>());
        }
        final JSONArray array = loader.downloadCustomFieldsSchema(projectId);
        for (final Object o : array) {
            final CustomFields customField = gson.fromJson(o.toString(), CustomFields.class);
            if (customField.getArchived()) {
                continue;
            }
            projectToCustomFieldMap.get(projectId).put(customField.getId(), customField.getName());
            customFieldIdToOptionsMap.put(customField.getId(), new HashMap<>());
            for (final CustomFields.CustomFieldOptions opt : customField.getOptions()) {
                if (opt.getArchived()) {
                    continue;
                }
                customFieldIdToOptionsMap.get(customField.getId()).put(opt.getId(), opt.getName());
            }
        }
    }

    public Integer getFolderId(final String name, final Integer projectId) {
        return getFolder(projectId).getIdByName(name);
    }

    public TCFolder getFolder(final Integer projectId) {
        return getFolder(projectId, true);
    }

    public TCFolder getFolder(final Integer projectId, final boolean fromCache) {
        if (!fromCache || !folderMap.containsKey(projectId)) {
            final JSONObject o = loader.downloadFolderSchema(projectId);
            folderMap.put(projectId, gson.fromJson(o.toString(), TCFolder.class));
        }
        return folderMap.get(projectId);
    }

    private synchronized void updateData(final JSONArray dataArray) {
        for (final Object o : dataArray) {
            final TestCaseModel model = gson.fromJson(o.toString(), TestCaseModel.class);
            parseCustomField(model);
            testCaseMap.put(model.getKey(), model);
        }
    }

    public void parseCustomField(final TestCaseModel model) {
        if (!projectToCustomFieldMap.containsKey(model.getProjectId())) {
            downloadCustomFieldList(model.getProjectId());
        }
        for (final TestCaseModel.CustomFieldValues value : model.getCustomFieldValues()) {
            final EnumMap<TCFields, Object> map = parseCustomField(value, model);
            if (map != null) {
                model.putCustomFields(map);
            }
        }
    }

    public void updateModel(final String testCaseKey, final TCFields field, final Object value) {
        updateModel(testCaseMap.get(testCaseKey), field,value);
    }

    public void updateModel(final TestCaseModel model, final TCFields field, final Object value) {
        model.update(field, value);
    }

    private EnumMap<TCFields, Object> parseCustomField(
            final TestCaseModel.CustomFieldValues values,
            final TestCaseModel model
    ) {

        final Integer fieldId = values.getCustomFieldId();
        final Integer valueId = values.getIntValue();
        final String stringValue = values.getStringValue();
        final Boolean booleanValue = values.getBooleanValue();
        final String key = model.getKey();
        final TCFields field = TCFields.getFieldByName(projectToCustomFieldMap.get(model.getProjectId()).get(fieldId));
        final Object value;
        switch (field) {
            case IGNORED:
                return null;
            case AUTOMATED_STATUS:
                value = valueId == null ? null : TCFields.AutomatedStatus.getByValue(customFieldIdToOptionsMap.get(fieldId).get(valueId), key);
                break;
            case TEST_TYPE:
                value = valueId == null ? null : TCFields.TestType.getByValue(customFieldIdToOptionsMap.get(fieldId).get(valueId), key);
                break;
            case TEST_VIEW:
                value = valueId == null ? null : TCFields.TestView.getByValue(customFieldIdToOptionsMap.get(fieldId).get(valueId), key);
                break;
            case RISK:
                value = valueId == null ? null : TCFields.Risk.getByValue(customFieldIdToOptionsMap.get(fieldId).get(valueId), key);
                break;
            case TEAM:
                value = valueId == null ? null : customFieldIdToOptionsMap.get(fieldId).get(valueId);
                break;
            case LAYOUT_DESKTOP:
            case LAYOUT_MOBILE_PORTRAIT:
            case LAYOUT_TABLET_PORTRAIT:
            case LAYOUT_TABLET_LANDSCAPE:
                value = valueId == null ? null : TCFields.Layout.getByValue(customFieldIdToOptionsMap.get(fieldId).get(valueId), key);
                break;
            case FRAMEWORK:
                final List<TCFields.Framework> frameworks = new ArrayList<>();
                if (stringValue != null) {
                    frameworks.addAll(
                            Stream.of(stringValue.split("-"))
                                  .filter(s -> !s.isEmpty())
                                  .map(Integer::parseInt)
                                  .map(customFieldIdToOptionsMap.get(fieldId)::get)
                                  .map(TCFields.Framework::getByValue)
                                  .collect(Collectors.toList())
                    );
                }
                if (frameworks.isEmpty()) {
                    frameworks.add(TCFields.Framework.EMPTY);
                }
                value = frameworks;
                break;
            case NOT_AUTOMATED_REASON:
            case AUTOMATOR:
                value = Objects.isNull(stringValue) ? "" : stringValue;
                break;
            case HANDLE_RISK_MANAGEMENT:
                value = booleanValue;
                break;
            default:
                throw new JiraConnectionException("Не определено поведение для поля " + field.value);
        }
        final EnumMap<TCFields, Object> map = new EnumMap<>(TCFields.class);
        map.put(field, value);
        return map;
    }

}
