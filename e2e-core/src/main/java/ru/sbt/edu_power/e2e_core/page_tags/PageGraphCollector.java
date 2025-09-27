package ru.sbt.edu_power.e2e_core.page_tags;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.error_processing.ErrorCollector;
import ru.sbt.edu_power.e2e_core.widgets.Widget;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.PageManager;
import ru.sbtqa.tag.pagefactory.annotations.PageEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class PageGraphCollector {
    private static Function<Class<? extends Page>, Boolean> isRootPage;
    private final MutableGraph<Class<? extends Page>> graph;
    private final List<PageChildren> pageChildren = new ArrayList<>();
    private final List<String> allPageClassNames = new ArrayList<>();
    private final List<String> allPageTitles = new ArrayList<>();
    private final ErrorCollector errorCollector = new ErrorCollector();

    public static void configure(final Function<Class<? extends Page>, Boolean> isRootPage) {
        PageGraphCollector.isRootPage = isRootPage;
    }

    public PageGraphCollector() {
        graph = GraphBuilder.directed().allowsSelfLoops(false).build();
    }

    public void collect() {
        PageManager.getPageRepository().keySet().forEach(page -> {
            allPageClassNameCheck(page);
            allPageTitlesCheck(page);
            if (Widget.class.isAssignableFrom(page)) {
                return;
            }
            if (page.isAnnotationPresent(ParentPage.class)) {
                if (page.getAnnotation(ParentPage.class).ignored()) {
                    return;
                }
                graph.addNode(page);
                for (final Class<? extends Page> parentPage : page.getAnnotation(ParentPage.class).pageClass()) {
                    graph.putEdge(parentPage, page);
                }
            } else {
                final boolean isNotRoot = !isRootPage.apply(page);
                if (isNotRoot) {
                    log.info("Не указана родительская страница для класса {}", page.getName());
                }
            }
        });
        errorCollector.assertAll();
    }

    private void allPageClassNameCheck(final Class<? extends Page> page) {
        errorCollector.assertFalse(
                String.format("Повторяющееся имя класса страницы '%s'", page.getSimpleName()),
                allPageClassNames.contains(page.getSimpleName())
        );
        allPageClassNames.add(page.getSimpleName());
    }

    private void allPageTitlesCheck(final Class<? extends Page> page) {
        if (Widget.class.isAssignableFrom(page)) {
            return;
        }
        errorCollector.assertTrue(
                String.format(
                        "Класс '%s' не содержит PageEntry аннотацию", page.getName()),
                page.isAnnotationPresent(PageEntry.class)
        );
        Assert.assertTrue(
                String.format("Страница %s не содержит аннотацию @PageEntry", page.getSimpleName()),
                page.isAnnotationPresent(PageEntry.class)
        );
        errorCollector.assertFalse(
                String.format(
                        "Повторяющееся название страницы '%s'",
                        page.getAnnotation(PageEntry.class).title()
                ),
                allPageTitles.contains(page.getAnnotation(PageEntry.class).title())
        );
        allPageTitles.add(page.getAnnotation(PageEntry.class).title());
    }

    public void buildChildrenList() {
        for (final Class<? extends Page> node : graph.nodes()) {
            if (graph.inDegree(node) == 0) {
                pageChildren.add(createChildren(node));
            }
        }
    }

    public List<PageChildren> getPageChildren() {
        return pageChildren;
    }

    private PageChildren createChildren(final Class<? extends Page> node) {
        final String pageTitle;
        if (node.isAnnotationPresent(PageEntry.class)) {
            pageTitle = node.getAnnotation(PageEntry.class).title();
        } else {
            pageTitle = "without title";
        }
        final String tag;
        if (node.isAnnotationPresent(ParentPage.class)) {
            tag = "@PG_" + node.getSimpleName();
        } else {
            tag = "";
        }
        final List<PageChildren> pageChildrenList = new ArrayList<>();
        if (graph.outDegree(node) > 0) {
            for (final Class<? extends Page> child : graph.successors(node)) {
                pageChildrenList.add(createChildren(child));
            }
        }
        return new PageChildren(pageTitle, tag, pageChildrenList);
    }

}
