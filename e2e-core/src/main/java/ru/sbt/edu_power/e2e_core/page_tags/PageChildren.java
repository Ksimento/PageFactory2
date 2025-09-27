package ru.sbt.edu_power.e2e_core.page_tags;

import lombok.Data;

import java.util.List;

@Data
public class PageChildren {
    private final String name;
    private final String tag;
    private final List<PageChildren> childrenList;

    public String getTagByPageName(final String pageName) {
        if (pageName.equals(name)) {
            return tag;
        }
        return childrenList
                .stream()
                .map(children -> children.getTagByPageName(pageName))
                .filter(childTag -> !childTag.isEmpty())
                .findFirst()
                .orElse("");
    }
}
