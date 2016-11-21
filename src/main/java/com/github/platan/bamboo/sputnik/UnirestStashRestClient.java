package com.github.platan.bamboo.sputnik;

import com.google.common.base.Optional;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.google.common.net.HttpHeaders.ACCEPT;

public class UnirestStashRestClient implements StashRestClient {

    private final String stashBaseUrl;
    private final String username;
    private final String password;

    public UnirestStashRestClient(String stashBaseUrl, String username, String password) {
        this.stashBaseUrl = stashBaseUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public Optional<Long> getPullRequestId(String projectKey, String repositorySlug, String branchName) {
        JsonNode body = fetchJson(projectKey, repositorySlug);
        return findPullRequestId(branchName, body);
    }

    private JsonNode fetchJson(String projectKey, String repositorySlug) {
        HttpResponse<JsonNode> jsonNodeHttpResponse;
        String url = String.format("%s/rest/api/1.0/projects/%s/repos/%s/pull-requests", stashBaseUrl,
                projectKey, repositorySlug);
        try {
            jsonNodeHttpResponse = Unirest.get(url).header(ACCEPT, "application/json")
                    .basicAuth(username, password).asJson();
        } catch (UnirestException e) {
            throw new StashException(e);
        }
        if (jsonNodeHttpResponse.getStatus() != 200) {
            throw new StashException(String.format("Stash REST API returned status code %d for %s", jsonNodeHttpResponse.getStatus(), url));
        }
        return jsonNodeHttpResponse.getBody();
    }

    private Optional<Long> findPullRequestId(String branchName, JsonNode body) {
        JSONArray values = body.getObject().getJSONArray("values");
        for (int i = 0; i < values.length(); i++) {
            JSONObject pullRequest = values.getJSONObject(i);
            if (pullRequest.getJSONObject("fromRef").getString("displayId").equals(branchName)) {
                return Optional.of(pullRequest.getLong("id"));
            }
        }
        return Optional.absent();
    }

}
