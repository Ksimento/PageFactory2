package ru.sbt.edu_power.e2e_core.devtools.network;


import ru.sbt.edu_power.external_services.validator.Validator;

import java.util.Arrays;

public enum ContentType {
    CSS("*css"),
    JS("*js"),
    GRAPHQL("*graphql"),
    STORAGE("*services/storage*"),
    REST("*services/rest*"),
    COLLECTOR("*collector*"),
    UNDEFINED("undefined");

    private final String typeName;

    ContentType(final String typeName) {
        this.typeName = typeName;
    }

    public static ContentType getContentTypeByPath(final String path) {
        return Arrays
                .stream(ContentType.values())
                .filter(type -> Validator.matchValues(path, type.typeName))
                .findFirst()
                .orElse(UNDEFINED);
    }
}
