package ru.sbt.edu_power.e2e_core.elements.multiple_type_content;

import lombok.Getter;

@Getter
enum ContentType {
    IMAGE(
            "descendant-or-self::img",
            "descendant-or-self::*[@src]"
    ),
    AUDIO(
            "descendant-or-self::audio | descendant-or-self::*[@type = 'audio/mpeg']",
            "descendant-or-self::*[@src]"),
    VIDEO(
            "descendant-or-self::video[not(descendant-or-self::*[@type = 'audio/mpeg'])]",
            "descendant-or-self::*[@src]"
    ),
    PDF(
            "descendant-or-self::embed",
            "descendant-or-self::*[@type]"
    ),
    IFRAME(
            "descendant-or-self::iframe",
            "descendant-or-self::*[@src]"
    ),
    KATEX(
            "descendant-or-self::span[@class = 'mjx-chtml MathJax_CHTML']",
            "descendant-or-self::span[@class = 'mjx-math']"
    ),
    TEXT(
            "descendant-or-self::*[string-length(text()) > 0 and not(ancestor::*[@class = 'mjx-chtml MathJax_CHTML'])]",
            "."
    );

    private final String identificationXpath;
    private final String valueElementXpath;

    ContentType(final String identificationXpath, final String valueElementXpath) {
        this.identificationXpath = identificationXpath;
        this.valueElementXpath = valueElementXpath;
    }
}
