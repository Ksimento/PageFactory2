package ru.sbt.edu_power.notification;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.sbt.edu_power.external_services.mattermost.Regressman;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Table {
    Date date = new Date();

    final DalyProjectValue dalyProjectValue;

    Table(final DalyProjectValue dalyProjectValue) {

        this.dalyProjectValue = dalyProjectValue;
    }

    public void mmNotification(final Document document) {

    }

    public void mmNotificationDaly(final Document document) {
        sendMessage(getNameUserToWeek(document), dalyProjectValue.textNotification);
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.MONDAY && dalyProjectValue.smokeJobUrl.equals("")) {
            sendMessage(getUserNext(document),
                    String.format("Привет, напоминаю что на следующей неделе ты дежуришь! [Инструкция по дежурству](%s)",
                            DalyProjectValue.getHttpViewpage()+  DalyProjectValue.EDU.getDalyInfo())
            );
        }

    }

    private Integer getNumberCell(final Elements tr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat dateFormatTr = new SimpleDateFormat(".yyyy");
        List<String> header = tr.get(0).getElementsByTag("th").eachText();
        for (int i = 3; i < header.size(); i++) {
            try {
                if (dateFormat.parse(header.get(i) + dateFormatTr.format(date)).after(date)) {
                    return i - 1;
                }
            } catch (ParseException e) {
                sendMessage(
                        dalyProjectValue.getAutomationLead(),
                        String.format(
                                "Ошибка парсинга 1 строки колонки \"%s\", формат даты должен быть dd.MM, значение в ячейке \"%s\"",
                                i,
                                header.get(i)
                        )
                );
            }
        }
        sendMessage(
                dalyProjectValue.getAutomationLead(),
                String.format(
                        "Не найдена колонка для дежурного \"%s\", возможно отсутствует зеленый цвет для строки в колонке",
                        dalyProjectValue.name()
                )
        );
        throw new TableDalyException("Не удалось найти колонку с датой дежурства");
    }

    private String getNameUserToWeek(final Document document) {
        final Elements tr = document.body().getElementsByTag("tr");
        final Integer numberHeader = getNumberCell(tr);
        tr.remove(0);
        return getUserNameInCell(tr, numberHeader);
    }

    private String getUserNext(final Document document) {
        final Elements tr = document.body().getElementsByTag("tr");
        final Integer numberHeader = getNumberCell(tr) + 1;
        tr.remove(0);
        return getUserNameInCell(tr, numberHeader);
    }

    private String getUserNameInCell(final Elements trElements, final Integer numberHeader) {
        for (Element element : trElements) {
            if (element.getElementsByTag("td").get(numberHeader).attr("title").contains("зеленый")) {
                return element.getElementsByAttribute("data-username").attr("data-username").toLowerCase();
            }
        }
        sendMessage(
                dalyProjectValue.getAutomationLead(),
                String.format(
                        "Не найден User для проекта \"%s\" требуется проверить таблицу",
                        dalyProjectValue.name()
                )
        );
        throw new TableDalyException("Не удалось найти юзера для дежурства на текущую неделю");
    }

    private void sendMessage(final String nameQa, final String massage) {
        Regressman.getInstance().sendMessageToMmByUserName(nameQa, massage);
    }

    public Date getDate() {
        return date;
    }
}
