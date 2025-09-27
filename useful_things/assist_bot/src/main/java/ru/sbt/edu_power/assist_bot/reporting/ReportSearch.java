package ru.sbt.edu_power.assist_bot.reporting;

import com.slack.api.model.Message;
import com.slack.api.model.User;
import ru.sbt.edu_power.assist_bot.services.channel_reader.ChannelReader;
import ru.sbt.edu_power.assist_bot.slack.users.SlackUsers;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// Класс для поиска сообщений, являющихся репортами
public class ReportSearch {
    private final String chanelId;
    private final ReportType reportType;
    private final String userId;

    public ReportSearch(final String chanelId, final ReportType reportType, final String userId) {
        this.chanelId = chanelId;
        this.reportType = reportType;
        this.userId = userId;
    }

    // возвращает список сообщений, отсортированных в обратном порядке (первым идёт самое новое)
    public List<Message> search() {
        final ChannelReader channelReader = new ChannelReader(chanelId, userId, 200);
        final User user = SlackUsers.getInstance().getUserByName("regressman");
        channelReader.waitWhenChanelBeRead();
        return channelReader.search(user)
                            .stream()
                            .filter(m -> {
                                if (Objects.nonNull(m
                                        .getBlocks())) {
                                    return m.getBlocks().stream()
                                            .anyMatch(lb -> lb.toString().contains(reportType.getReportName()));
                                }
                                return false;
                            })
                            .sorted(Comparator.comparing(Message::getTs, Comparator.reverseOrder()))
                            .collect(Collectors.toList());
    }
}
