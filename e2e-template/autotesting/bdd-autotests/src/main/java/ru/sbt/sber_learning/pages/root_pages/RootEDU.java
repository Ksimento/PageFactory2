package ru.sbt.sber_learning.pages.root_pages;

import ru.sbt.edu_power.e2e_core.page_tags.RootPage;
import ru.sbt.edu_power.e2e_core.smoke.EndPoints;
import ru.sbtqa.tag.pagefactory.annotations.PageEntry;

/**
 * Класс реализует иерархию страниц платформы
 * Корневая страница для ШЦП
 */
@PageEntry(title = "ШЦП")
@EndPoints(ignored = true)
public class RootEDU extends RootPage {
}
