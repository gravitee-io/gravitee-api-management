/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.integration.tests.messages.webhook;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.reactivex.rxjava3.core.Observable.interval;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.graviteesource.entrypoint.webhook.configuration.HttpHeader;
import com.graviteesource.entrypoint.webhook.configuration.SecurityType;
import com.graviteesource.entrypoint.webhook.configuration.WebhookEntrypointConnectorSubscriptionConfiguration;
import com.graviteesource.entrypoint.webhook.configuration.WebhookSubscriptionAuthConfiguration;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionConfiguration;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.List;
import java.util.UUID;

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
        return interval(50, MILLISECONDS)
            .takeWhile(i -> wiremock.countRequestsMatching(anyRequestedFor(urlPathEqualTo(callback)).build()).getCount() < requestCounts)
            .ignoreElements();
    }

    /**
     * Waits for the `callback` configured on wiremock to receive `requestCounts` requests
     * @param requestCounts is the number of requests expected on the callback
     * @param callback is the callback url
     * @param dispatchSubscription is the Disposable returned by the dispatchSubscription call. When we received all the expected messages, we dispose it.
     */
    public void waitForRequestsOnCallbackBlocking(int requestCounts, String callback, Disposable dispatchSubscription) {
        interval(50, MILLISECONDS)
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
        return configureSubscriptionAndCallback(apiId, callbackPath, null, null);
    }

    /**
     * Configures a {@link Subscription} object to be deployed and the wiremock target accordingly.
     * @param apiId is the id of the api to configure in the subscription. ⚠️ It must match the id used in the api you want to test.
     * @param callbackPath is the path of callback to reach.
     * @param authConfiguration is the authentication configuration of the webhook subscription
     * @param headers are the additional headers to send to callback
     * @return the {@link Subscription} object.
     * @throws JsonProcessingException
     */
    public Subscription configureSubscriptionAndCallback(
        String apiId,
        String callbackPath,
        WebhookSubscriptionAuthConfiguration authConfiguration,
        List<HttpHeader> headers
    ) throws JsonProcessingException {
        WebhookEntrypointConnectorSubscriptionConfiguration configuration = new WebhookEntrypointConnectorSubscriptionConfiguration();
        configuration.setCallbackUrl(String.format("http://localhost:%s%s", wiremock.port(), callbackPath));
        final WebhookSubscriptionAuthConfiguration auth = prepareWebhookAuth(authConfiguration);
        configuration.setAuth(auth);
        configuration.setHeaders(headers);
        wiremock.stubFor(
            post(callbackPath).withBasicAuth(auth.getBasic().getUsername(), auth.getBasic().getPassword()).willReturn(ok("callback body"))
        );

        return buildTestSubscription(apiId, configuration);
    }

    private WebhookSubscriptionAuthConfiguration prepareWebhookAuth(WebhookSubscriptionAuthConfiguration authConfiguration) {
        return authConfiguration == null
            ? new WebhookSubscriptionAuthConfiguration( // TODO: currently we have to default to basic auth, but would be better with no auth
                SecurityType.BASIC,
                WebhookSubscriptionAuthConfiguration.Basic.builder().username("toto").password("password").build(),
                null,
                null
            )
            : authConfiguration;
    }

    private Subscription buildTestSubscription(String apiId, WebhookEntrypointConnectorSubscriptionConfiguration configuration)
        throws JsonProcessingException {
        Subscription subscription = new Subscription();
        subscription.setApi(apiId);
        subscription.setId(UUID.randomUUID().toString());
        subscription.setStatus("ACCEPTED");
        subscription.setType(Subscription.Type.PUSH);
        SubscriptionConfiguration subscriptionConfiguration = new SubscriptionConfiguration(
            "subscribe",
            "webhook",
            MAPPER.writeValueAsString(configuration)
        );
        subscription.setConfiguration(subscriptionConfiguration);
        return subscription;
    }
}
