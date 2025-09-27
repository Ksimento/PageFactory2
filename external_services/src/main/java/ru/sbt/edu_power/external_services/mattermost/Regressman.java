package ru.sbt.edu_power.external_services.mattermost;

import jakarta.ws.rs.core.Response;
import net.bis5.mattermost.client4.ApiResponse;
import net.bis5.mattermost.client4.MattermostClient;
import net.bis5.mattermost.model.Channel;
import net.bis5.mattermost.model.Post;
import net.bis5.mattermost.model.PostType;
import net.bis5.mattermost.model.User;
import ru.sbt.edu_power.external_services.ExternalServicesException;
import ru.sbt.edu_power.external_services.PropReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Regressman {
    public static final int MAX_LENGTH_MESSAGE = 16380;
    private final MattermostClient client = new MattermostClient(PropReader.get("mattermost.url"));
    private static final String BOT_TOKEN = PropReader.get("mattermost.bot.token");
    private static final String TEAM = "slack-export";
    private static Regressman INSTANCE;
    private static final String REGRESSMAN_USER_ID = PropReader.get("mattermost.bot.id");

    public static Regressman getInstance() {
        if (Objects.isNull(INSTANCE)) {
            INSTANCE = new Regressman();
            INSTANCE.client.setAccessToken(BOT_TOKEN);
        }
        return INSTANCE;
    }

    public Channel getChannelByName(final String channelName) {
        final ApiResponse<Channel> response = client.getChannelByNameForTeamName(channelName, TEAM);
        if (response.hasError()) {
            throw new ExternalServicesException(String.format(
                    "Не удалось получить канал %s:\n%s",
                    channelName,
                    response.readError().getDetailedError()
            ));
        }
        return response.readEntity();
    }

    public void sendPostByChannelName(final String channel, final String message) {
        sendPost(getChannelByName(channel).getId(), message, PostType.DEFAULT);
    }

    public void sendPost(final String channelId, final String message, final PostType postType) {
        final List<String> listMessage = checkingLengthMessage(message);
        listMessage.forEach(newMessage -> {
            final Post post = new Post(channelId, newMessage);
            post.setType(postType);
            final ApiResponse<Post> response = client.createPost(post);
            if (response.hasError()) {
                throw new ExternalServicesException(String.format(
                        "Не удалось отправить сообщение '%s' в канал '%s':\n%s",
                        newMessage,
                        channelId,
                        response.readError().getDetailedError()
                ));
            }
        });
    }

    private List<String> checkingLengthMessage(final String message){
        final List <String> listMessage = new ArrayList<>();
        if (message.length() > MAX_LENGTH_MESSAGE){
            listMessage.add(message.substring(0, MAX_LENGTH_MESSAGE));
            listMessage.add(message.substring(MAX_LENGTH_MESSAGE));
        } else {
            listMessage.add(message);
        }
        return listMessage;
    }

    public void sendPostMessageInThread(
            final String channelName,
            final String messageHeader,
            final String messageThread,
            final Path filePath
    ) {
        final String channelId = getChannelByName(channelName).getId();
        final Post post = new Post(channelId, messageHeader);

        if (Objects.nonNull(filePath)){
            List<String> listIdAttachFile = new ArrayList<>();
            String idUploadFile = null;
            try {
                String entityFile = Regressman.getInstance().getClient().uploadFile(channelId, filePath)
                        .getRawResponse().readEntity(String.class);
                if (entityFile.contains("id\":\"")) {
                    idUploadFile = entityFile.substring(entityFile.indexOf("id\":\"") + 5, entityFile.indexOf("\","));
                    listIdAttachFile.add(idUploadFile);
                    post.setFileIds(listIdAttachFile);
                }
            } catch (final IOException ignored) {

            }
        }
        post.setType(PostType.SLACK_ATTACHMENT);
        final Post postThread = new Post(channelId, messageThread);
        final String response = client.createPost(post).getRawResponse().readEntity(String.class);
        postThread.setRootId(response.substring(response.indexOf("id\":\"") + 5, response.indexOf("\",")));
        client.createPost(postThread);
    }

    public String getUserTeamByUserName(String userName) {
        if (Objects.nonNull(userName)) {
            List<User> userMMList = MattermostUsers.getInstance().getUserMMList();
            User userMm = userMMList
                    .stream()
                    .filter(u -> u.getUsername().toLowerCase().equals(userName.toLowerCase()))
                    .findFirst()
                    .orElse(new User());

            if (Objects.nonNull(userMm.getLastName())) {
                return userMm.
                        getLastName().
                        contains("(") ? userMm.getLastName().substring(userMm.getLastName().indexOf("(")) : "no Team";
            }
        }
        return "no Team";
    }

    public void sendMessageToMmByUserName(String userName, String message) {
        List<User> userMMList = MattermostUsers.getInstance().getUserMMList();
        User user = userMMList
                .stream()
                .filter(u -> u.getUsername().toLowerCase().equals(userName))
                .findFirst()
                .orElse(new User());
        if (Objects.nonNull(user.getId())) {
            String channel = getClient()
                    .createDirectChannel(user.getId(), getRegressmanUserId()).readEntity().getId();

            final Post post = new Post(channel, message);
            getClient().createPost(post);
        }
    }

    public static String getRegressmanUserId() {
        return REGRESSMAN_USER_ID;
    }

    public MattermostClient getClient() {
        return client;
    }
}
