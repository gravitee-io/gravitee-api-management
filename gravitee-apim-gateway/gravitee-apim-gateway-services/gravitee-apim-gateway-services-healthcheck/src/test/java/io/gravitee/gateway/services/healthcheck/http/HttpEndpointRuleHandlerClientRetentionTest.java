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
package io.gravitee.gateway.services.healthcheck.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.healthcheck.HealthCheckRequest;
import io.gravitee.definition.model.services.healthcheck.HealthCheckResponse;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.internal.CloseFuture;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.VertxInternal;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

/**
 * Guards the shared-client workaround of the v2 health check: the client of a closed rule handler must leave the close
 * future of the context that created it, once its connection is closed.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpEndpointRuleHandlerClientRetentionTest {

    private static final int REDEPLOYS = 20;

    private Vertx vertx;
    private HttpServer server;
    private Environment environment;
    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() throws Exception {
        vertx = Vertx.vertx();
        server = vertx
            .createHttpServer()
            .requestHandler(request -> request.response().end("ok"))
            .listen(0, "localhost")
            .await(10, TimeUnit.SECONDS);
        environment = mock(Environment.class);
        templateEngine = mock(TemplateEngine.class);
        when(environment.getProperty("http.ssl.openssl", Boolean.class, false)).thenReturn(false);
        when(templateEngine.getValue(anyString(), eq(String.class))).thenAnswer(invocation -> invocation.getArgument(0, String.class));
    }

    @AfterEach
    void tearDown() throws Exception {
        vertx.close().await(10, TimeUnit.SECONDS);
    }

    @Test
    void should_not_retain_the_clients_of_closed_handlers_off_a_vertx_context() throws Exception {
        // Deploying and undeploying an API runs on the sync deployer thread, which is not a Vert.x thread.
        CloseFuture root = ((VertxInternal) vertx).closeFuture();
        int before = retained(root);
        AtomicInteger checks = new AtomicInteger();

        for (int i = 0; i < REDEPLOYS; i++) {
            HttpEndpointRuleHandler<HttpEndpoint> handler = new HttpEndpointRuleHandler<>(
                vertx,
                endpointRule(),
                templateEngine,
                environment
            );
            runCheck(handler).await(10, TimeUnit.SECONDS);
            checks.incrementAndGet();
            handler.close();
        }

        assertThat(checks).hasValue(REDEPLOYS);
        assertThat(awaitRetained(root, before) - before).isZero();
    }

    @Test
    void should_not_retain_the_clients_of_closed_handlers_on_a_deployment_context() throws Exception {
        AtomicReference<CloseFuture> closeFuture = new AtomicReference<>();
        AtomicInteger before = new AtomicInteger();
        CompletableFuture<List<HttpEndpointRuleHandler<HttpEndpoint>>> handlers = new CompletableFuture<>();

        vertx
            .deployVerticle(
                new AbstractVerticle() {
                    @Override
                    public void start(Promise<Void> started) throws Exception {
                        closeFuture.set(((ContextInternal) context).closeFuture());
                        before.set(retained(closeFuture.get()));
                        List<HttpEndpointRuleHandler<HttpEndpoint>> created = new ArrayList<>();
                        List<Future<Void>> checks = new ArrayList<>();
                        for (int i = 0; i < REDEPLOYS; i++) {
                            HttpEndpointRuleHandler<HttpEndpoint> handler = new HttpEndpointRuleHandler<>(
                                vertx,
                                endpointRule(),
                                templateEngine,
                                environment
                            );
                            created.add(handler);
                            checks.add(runCheck(handler));
                        }
                        Future.all(checks)
                            .onSuccess(done -> handlers.complete(created))
                            .onFailure(handlers::completeExceptionally);
                        started.complete();
                    }
                }
            )
            .await(10, TimeUnit.SECONDS);

        // Closed from the test thread, as the deployer does, while the connections opened by the checks are still pooled.
        handlers.get(10, TimeUnit.SECONDS).forEach(HttpEndpointRuleHandler::close);

        assertThat(closeFuture.get()).isNotSameAs(((VertxInternal) vertx).closeFuture());
        assertThat(awaitRetained(closeFuture.get(), before.get()) - before.get()).isZero();
    }

    @Test
    void should_give_each_handler_its_own_shared_client() throws Exception {
        // A fixed name would make every handler share the client, hence the options, of the first one created.
        HttpEndpointRuleHandler<HttpEndpoint> first = new HttpEndpointRuleHandler<>(vertx, endpointRule(), templateEngine, environment);
        HttpEndpointRuleHandler<HttpEndpoint> second = new HttpEndpointRuleHandler<>(vertx, endpointRule(), templateEngine, environment);
        try {
            assertThat(sharedHttpClientNames())
                .hasSize(2)
                .allMatch(name -> name.startsWith("healthcheck-"));
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void should_still_leak_when_the_client_comes_straight_from_vertx() throws Exception {
        // This watches the Vert.x version the gateway is built against (vertx.version in the root pom).
        AtomicReference<CloseFuture> closeFuture = new AtomicReference<>();
        AtomicInteger delta = new AtomicInteger();

        vertx
            .deployVerticle(
                new AbstractVerticle() {
                    @Override
                    public void start(Promise<Void> started) {
                        closeFuture.set(((ContextInternal) context).closeFuture());
                        int before = retained(closeFuture.get());
                        List<Future<Void>> closes = new ArrayList<>();
                        for (int i = 0; i < REDEPLOYS; i++) {
                            closes.add(vertx.createHttpClient(new io.vertx.core.http.HttpClientOptions()).close());
                        }
                        Future.all(closes).onComplete(done -> {
                            delta.set(retained(closeFuture.get()) - before);
                            started.complete();
                        });
                    }
                }
            )
            .await(10, TimeUnit.SECONDS);

        assertThat(delta)
            .as(
                "[vertx-5.0-close-future-leak] Vert.x no longer leaks closed clients: remove the shared-client workaround of EndpointRuleHandler and this test"
            )
            .hasValue(REDEPLOYS);
    }

    private static Future<Void> runCheck(HttpEndpointRuleHandler<HttpEndpoint> handler) {
        // A real health check: the handler sends its request to the local server and reports the endpoint status.
        Promise<Void> checked = Promise.promise();
        handler.setStatusHandler((EndpointStatus status) -> checked.tryComplete());
        handler.setRescheduleHandler(v -> {});
        handler.handle(0L);
        return checked.future();
    }

    @SuppressWarnings("unchecked")
    private EndpointRule<HttpEndpoint> endpointRule() {
        EndpointRule<HttpEndpoint> rule = mock(EndpointRule.class);
        io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
        apiDefinition.setId("api");
        HttpEndpoint endpoint = new HttpEndpoint("default", "http://localhost:" + server.actualPort());
        endpoint.setHttpClientOptions(new HttpClientOptions());
        when(rule.endpoint()).thenReturn(endpoint);
        when(rule.api()).thenReturn(new Api(apiDefinition));

        HealthCheckStep step = new HealthCheckStep();
        step.setRequest(new HealthCheckRequest("/", HttpMethod.GET));
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));
        return rule;
    }

    private Set<String> sharedHttpClientNames() {
        return vertx.sharedData().<String, Object>getLocalMap("__vertx.shared.httpClients").keySet();
    }

    /** Waits for close() to complete asynchronously and returns the count observed once it settles or the wait expires. */
    private static int awaitRetained(CloseFuture closeFuture, int expected) {
        try {
            return Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(20, TimeUnit.MILLISECONDS)
                .until(() -> retained(closeFuture), retained -> retained == expected);
        } catch (ConditionTimeoutException e) {
            return retained(closeFuture);
        }
    }

    private static int retained(CloseFuture closeFuture) {
        try {
            Field children = CloseFuture.class.getDeclaredField("children");
            children.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) children.get(closeFuture);
            return map == null ? 0 : map.size();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                "CloseFuture.children is gone: Vert.x internals changed, this does not mean the leak is fixed. " +
                    "Re-check how closed clients are tracked before touching the shared-client workaround.",
                e
            );
        }
    }
}
