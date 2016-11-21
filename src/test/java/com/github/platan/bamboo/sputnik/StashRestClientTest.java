package com.github.platan.bamboo.sputnik;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Optional;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.BDDCatchException.when;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.Assert.assertEquals;

public class StashRestClientTest {

    @Rule
    public WireMockRule stashRestStub = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort()
            .keystorePath(this.getClass().getResource("/ssl/keystore.jks").getPath())
            .keystorePassword("changeit"));

    static {
        System.setProperty("javax.net.ssl.trustStore",
                StashRestClientTest.class.getResource("/ssl/cacerts.jks").getPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
    }

    @Test
    public void returnOnlyPullRequestId() {
        // given
        stubFor(get(urlEqualTo("/rest/api/1.0/projects/PROJECT_1/repos/rep1/pull-requests"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Authorization", equalTo("Basic dXNlcjE6cGFzc3dvcmQx"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("pull-requests-with-one-pr.json")));

        // when
        Optional<Long> pullRequestId = new UnirestStashRestClient("https://localhost:" + stashRestStub.httpsPort(), "user1",
                "password1")
                .getPullRequestId("PROJECT_1", "rep1", "basic_branching");

        // then
        assertEquals(Optional.of(1L), pullRequestId);
    }

    @Test
    public void returnEmptyPullRequestId() {
        // given
        stubFor(get(urlEqualTo("/rest/api/1.0/projects/PROJECT_1/repos/rep1/pull-requests"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Authorization", equalTo("Basic dXNlcjE6cGFzc3dvcmQx"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("pull-requests-with-one-pr.json")));

        // when
        Optional<Long> pullRequestId = new UnirestStashRestClient("https://localhost:" + stashRestStub.httpsPort(), "user1",
                "password1")
                .getPullRequestId("PROJECT_1", "rep1", "feature1");

        // then
        assertEquals(Optional.absent(), pullRequestId);
    }

    @Test
    public void returnEmptyPullRequestIdWhenThereIsNoPr() {
        // given
        stubFor(get(urlEqualTo("/rest/api/1.0/projects/PROJECT_1/repos/rep1/pull-requests"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Authorization", equalTo("Basic dXNlcjE6cGFzc3dvcmQx"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("no-pull-requests.json")));

        // when
        Optional<Long> pullRequestId = new UnirestStashRestClient("https://localhost:" + stashRestStub.httpsPort(), "user1",
                "password1")
                .getPullRequestId("PROJECT_1", "rep1", "feature1");

        // then
        assertEquals(Optional.absent(), pullRequestId);
    }

    @Test
    public void throwExceptionFor404() {
        // given
        stubFor(get(urlEqualTo("/rest/api/1.0/projects/PROJECT_1/repos/rep1/pull-requests"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Authorization", equalTo("Basic dXNlcjE6cGFzc3dvcmQx"))
                .willReturn(aResponse().withStatus(404)));

        // when
        when(new UnirestStashRestClient("https://localhost:" + stashRestStub.httpsPort(), "user1", "password1"))
                .getPullRequestId("PROJECT_1", "rep1", "feature1");

        then(caughtException())
                .isInstanceOf(StashException.class);
    }

    @Test
    public void throwExceptionWhenCannotConnect() {
        when(new UnirestStashRestClient("https://localhost:10000", "user1", "password1")).getPullRequestId("PROJECT_1",
                "rep1", "feature1");

        then(caughtException())
                .isInstanceOf(StashException.class)
                .hasCauseInstanceOf(Throwable.class);
    }
}
