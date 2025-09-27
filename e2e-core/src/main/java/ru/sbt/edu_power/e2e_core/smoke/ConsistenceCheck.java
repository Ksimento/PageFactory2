package ru.sbt.edu_power.e2e_core.smoke;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import ru.sbt.edu_power.e2e_core.page_tags.ParentPage;
import ru.sbt.edu_power.e2e_core.page_tags.RootPage;
import ru.sbt.edu_power.e2e_core.widgets.Widget;
import ru.sbtqa.tag.pagefactory.Page;
import ru.sbtqa.tag.pagefactory.PageManager;
import ru.sbtqa.tag.qautils.errors.AutotestError;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ConsistenceCheck {
    private final List<String> projectsFromRoles = new ArrayList<>();
    private final List<String> projectList = Stream.of(Projects.values()).map(Enum::name).collect(Collectors.toList());

    public ConsistenceCheck() {
        collectProjectsFromRoles();
    }

    private void collectProjectsFromRoles() {
        for (final Roles role : Roles.values()) {
            final String[] roleParts = role.name().split("_");
            Assert.assertTrue(
                    "Название роли должно содержать минимум одни знак подчёркивания " + role.name(),
                    roleParts.length>=
                    2
            );
            if (!projectsFromRoles.contains(roleParts[roleParts.length-1])) {
                projectsFromRoles.add(roleParts[roleParts.length-1]);
            }
        }
    }

    // проверяем, что для каждого проекта есть роли
    public void matchingRoles() {
        Assert.assertEquals("Набор проектов не совпадает в классах Roles и Projects", projectsFromRoles, projectList);
    }

    // проверяем, что в аннотации EndPoints есть все роли по проекту, представленные в Roles
    public void matchingRoleFunctions() {
        final List<String> endPointsMethods = Stream.of(EndPoints.class.getDeclaredMethods())
                                                    .filter(method -> String.class.isAssignableFrom(method.getReturnType()))
                                                    .map(Method::getName)
                                                    .filter(name -> !"toString".equals(name))
                                                    .collect(Collectors.toList());
        final List<String> endPointMethodsFromRoles = Stream.of(Roles.values())
                                                            .map(Roles::getEndPointName)
                                                            .collect(Collectors.toList());
        new ArrayList<>(endPointsMethods).forEach(method -> {
            if (endPointMethodsFromRoles.contains(method)) {
                endPointMethodsFromRoles.remove(method);
                endPointsMethods.remove(method);
            }
        });
        Assert.assertEquals(
                "Состав методов EndPoints не соответствует составу методов в ролях Roles",
                endPointsMethods,
                endPointMethodsFromRoles
        );
    }

    // находим страницы в которых не указаны роуты и не отмечен признак ignored = true
    public void searchPagesWithoutEndPoints() {
        PageManager.getPageRepository()
                   .keySet()
                   .forEach(pClass -> {
                       if (Widget.class.isAssignableFrom(pClass)) {
                           return;
                       }
                       if (pClass.isAnnotationPresent(EndPoints.class)) {
                           final boolean ignored = pClass.getAnnotation(EndPoints.class).ignored();
                           if (ignored) {
                               return;
                           }
                           if (RoutEvaluator.getRoleToRouteMap(pClass).isEmpty()) {
                               log.info("Страница {} содержит пустую аннотацию EndPoints", pClass.getSimpleName());
                           }
                       } else {
                           log.info("Страница {} не содержит аннотацию EndPoints", pClass.getSimpleName());
                       }
                   });
    }

    // находим страницы, в которых проекты в EndPoints не соответствуют проектам из ParentPage
    public void matchEndPointsAndParentPageProjects() {
        PageManager.getPageRepository()
                   .keySet()
                   .forEach(pClass -> {
                       final Set<String> parentPageProjects = getProjectsFromParentPage(pClass);
                       final Set<String> endPointsProjects = getRolesFromEndPoints(pClass)
                               .stream()
                               .map(Roles::name)
                               .map(r -> r.split("_")[1])
                               .collect(Collectors.toSet());
                       if (endPointsProjects.isEmpty() || parentPageProjects.isEmpty()) {
                           return;
                       }
                       if (!parentPageProjects.equals(endPointsProjects)) {
                           log.info(
                                   "Набор проектов в EndPoints не соответствует набору проектов в ParentPage для страницы {}",
                                   pClass.getSimpleName()
                           );
                       }
                   });
    }

    // находим страницы в которых роли в EndPoints не соответствуют ролям из ParentPage
    public void matchEndPointsAndParentPageRoles() {
        PageManager.getPageRepository()
                   .keySet()
                   .forEach(pClass -> {
                       final Set<String> parentPageRoles = getPageRootClasses(pClass, new HashSet<>(), 0)
                               .stream()
                               .map(Class::getSimpleName)
                               .map(String::toLowerCase)
                               .collect(Collectors.toSet());
                       final Set<String> endPointsRoles = getRolesFromEndPoints(pClass)
                               .stream()
                               .map(Roles::getEndPointName)
                               .map(String::toLowerCase)
                               .collect(Collectors.toSet());
                       if (endPointsRoles.isEmpty() || parentPageRoles.isEmpty()) {
                           return;
                       }
                       if (!parentPageRoles.equals(endPointsRoles)) {
                           log.info(
                                   "Набор ролей в EndPoints не соответствует набору ролей в ParentPage для страницы {}",
                                   pClass.getSimpleName()
                           );
                       }

                   });
    }

    // получаем список проектов в которых используется эта страница по аннотации ParentPage
    private Set<String> getProjectsFromParentPage(final Class<? extends Page> pClass) {
        final Set<String> set = new HashSet<>();
        if (
                pClass.isAnnotationPresent(ParentPage.class) &&
                !pClass.getAnnotation(ParentPage.class).ignored()
        ) {
            final Set<Class<? extends Page>> pClasses = getPageRootClasses(pClass, new HashSet<>(), 0);
            set.addAll(pClasses.stream()
                             .map(Class::getSimpleName)
                             .map(r -> projectList
                                     .stream()
                                     .filter(r::endsWith)
                                     .findFirst()
                                     .orElseThrow(() -> new AutotestError(
                                             "Не найдено проекта для страницы " + r)))
                             .collect(Collectors.toList())
            );

        }
        return set;
    }

    // получаем список проектов в которых используется эта страница по аннотации EndPoints
    private Set<Roles> getRolesFromEndPoints(final Class<? extends Page> pClass) {
        final Set<Roles> set = new HashSet<>();
        if (
                pClass.isAnnotationPresent(EndPoints.class) &&
                !pClass.getAnnotation(EndPoints.class).ignored()
        ) {
            final Map<Roles, String> map = RoutEvaluator.getRoleToRouteMap(pClass);
            set.addAll(map.keySet());
        }
        return set;
    }

    // метод перебирает вглубь родительские страницы пока не найдёт корневую, по которой можно получить принадлежность к проекту
    private Set<Class<? extends Page>> getPageRootClasses(final Class<? extends Page> rtClass, final Set<Class<? extends Page>> rootPageSet, int depth) {
        if (depth > 20) {
            log.error("Перекрёстные ссылки на страницы {}", rtClass.getSimpleName());
        }
        if (depth > 30) {
            throw new AutotestError("В аннотации @ParentPage перечисленных страниц произошла рекурсия");
        }
        depth++;
        if (rtClass.getSuperclass().isAssignableFrom(RootPage.class)) {
            rootPageSet.add(rtClass);
            return rootPageSet;
        }
        if (
                rtClass.isAnnotationPresent(ParentPage.class)
                && !rtClass.getAnnotation(ParentPage.class).ignored()
        ) {
            for (final Class<? extends Page> parentPage : rtClass.getAnnotation(ParentPage.class).pageClass()) {
                rootPageSet.addAll(getPageRootClasses(parentPage, new HashSet<>(), depth));
            }
        }
        return rootPageSet;
    }
}
