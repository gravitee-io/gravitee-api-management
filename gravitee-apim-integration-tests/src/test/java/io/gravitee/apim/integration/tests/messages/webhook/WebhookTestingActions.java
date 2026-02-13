/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.integration.tests.messages.webhook;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToIgnoreCase;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThan;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.reactivex.rxjava3.core.Observable.interval;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.graviteesource.entrypoint.webhook.configuration.HttpHeader;
import com.graviteesource.entrypoint.webhook.configuration.RetryStrategy;
import com.graviteesource.entrypoint.webhook.configuration.SecurityType;
import com.graviteesource.entrypoint.webhook.configuration.WebhookEntrypointConnectorSubscriptionConfiguration;
import com.graviteesource.entrypoint.webhook.configuration.WebhookSubscriptionAuthConfiguration;
import io.gravitee.apim.integration.tests.fake.MessageFlowReadyPolicy;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionConfiguration;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Useful methods to use for webhook related tests
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WebhookTestingActions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WireMockServer wiremock;
    private final SubscriptionDispatcher subscriptionDispatcher;

    public WebhookTestingActions(WireMockServer wireMock, SubscriptionDispatcher subscriptionDispatcher) {
        this.wiremock = wireMock;
        this.subscriptionDispatcher = subscriptionDispatcher;
    }

    public Subscription createSubscription(String apiId, String callbackPath) throws JsonProcessingException {
        return this.createSubscription(apiId, callbackPath, new ArrayList<>());
    }

    public Subscription createSubscription(String apiId, String callbackPath, List<Completable> readyObs) throws JsonProcessingException {
        return this.createSubscription(apiId, callbackPath, null, readyObs);
    }

    public Subscription createSubscription(String apiId, String callbackPath, List<HttpHeader> headers, List<Completable> readyObs)
        throws JsonProcessingException {
        final Subscription subscription = this.configureSubscriptionAndCallback(apiId, callbackPath, headers);
        readyObs.add(MessageFlowReadyPolicy.readyObs(subscription));

        return subscription;
    }

    /**
     * Dispatches a subscription
     * @param subscription to dispatch
     * @return a completable when subscription is dispatched
     */
    public Completable dispatchSubscription(Subscription subscription) {
        return subscriptionDispatcher.dispatch(subscription);
    }

    /**
     * Waits for the `callback` configured on wiremock to receive `requestCounts` requests
     * @param requestCounts is the number of requests expected on the callback
     * @param callback is the callback url
     */
    public void waitForRequestsOnCallbackBlocking(int requestCounts, String callback) {
        waitForRequestsOnCallback(requestCounts, callback).test().awaitDone(10, SECONDS).assertComplete();
    }

    public Completable waitForRequestsOnCallback(int requestCounts, String callback) {
        return interval(50, MILLISECONDS, Schedulers.newThread())
            .takeWhile(i -> wiremock.countRequestsMatching(anyRequestedFor(urlPathEqualTo(callback)).build()).getCount() < requestCounts)
            .ignoreElements();
    }

    public Completable waitForRequestsOnCallback(int requestCounts, String callback, Runnable action) {
        AtomicInteger previousCount = new AtomicInteger(0);
        return interval(50, MILLISECONDS, Schedulers.newThread())
            .takeWhile(i -> {
                final int currentCount = wiremock.countRequestsMatching(anyRequestedFor(urlPathEqualTo(callback)).build()).getCount();

                for (int c = previousCount.get(); c < currentCount; c++) {
                    action.run();
                }
                previousCount.set(currentCount);
                return currentCount < requestCounts;
            })
            .ignoreElements();
    }

    /**
     * Waits for the `callback` configured on wiremock to receive `requestCounts` requests
     * @param requestCounts is the number of requests expected on the callback
     * @param callback is the callback url
     * @param dispatchSubscription is the Disposable returned by the dispatchSubscription call. When we received all the expected messages, we dispose it.
     */
    public void waitForRequestsOnCallbackBlocking(int requestCounts, String callback, Disposable dispatchSubscription) {
        interval(50, MILLISECONDS, Schedulers.newThread())
            .takeWhile(i -> wiremock.countRequestsMatching(anyRequestedFor(urlPathEqualTo(callback)).build()).getCount() < requestCounts)
            .doOnComplete(dispatchSubscription::dispose)
            .test()
            .awaitDone(10, SECONDS)
            .assertComplete();
    }

    /**
     * Concatenation of {@link this#dispatchSubscription(Subscription)} and {@link this#waitForRequestsOnCallbackBlocking(int, String, Disposable)}
     * It will dispatch the subscription, wait for the good number of requests on the defined callback and dispose the dispatchSubscription.
     * @param subscription the subscription to dispatch
     * @param requestCounts is the number of requests expected on the callback
     * @param callback is the callback url
     */
    public void dispatchSubscriptionAndWaitForRequestsOnCallback(Subscription subscription, int requestCounts, String callback) {
        final Disposable dispatch = dispatchSubscription(subscription).subscribe();
        waitForRequestsOnCallbackBlocking(requestCounts, callback, dispatch);
    }

    /**
     * Closes the subscription
     * @param subscription to close
     */
    public void closeSubscription(Subscription subscription) {
        subscription.setStatus("CLOSED");
        dispatchSubscription(subscription).blockingAwait();
    }

    /**
     * Configures a {@link Subscription} object to be deployed and the wiremock target accordingly.
     * 🔓 Default authentication chosen for subscription configuration and wiremock is Basic. Everything is already configured
     * @param apiId is the id of the api to configure in the subscription. ⚠️ It must match the id used in the api you want to test.
     * @param callbackPath is the path of callback to reach.
     * @return the {@link Subscription} object.
     * @throws JsonProcessingException
     */
    public Subscription configureSubscriptionAndCallback(String apiId, String callbackPath) throws JsonProcessingException {
        return configureSubscriptionAndCallback(apiId, callbackPath, null);
    }

    /**
     * Configures a {@link Subscription} object to be deployed and the wiremock target accordingly.
     * @param apiId is the id of the api to configure in the subscription. ⚠️ It must match the id used in the api you want to test.
     * @param callbackPath is the path of callback to reach.
     * @param headers are the additional headers to send to callback
     * @return the {@link Subscription} object.
     * @throws JsonProcessingException
     */
    public Subscription configureSubscriptionAndCallback(String apiId, String callbackPath, List<HttpHeader> headers)
        throws JsonProcessingException {
        WebhookEntrypointConnectorSubscriptionConfiguration configuration = new WebhookEntrypointConnectorSubscriptionConfiguration();
        configuration.setCallbackUrl(String.format("http://localhost:%s%s", wiremock.port(), callbackPath));
        configuration.setHeaders(headers);
        configuration.getRetry().setRetryStrategy(RetryStrategy.EXPONENTIAL);
        configuration.getRetry().setRetryOption("Retry On Fail");
        configuration.getRetry().setInitialDelaySeconds(3L);
        configuration.getRetry().setMaxDelaySeconds(3L);
        wiremock.stubFor(post(callbackPath).willReturn(ok("callback body")));

        return buildTestSubscription(apiId, configuration);
    }

    void applyBasicAuth(Subscription subscription, String username, String password) throws JsonProcessingException {
        final SubscriptionConfiguration subscriptionConfiguration = subscription.getConfiguration();

        final WebhookEntrypointConnectorSubscriptionConfiguration conf = MAPPER.readValue(
            subscriptionConfiguration.getEntrypointConfiguration(),
            WebhookEntrypointConnectorSubscriptionConfiguration.class
        );
        conf.setAuth(
            WebhookSubscriptionAuthConfiguration.builder()
                .type(SecurityType.BASIC)
                .basic(WebhookSubscriptionAuthConfiguration.Basic.builder().username(username).password(password).build())
                .build()
        );

        subscriptionConfiguration.setEntrypointConfiguration(MAPPER.writeValueAsString(conf));

        wiremock.resetAll();
        wiremock.stubFor(
            post(conf.getCallbackUrl().replace("http://localhost:" + wiremock.port(), ""))
                .withBasicAuth(username, password)
                .willReturn(ok())
        );
    }

    void applyTokenAuth(Subscription subscription, String token) throws JsonProcessingException {
        final SubscriptionConfiguration subscriptionConfiguration = subscription.getConfiguration();

        final WebhookEntrypointConnectorSubscriptionConfiguration conf = MAPPER.readValue(
            subscriptionConfiguration.getEntrypointConfiguration(),
            WebhookEntrypointConnectorSubscriptionConfiguration.class
        );
        conf.setAuth(
            WebhookSubscriptionAuthConfiguration.builder()
                .type(SecurityType.TOKEN)
                .token(WebhookSubscriptionAuthConfiguration.Token.builder().value(token).build())
                .build()
        );

        subscriptionConfiguration.setEntrypointConfiguration(MAPPER.writeValueAsString(conf));

        wiremock.resetAll();
        wiremock.stubFor(
            post(conf.getCallbackUrl().replace("http://localhost:" + wiremock.port(), ""))
                .withHeader("Authorization", equalToIgnoreCase("Bearer " + token))
                .willReturn(ok())
        );
    }

    void applyOauth2Auth(Subscription subscription, String clientId, String clientSecret) throws JsonProcessingException {
        final SubscriptionConfiguration subscriptionConfiguration = subscription.getConfiguration();

        final WebhookEntrypointConnectorSubscriptionConfiguration conf = MAPPER.readValue(
            subscriptionConfiguration.getEntrypointConfiguration(),
            WebhookEntrypointConnectorSubscriptionConfiguration.class
        );
        conf.setAuth(
            WebhookSubscriptionAuthConfiguration.builder()
                .type(SecurityType.OAUTH2)
                .oauth2(
                    WebhookSubscriptionAuthConfiguration.Oauth2.builder()
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .endpoint("http://localhost:" + wiremock.port() + "/oauth2endpoint")
                        .build()
                )
                .build()
        );

        subscriptionConfiguration.setEntrypointConfiguration(MAPPER.writeValueAsString(conf));

        wiremock.resetAll();
        wiremock.stubFor(
            post(conf.getCallbackUrl().replace("http://localhost:" + wiremock.port(), ""))
                .withHeader("Authorization", equalToIgnoreCase("Bearer token-from-oauth2-server"))
                .willReturn(ok())
        );

        wiremock.stubFor(
            post("/oauth2endpoint")
                .withRequestBody(equalTo("grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret))
                .willReturn(ok("{\"token_type\":\"Bearer\",\"access_token\":\"token-from-oauth2-server\"}"))
        );
    }

    String jwtTokenEndpoint() {
        return "http://localhost:" + wiremock.port() + "/jwt-token-endpoint";
    }

    void applyJwtProfileOauth2Auth(Subscription subscription, String issuer, String subject, String hmacKey)
        throws JsonProcessingException {
        applyJwtProfileOauth2Auth(
            subscription,
            WebhookSubscriptionAuthConfiguration.JwtProfile.builder()
                .issuer(issuer)
                .subject(subject)
                .audience(jwtTokenEndpoint())
                .signatureAlgorithm(WebhookSubscriptionAuthConfiguration.SignatureAlgorithm.HMAC_HS256)
                .keyContent(hmacKey)
                .build()
        );
    }

    void applyJwtProfileOauth2Auth(Subscription subscription, WebhookSubscriptionAuthConfiguration.JwtProfile jwtProfile)
        throws JsonProcessingException {
        final SubscriptionConfiguration subscriptionConfiguration = subscription.getConfiguration();

        final WebhookEntrypointConnectorSubscriptionConfiguration conf = MAPPER.readValue(
            subscriptionConfiguration.getEntrypointConfiguration(),
            WebhookEntrypointConnectorSubscriptionConfiguration.class
        );

        conf.setAuth(
            WebhookSubscriptionAuthConfiguration.builder().type(SecurityType.JWT_PROFILE_OAUTH2).jwtProfileOauth2(jwtProfile).build()
        );

        subscriptionConfiguration.setEntrypointConfiguration(MAPPER.writeValueAsString(conf));

        wiremock.resetAll();
        wiremock.stubFor(
            post(conf.getCallbackUrl().replace("http://localhost:" + wiremock.port(), ""))
                .withHeader("Authorization", equalToIgnoreCase("Bearer token-from-jwt-profile-server"))
                .willReturn(ok())
        );

        wiremock.stubFor(
            post("/jwt-token-endpoint")
                .withRequestBody(containing("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer"))
                .willReturn(ok("{\"token_type\":\"Bearer\",\"access_token\":\"token-from-jwt-profile-server\"}"))
        );
    }

    private Subscription buildTestSubscription(String apiId, WebhookEntrypointConnectorSubscriptionConfiguration configuration)
        throws JsonProcessingException {
        Subscription subscription = new Subscription();
        subscription.setApi(apiId);
        subscription.setId(UUID.randomUUID().toString());
        subscription.setApplication(UUID.randomUUID().toString());
        subscription.setStatus("ACCEPTED");
        subscription.setType(Subscription.Type.PUSH);
        SubscriptionConfiguration subscriptionConfiguration = new SubscriptionConfiguration(
            null,
            "webhook",
            MAPPER.writeValueAsString(configuration)
        );
        subscription.setConfiguration(subscriptionConfiguration);
        subscription.setPlan(UUID.randomUUID().toString());
        return subscription;
    }

    public void verifyMessages(int messageCount, String callbackPath) {
        for (int i = 0; i < messageCount; i++) {
            wiremock.verify(1, postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo("message-" + i)));
        }
    }

    public void verifyMessages(int messageCount, String callbackPath, String message) {
        wiremock.verify(messageCount, postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo(message)));
    }

    public void verifyMessagesAtLeast(int messageCount, String callbackPath, String message) {
        wiremock.verify(moreThan(1), postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo(message)));
    }

    public void verifyMessagesWithHeaders(int messageCount, String callbackPath, String message, List<HttpHeader> headers) {
        final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(
            equalTo(message)
        );

        for (HttpHeader header : headers) {
            requestPatternBuilder.withHeader(header.getName(), equalTo(header.getValue()));
        }

        wiremock.verify(messageCount, requestPatternBuilder);
    }

    public void verifyMessagesWithHeaders(int messageCount, String callbackPath, List<HttpHeader> headers) {
        for (int i = 0; i < messageCount; i++) {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(
                equalTo("message-" + i)
            );

            for (HttpHeader header : headers) {
                requestPatternBuilder.withHeader(header.getName(), equalTo(header.getValue()));
            }

            wiremock.verify(1, requestPatternBuilder);
        }
    }
}
