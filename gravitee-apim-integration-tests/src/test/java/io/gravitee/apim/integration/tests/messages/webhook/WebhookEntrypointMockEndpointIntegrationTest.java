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

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToIgnoreCase;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.graviteesource.entrypoint.webhook.auth.AccessTokenProvider.ACCESS_TOKEN_KEY;
import static com.graviteesource.entrypoint.webhook.auth.AccessTokenProvider.EXPIRES_IN_KEY;
import static com.graviteesource.entrypoint.webhook.auth.AccessTokenProvider.TOKEN_TYPE_KEY;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.graviteesource.entrypoint.webhook.WebhookEntrypointConnectorFactory;
import com.graviteesource.entrypoint.webhook.configuration.HttpHeader;
import com.graviteesource.entrypoint.webhook.exception.UnauthorizedException;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.InjectApi;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.MessageStorage;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.PersistentMockEndpointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.integration.tests.fake.MessageFlowReadyPolicy;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.definition.model.v4.listener.entrypoint.Dlq;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.api.exception.MessageProcessingException;
import io.gravitee.gateway.reactive.core.connection.ConnectionDrainManager;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
class WebhookEntrypointMockEndpointIntegrationTest extends AbstractGatewayTest {

    private static final String API_ID = "webhook-entrypoint-mock-endpoint";
    private static final String WEBHOOK_URL_PATH = "/webhook";
    private WebhookTestingActions webhookActions;
    private MessageStorage messageStorage;

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("webhook", EntrypointBuilder.build("webhook", WebhookEntrypointConnectorFactory.class));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("message-flow-ready", PolicyBuilder.build("message-flow-ready", MessageFlowReadyPolicy.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        super.configureEndpoints(endpoints);
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", PersistentMockEndpointConnectorFactory.class));
    }

    @BeforeEach
    void setUp() {
        webhookActions = new WebhookTestingActions(wiremock, getBean(SubscriptionDispatcher.class));
        messageStorage = getBean(MessageStorage.class);
    }

    @AfterEach
    void tearDown() {
        messageStorage.reset();
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages() throws JsonProcessingException {
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);

        webhookActions
            .dispatchSubscription(subscription)
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test()
            .awaitDone(10, SECONDS)
            .assertComplete();

        webhookActions.verifyMessages(messageCount, callbackPath, "message");
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_with_headers() throws JsonProcessingException {
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/with-headers";
        final List<HttpHeader> headers = List.of(
            new HttpHeader("Header1", "my-header-1-value"),
            new HttpHeader("Header2", "my-header-2-value")
        );

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath, headers, new ArrayList<>());

        webhookActions
            .dispatchSubscription(subscription)
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test()
            .awaitDone(10, SECONDS)
            .assertComplete();

        webhookActions.verifyMessagesWithHeaders(messageCount, callbackPath, "message", headers);
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_retry_on_5xx_and_interrupt_when_max_retries_is_reached(
        @InjectApi(apiId = "webhook-entrypoint-mock-endpoint") ReactableApi<?> api
    ) throws JsonProcessingException {
        try {
            configureWebhookConcurrency(api, 1);
            configureMockEndpoint(api, 0, 1);
            deploy(api);

            // 1 first attempt + 3 retries
            final int messageCount = (1 + 3);
            final String callbackPath = WEBHOOK_URL_PATH + "/test";
            final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);

            TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

            wiremock.resetAll();
            wiremock.stubFor(post(callbackPath).willReturn(serverError()));

            webhookActions
                .dispatchSubscription(subscription)
                .mergeWith(Completable.fromRunnable(testScheduler::triggerActions))
                .takeUntil(
                    webhookActions.waitForRequestsOnCallback(messageCount, callbackPath, () -> {
                        // 1000 for the webhook
                        testScheduler.advanceTimeBy(3000, MILLISECONDS);
                        testScheduler.triggerActions();
                    })
                )
                .test()
                .awaitDone(10, SECONDS)
                .assertError(InterruptionFailureException.class);

            // Mock endpoint produces only 1 message. 1 attempt + 5 retries. We should expect no more than 6 calls
            webhookActions.verifyMessages(messageCount, callbackPath, "message");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_retry_in_case_of_5xx_and_continue_normally(@InjectApi(apiId = "webhook-entrypoint-mock-endpoint") ReactableApi<?> api)
        throws JsonProcessingException {
        try {
            // Note that this test forces webhook concurrency to 1 because of wiremock scenario not supporting concurrency (see https://github.com/wiremock/wiremock/issues?q=is%3Aissue+is%3Aopen+scenario#issuecomment-1348029799).
            configureWebhookConcurrency(api, 1);
            configureMockEndpoint(api, 0, 1);
            deploy(api);

            final int messageCount = 3;
            final String callbackPath = WEBHOOK_URL_PATH + "/test";
            final ArrayList<Completable> readyObs = new ArrayList<>();
            final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath, readyObs);

            TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

            wiremock.resetAll();
            // Two 500 error then a 200 ok.
            wiremock.stubFor(
                post(callbackPath)
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willReturn(serverError())
                    .willSetStateTo("Second attempt")
            );
            wiremock.stubFor(
                post(callbackPath)
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs("Second attempt")
                    .willReturn(serverError())
                    .willSetStateTo("Third attempt")
            );
            wiremock.stubFor(post(callbackPath).inScenario("Retry Scenario").whenScenarioStateIs("Third attempt").willReturn(ok()));

            webhookActions
                .dispatchSubscription(subscription)
                .mergeWith(Completable.merge(readyObs).andThen(Completable.fromRunnable(testScheduler::triggerActions)))
                .takeUntil(
                    webhookActions.waitForRequestsOnCallback(messageCount, callbackPath, () -> {
                        testScheduler.advanceTimeBy(3000, MILLISECONDS);
                        testScheduler.triggerActions();
                    })
                )
                .test()
                .awaitDone(10, SECONDS)
                .assertNoErrors()
                .assertComplete();

            // Mock endpoint produces only 1 message. 1 attempt (500) + 1 retry (500) then a 200. We should expect no more than 3 calls
            webhookActions.verifyMessages(messageCount, callbackPath, "message");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_stop_on_4xx(@InjectApi(apiId = "webhook-entrypoint-mock-endpoint") ReactableApi<?> api) throws JsonProcessingException {
        final int messageCount = 3;

        // Note that this test forces webhook concurrency to 1 because of wiremock scenario not supporting concurrency (see https://github.com/wiremock/wiremock/issues?q=is%3Aissue+is%3Aopen+scenario#issuecomment-1348029799).
        configureWebhookConcurrency(api, 1);
        configureMockEndpoint(api, 10, messageCount);
        deploy(api);
        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);

        wiremock.resetAll();
        // Two 200 ok then a 400 bad request.
        wiremock.stubFor(
            post(callbackPath)
                .inScenario("4xx Scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(ok())
                .willSetStateTo("Second call")
        );
        wiremock.stubFor(
            post(callbackPath).inScenario("4xx Scenario").whenScenarioStateIs("Second call").willReturn(ok()).willSetStateTo("Third call")
        );
        wiremock.stubFor(
            post(callbackPath).inScenario("4xx Scenario").whenScenarioStateIs("Third call").willReturn(badRequest()).willSetStateTo("End")
        );

        webhookActions.dispatchSubscription(subscription).test().awaitDone(10, SECONDS).assertError(MessageProcessingException.class);

        webhookActions.verifyMessages(messageCount, callbackPath, "message");
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_basic_auth() throws JsonProcessingException {
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);
        webhookActions.applyBasicAuth(subscription, "foo", "bar");

        webhookActions
            .dispatchSubscription(subscription)
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test()
            .awaitDone(10, SECONDS)
            .assertComplete();

        webhookActions.verifyMessagesWithHeaders(
            messageCount,
            callbackPath,
            "message",
            List.of(new HttpHeader("Authorization", "Basic Zm9vOmJhcg=="))
        );
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_basic_auth_failure() throws JsonProcessingException {
        final String callbackPath = WEBHOOK_URL_PATH + "/auth-failure";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);
        webhookActions.applyBasicAuth(subscription, "foo", "bar");

        // Override the mock response to return an 401 error.
        wiremock.resetAll();
        wiremock.stubFor(post(callbackPath).withBasicAuth("foo", "bar").willReturn(unauthorized()));

        webhookActions.dispatchSubscription(subscription).test().awaitDone(10, SECONDS).assertError(UnauthorizedException.class);
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_token_auth() throws JsonProcessingException {
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);
        webhookActions.applyTokenAuth(subscription, "token");

        webhookActions
            .dispatchSubscription(subscription)
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test()
            .awaitDone(10, SECONDS)
            .assertComplete();

        webhookActions.verifyMessagesWithHeaders(
            messageCount,
            callbackPath,
            "message",
            List.of(new HttpHeader("Authorization", "Bearer token"))
        );
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_token_auth_failure() throws JsonProcessingException {
        final String callbackPath = WEBHOOK_URL_PATH + "/auth-failure";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);
        webhookActions.applyTokenAuth(subscription, "token");

        // Override the mock response to return an 401 error.
        wiremock.resetAll();
        wiremock.stubFor(post(callbackPath).withHeader("Authorization", equalToIgnoreCase("Bearer token")).willReturn(unauthorized()));

        webhookActions.dispatchSubscription(subscription).test().awaitDone(10, SECONDS).assertError(UnauthorizedException.class);
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_oauth2_auth() throws JsonProcessingException {
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);
        webhookActions.applyOauth2Auth(subscription, "my-client-id", "my-client-secret");

        webhookActions
            .dispatchSubscription(subscription)
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test()
            .awaitDone(10, SECONDS)
            .assertComplete();

        // Make sure the oauth server has been called.
        wiremock.verify(moreThanOrExactly(1), postRequestedFor(urlPathEqualTo("/oauth2endpoint")));

        // And the messages received.
        webhookActions.verifyMessagesWithHeaders(
            messageCount,
            callbackPath,
            "message",
            List.of(new HttpHeader("Authorization", "Bearer token-from-oauth2-server"))
        );
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_oauth2_auth_failure() throws JsonProcessingException {
        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);
        webhookActions.applyOauth2Auth(subscription, "my-client-id", "my-client-secret");

        // Override the mock response to return a 401 error.
        wiremock.resetAll();
        wiremock.stubFor(
            post("/oauth2endpoint").willReturn(
                unauthorized()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"invalid_client\",\"error_description\":\"Client authentication failed\"}")
            )
        );

        webhookActions.dispatchSubscription(subscription).test().awaitDone(10, SECONDS).assertError(MessageProcessingException.class);

        // Make sure the oauth server has been called.
        wiremock.verify(moreThanOrExactly(1), postRequestedFor(urlPathEqualTo("/oauth2endpoint")));

        // Make sure webhook has not been called because no oauth token has been generated.
        wiremock.verify(0, postRequestedFor(urlPathEqualTo(callbackPath)));
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_oauth2_auth_when_token_expired_and_is_renewed(
        @InjectApi(apiId = "webhook-entrypoint-mock-endpoint") ReactableApi<?> api
    ) throws JsonProcessingException {
        final int messageCount = 3;

        // Note that this test forces webhook concurrency to 1 because of wiremock scenario not supporting concurrency (see https://github.com/wiremock/wiremock/issues?q=is%3Aissue+is%3Aopen+scenario#issuecomment-1348029799).
        configureWebhookConcurrency(api, 1);
        // Use a message interval longer than the token expiration (1 second) to ensure the token expires between messages.
        configureMockEndpoint(api, 2000, messageCount);
        deploy(api);

        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);
        webhookActions.applyOauth2Auth(subscription, "my-client-id", "my-client-secret");

        // Override the mock response to override oauth2 response.
        wiremock.resetAll();

        wiremock.stubFor(post(callbackPath).willReturn(ok()));

        // First token expires in 1 second.
        wiremock.stubFor(
            post("/oauth2endpoint")
                .inScenario("Oauth Scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(
                    ok(
                        "{\"" +
                            TOKEN_TYPE_KEY +
                            "\":\"Bearer\",\"" +
                            ACCESS_TOKEN_KEY +
                            "\":\"token-from-oauth2-server-first\", \"" +
                            EXPIRES_IN_KEY +
                            "\": 1}"
                    )
                )
                .willSetStateTo("Second call")
        );

        // Second token never expires.
        wiremock.stubFor(
            post("/oauth2endpoint")
                .inScenario("Oauth Scenario")
                .whenScenarioStateIs("Second call")
                .willReturn(ok("{\"" + TOKEN_TYPE_KEY + "\":\"Bearer\",\"" + ACCESS_TOKEN_KEY + "\":\"token-from-oauth2-server-second\"}"))
        );

        webhookActions.dispatchSubscription(subscription).test().awaitDone(30, SECONDS).assertComplete();

        // Make sure the oauth server has been called twice (first token expiring after 1s, second never expires).
        wiremock.verify(2, postRequestedFor(urlPathEqualTo("/oauth2endpoint")));

        // Make sure webhook has been call once with the first token which then expires.
        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo(callbackPath)).withHeader(
                "Authorization",
                equalToIgnoreCase("Bearer token-from-oauth2-server-first")
            )
        );

        // Then other calls with the new second token.
        wiremock.verify(
            2,
            postRequestedFor(urlPathEqualTo(callbackPath)).withHeader(
                "Authorization",
                equalToIgnoreCase("Bearer token-from-oauth2-server-second")
            )
        );
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_jwt_profile_oauth2_auth() throws JsonProcessingException {
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);
        webhookActions.applyJwtProfileOauth2Auth(
            subscription,
            "test-issuer",
            "test-subject",
            "this-is-a-very-long-secret-key-for-hmac-hs256-test"
        );

        webhookActions
            .dispatchSubscription(subscription)
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test()
            .awaitDone(10, SECONDS)
            .assertComplete();

        // Make sure the token endpoint has been called with the JWT bearer grant type.
        wiremock.verify(moreThanOrExactly(1), postRequestedFor(urlPathEqualTo("/jwt-token-endpoint")));

        // And the messages received with the access token from the JWT profile token endpoint.
        webhookActions.verifyMessagesWithHeaders(
            messageCount,
            callbackPath,
            "message",
            List.of(new HttpHeader("Authorization", "Bearer token-from-jwt-profile-server"))
        );
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_jwt_profile_oauth2_auth_failure() throws JsonProcessingException {
        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);
        webhookActions.applyJwtProfileOauth2Auth(
            subscription,
            "test-issuer",
            "test-subject",
            "this-is-a-very-long-secret-key-for-hmac-hs256-test"
        );

        // Override the mock response to return a 401 error.
        wiremock.resetAll();
        wiremock.stubFor(
            post("/jwt-token-endpoint").willReturn(
                unauthorized()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"invalid_grant\",\"error_description\":\"JWT assertion is invalid\"}")
            )
        );

        webhookActions.dispatchSubscription(subscription).test().awaitDone(10, SECONDS).assertError(MessageProcessingException.class);

        // Make sure the token endpoint has been called.
        wiremock.verify(moreThanOrExactly(1), postRequestedFor(urlPathEqualTo("/jwt-token-endpoint")));

        // Make sure webhook has not been called because no token has been generated.
        wiremock.verify(0, postRequestedFor(urlPathEqualTo(callbackPath)));
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_receive_messages_jwt_profile_oauth2_auth_when_token_expired_and_is_renewed(
        @InjectApi(apiId = "webhook-entrypoint-mock-endpoint") ReactableApi<?> api
    ) throws JsonProcessingException {
        final int messageCount = 3;

        // Force concurrency to 1 because of wiremock scenario not supporting concurrency.
        configureWebhookConcurrency(api, 1);
        // Use a message interval longer than the token expiration (1 second) to ensure the token expires between messages.
        configureMockEndpoint(api, 2000, messageCount);
        deploy(api);

        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);
        webhookActions.applyJwtProfileOauth2Auth(
            subscription,
            "test-issuer",
            "test-subject",
            "this-is-a-very-long-secret-key-for-hmac-hs256-test"
        );

        // Override the mock response to override token endpoint response.
        wiremock.resetAll();

        wiremock.stubFor(post(callbackPath).willReturn(ok()));

        // First token expires in 1 second.
        wiremock.stubFor(
            post("/jwt-token-endpoint")
                .inScenario("JwtProfile Scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(
                    ok(
                        "{\"" +
                            TOKEN_TYPE_KEY +
                            "\":\"Bearer\",\"" +
                            ACCESS_TOKEN_KEY +
                            "\":\"token-from-jwt-profile-server-first\", \"" +
                            EXPIRES_IN_KEY +
                            "\": 1}"
                    )
                )
                .willSetStateTo("Second call")
        );

        // Second token never expires.
        wiremock.stubFor(
            post("/jwt-token-endpoint")
                .inScenario("JwtProfile Scenario")
                .whenScenarioStateIs("Second call")
                .willReturn(
                    ok("{\"" + TOKEN_TYPE_KEY + "\":\"Bearer\",\"" + ACCESS_TOKEN_KEY + "\":\"token-from-jwt-profile-server-second\"}")
                )
        );

        webhookActions.dispatchSubscription(subscription).test().awaitDone(30, SECONDS).assertComplete();

        // Make sure the token endpoint has been called twice (first token expiring after 1s, second never expires).
        wiremock.verify(2, postRequestedFor(urlPathEqualTo("/jwt-token-endpoint")));

        // Make sure webhook has been called once with the first token which then expires.
        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo(callbackPath)).withHeader(
                "Authorization",
                equalToIgnoreCase("Bearer token-from-jwt-profile-server-first")
            )
        );

        // Then other calls with the new second token.
        wiremock.verify(
            2,
            postRequestedFor(urlPathEqualTo(callbackPath)).withHeader(
                "Authorization",
                equalToIgnoreCase("Bearer token-from-jwt-profile-server-second")
            )
        );
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_ignore_when_connection_is_drained(@InjectApi(apiId = "webhook-entrypoint-mock-endpoint") ReactableApi<?> api)
        throws JsonProcessingException {
        try {
            final ConnectionDrainManager connectionDrainManager = applicationContext.getBean(ConnectionDrainManager.class);

            final TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

            // Note that this test forces webhook concurrency to 1 because of wiremock scenario not supporting concurrency (see https://github.com/wiremock/wiremock/issues?q=is%3Aissue+is%3Aopen+scenario#issuecomment-1348029799).
            configureWebhookConcurrency(api, 1);
            configureMockEndpoint(api, 10, Integer.MAX_VALUE);
            deploy(api);
            final String callbackPath = WEBHOOK_URL_PATH + "/test";

            final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);

            wiremock.resetAll();
            wiremock.stubFor(post(callbackPath).willReturn(ok()));

            final AtomicBoolean first = new AtomicBoolean(true);

            webhookActions
                .dispatchSubscription(subscription)
                .mergeWith(
                    Completable.fromRunnable(() -> {
                        // Triggers the first message.
                        testScheduler.advanceTimeBy(10, MILLISECONDS);
                        testScheduler.triggerActions();
                    })
                )
                .takeUntil(
                    webhookActions.waitForRequestsOnCallback(3, callbackPath, () -> {
                        // When a message is received by the webhook, request a drain (should have no effect)
                        connectionDrainManager.requestDrain();

                        if (first.compareAndSet(true, false)) {
                            // Triggers 2 more messages after the first one.
                            testScheduler.advanceTimeBy(20, MILLISECONDS);
                            testScheduler.triggerActions();
                        }
                    })
                )
                .test()
                .awaitDone(10, SECONDS)
                .assertComplete();

            // Should receive 3 messages, despite the connection drain which should be ignored by the webhook entrypoint.
            webhookActions.verifyMessages(3, callbackPath, "message");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-mock-endpoint.json" })
    void should_put_into_dlq_on_4xx(@InjectApi(apiId = "webhook-entrypoint-mock-endpoint") ReactableApi<?> api)
        throws JsonProcessingException {
        final int messageCount = 3;

        // Note that this test forces webhook concurrency to 1 because of wiremock scenario not supporting concurrency (see https://github.com/wiremock/wiremock/issues?q=is%3Aissue+is%3Aopen+scenario#issuecomment-1348029799).
        configureWebhookConcurrency(api, 1);
        configureMockEndpoint(api, 10, messageCount);
        configureDlq(api, "mock");
        deploy(api);
        final String callbackPath = WEBHOOK_URL_PATH + "/test";

        final Subscription subscription = webhookActions.createSubscription(API_ID, callbackPath);

        wiremock.resetAll();
        // Two 400 ok then a 200 bad request.
        wiremock.stubFor(
            post(callbackPath)
                .inScenario("4xx Scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(badRequest())
                .willSetStateTo("Second call")
        );
        wiremock.stubFor(
            post(callbackPath)
                .inScenario("4xx Scenario")
                .whenScenarioStateIs("Second call")
                .willReturn(badRequest())
                .willSetStateTo("Third call")
        );
        wiremock.stubFor(
            post(callbackPath).inScenario("4xx Scenario").whenScenarioStateIs("Third call").willReturn(ok()).willSetStateTo("End")
        );

        webhookActions.dispatchSubscription(subscription).test().awaitDone(10, SECONDS).assertComplete();

        webhookActions.verifyMessages(messageCount, callbackPath, "message");

        messageStorage.subject().test().assertValueCount(2);
    }

    private void configureWebhookConcurrency(ReactableApi<?> api, int concurrency) {
        ((Api) api).getDefinition()
            .getListeners()
            .get(0)
            .getEntrypoints()
            .get(0)
            .setConfiguration("{\"http\": { \"maxConcurrentConnections\": " + concurrency + " }}");
    }

    private void configureMockEndpoint(ReactableApi<?> api, int messageInterval, int messageCount) {
        ((Api) api).getDefinition()
            .getEndpointGroups()
            .get(0)
            .getEndpoints()
            .get(0)
            .setConfiguration(
                "{\"messageInterval\": " + messageInterval + ",\"messageContent\": \"message\", \"messageCount\": " + messageCount + "}"
            );
    }

    private void configureDlq(ReactableApi<?> api, String endpointId) {
        ((Api) api).getDefinition().getListeners().get(0).getEntrypoints().get(0).setDlq(new Dlq(endpointId));
    }
}
