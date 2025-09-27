package ru.sbt.edu_power.e2e_core.api.gitlabRequests;


import com.google.gson.Gson;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import ru.sbtqa.tag.qautils.errors.AutotestError;
import ru.sbtqa.tag.qautils.properties.Props;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
@Deprecated
public class APIGitlabOldRequest {

    private static final String LOGIN = Props.get("gitlab.login");
    private static final String PASS = Props.get("gitlab.pass");

    public static String getNameProject(final String url) {
        final String[] pathList = url.split("/");
        for (final String path : pathList) {
            if (path.contains("ID")) {
                return path.replace(".git", "");
            }
        }
        throw new AutotestError("Не найдено название проекта");
    }

    public static String getIdProject(final String url) {
        final String project = getNameProject(url);
        final String endpoint = Props.get("gitlab.api.project.id").replace("{NAME}", project);

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(endpoint);
            httpGet.addHeader(LOGIN, PASS);
            try (final CloseableHttpResponse response = client.execute(httpGet)) {
                final HttpEntity entity = response.getEntity();
                // если нет тела ответа
                if (entity == null) {
                    throw new AutotestError("Нет ответа");
                }
                final String result = EntityUtils.toString(entity);
                final String[] dataList = result.split(",");
                for (final String data : dataList) {
                    if (data.contains("id")) {
                        return data.split(":")[1];
                    }
                }
            }
        } catch (final IOException e) {
            log.debug("APIGitlabRequest exception={}", e.toString());
        }
        throw new AutotestError("Не найден ID проекта");
    }


    public static int getIdUser(final String username) {
        String actualUrl = Props.get("gitlab.get.user").replace("{NAME}", username);
        int id = 0;

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet httpGet = new HttpGet(actualUrl);
            httpGet.addHeader(LOGIN, PASS);
            try (final CloseableHttpResponse response = client.execute(httpGet)) {
                final HttpEntity entity = response.getEntity();
                final String result = EntityUtils.toString(entity);
                final JSONArray jsonArray = new JSONArray(result);

                id = IntStream.range(0, jsonArray.length())
                              .filter(index -> ((JSONObject) jsonArray.get(index))
                                      .getString("username")
                                      .equals(username))
                              .mapToObj(index -> ((JSONObject) jsonArray.get(index)).getInt("id"))
                              .findFirst().orElse(0);
            }
        } catch (final JSONException e) {
            log.debug("JSONException exception={}", e.toString());
        } catch (final IOException e) {
            log.debug("APIGitlabRequest exception={}", e.toString());
        }
        return id;
    }


    private static List<NameValuePair> getAuthData() {
        final ArrayList<NameValuePair> auth = new ArrayList<>();
        auth.add(new BasicNameValuePair(LOGIN, PASS));
        return auth;
    }

    public static void createTag(final String url, final String tagName) {
        final Gson gson = new Gson();
        final String idProject = getIdProject(url);
        final Map<String, String> param = new HashMap<>();
        param.put("tag_name", tagName);
        final String query = gson.toJson(param);
        final String actualUrl = Props.get("gitlab.create.tag")
                                      .replace("{ID}", idProject)
                                      .replace("{TAGNAME}", tagName);
        try {
            final CloseableHttpClient client = HttpClientBuilder.create()
                                                                .setRetryHandler((exception, executionCount, context) ->
                                                                        executionCount <= 20)
                                                                .setServiceUnavailableRetryStrategy(new ServiceUnavailableRetryStrategy() {
                                                                    final int waitPeriod = 1000;

                                                                    @Override
                                                                    public boolean retryRequest(
                                                                            final HttpResponse response,
                                                                            final int executionCount,
                                                                            final HttpContext context
                                                                    ) {
                                                                        return executionCount <= 10 &&
                                                                               response
                                                                                       .getStatusLine()
                                                                                       .getStatusCode() >=
                                                                               400; //important!
                                                                    }

                                                                    @Override
                                                                    public long getRetryInterval() {
                                                                        return waitPeriod;
                                                                    }
                                                                })
                                                                .build();


            final HttpPost httpPost = new HttpPost(actualUrl);
            httpPost.addHeader(LOGIN, PASS);
            client.execute(httpPost);
        } catch (final IOException e) {
            log.debug("APIGitlabRequest exception={}", e.toString());
        }

    }

    public static void deleteTask(final String url) {
        final String idProject = getIdProject(url);
        final String actualUrl = Props.get("gitlab.delete.project")
                                      .replace("{ID}", idProject);

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpDelete httpDelete = new HttpDelete(actualUrl);
            httpDelete.addHeader(LOGIN, PASS);
            client.execute(httpDelete);
        } catch (final IOException e) {
            log.debug("APIGitlabRequest exception={}", e.toString());
        }
    }


    public static void createUser(final String username, final String email) {
        final String actualUrl = Props.get("gitlab.create.user");

        try {
            // реализация повтора запроса в случае неудачи
            final CloseableHttpClient client = HttpClientBuilder
                    .create()
                    .setRetryHandler((exception, executionCount, context) -> executionCount <= 10)
                    .setServiceUnavailableRetryStrategy(new ServiceUnavailableRetryStrategy() {
                        private static final int WAIT_PERIOD = 1000;

                        @Override
                        public boolean retryRequest(
                                final HttpResponse response,
                                final int executionCount,
                                final HttpContext context
                        ) {
                            return executionCount <= 10 &&
                                   response
                                           .getStatusLine()
                                           .getStatusCode() >=
                                   400; //important!
                        }

                        @Override
                        public long getRetryInterval() {
                            return WAIT_PERIOD;
                        }
                    })
                    .build();


            final HttpPost httpPost = new HttpPost(actualUrl);
            httpPost.addHeader(LOGIN, PASS);

            final ArrayList<NameValuePair> postParameters = new ArrayList<>();
            postParameters.add(new BasicNameValuePair("name", username));
            postParameters.add(new BasicNameValuePair("email", email));
            postParameters.add(new BasicNameValuePair("username", username));
            postParameters.add(new BasicNameValuePair("password", "password"));
            httpPost.setEntity(new UrlEncodedFormEntity(postParameters, StandardCharsets.UTF_8));

            client.execute(httpPost);
        } catch (final IOException e) {
            log.debug("APIGitlabRequest exception={}", e.toString());
        }
    }

    public static void deleteUser(final String username) {
        final int idUser = getIdUser(username);
        final String actualUrl = Props.get("gitlab.delete.user")
                                      .replace("{ID}", String.valueOf(idUser));

        try (final CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpDelete httpDelete = new HttpDelete(actualUrl);
            httpDelete.addHeader(LOGIN, PASS);
            client.execute(httpDelete);
        } catch (final IOException e) {
            log.debug("APIGitlabRequest exception={}", e.toString());
        }
    }

}
