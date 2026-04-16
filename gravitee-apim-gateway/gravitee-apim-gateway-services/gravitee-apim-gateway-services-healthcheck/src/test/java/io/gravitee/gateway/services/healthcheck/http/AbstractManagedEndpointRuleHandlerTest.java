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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.healthcheck.HealthCheckRequest;
import io.gravitee.definition.model.services.healthcheck.HealthCheckResponse;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractManagedEndpointRuleHandlerTest {

    private Environment environment;

    private TemplateEngine templateEngine;

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).proxyMode(true).build();

    @BeforeEach
    void setup() {
        wm.resetAll();
        environment = mock(Environment.class);
        templateEngine = mock(TemplateEngine.class);
        when(environment.getProperty("http.ssl.openssl", Boolean.class, false)).thenReturn(useOpenSsl());
        when(templateEngine.getValue(anyString(), eq(String.class))).thenAnswer(invocationOnMock ->
            invocationOnMock.getArgument(0, String.class)
        );
    }

    protected abstract Boolean useOpenSsl();

    @Test
    void shouldNotValidate_invalidEndpoint(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/")).willReturn(notFound()));
        final Checkpoint statusCheckpoint = context.checkpoint();
        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        EndpointRule rule = createEndpointRule();

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);

        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Verify
        runner.setStatusHandler(
            (Handler<EndpointStatus>) status -> {
                assertFalse(status.isSuccess());
                wm.verify(getRequestedFor(urlEqualTo("/")));
                statusCheckpoint.flag();
            }
        );
        runner.setRescheduleHandler(v -> {
            rescheduleCheckpoint.flag();
        });

        // Run
        runner.handle(null);
    }

    @Test
    void shouldValidate(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/")).willReturn(ok("{\"status\": \"green\"}")));

        final Checkpoint statusCheckpoint = context.checkpoint();
        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        // Prepare
        EndpointRule rule = createEndpointRule();

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Verify
        runner.setStatusHandler(
            (Handler<EndpointStatus>) status -> {
                assertTrue(status.isSuccess());
                wm.verify(getRequestedFor(urlEqualTo("/")));
                statusCheckpoint.flag();
            }
        );
        runner.setRescheduleHandler(v -> {
            rescheduleCheckpoint.flag();
        });

        // Run
        runner.handle(null);
    }

    @Test
    void shouldValidateWithEL(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/withProperties/")).willReturn(ok("{\"status\": \"green\"}")));

        final Checkpoint statusCheckpoint = context.checkpoint();
        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        // Prepare
        EndpointRule rule = createEndpointRule("{#properties['backend’]}");
        when(templateEngine.getValue(eq(wm.baseUrl() + "{#properties['backend’]}"), eq(String.class))).thenReturn(
            wm.baseUrl() + "/withProperties"
        );

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Verify
        runner.setStatusHandler(
            (Handler<EndpointStatus>) status -> {
                assertTrue(status.isSuccess());
                wm.verify(getRequestedFor(urlEqualTo("/withProperties/")));
                statusCheckpoint.flag();
            }
        );
        runner.setRescheduleHandler(v -> {
            rescheduleCheckpoint.flag();
        });

        // Run
        runner.handle(null);
    }

    @Test
    void shouldNotValidate_invalidResponseBody(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/")).willReturn(ok("{\"status\": \"yellow\"}")));

        final Checkpoint statusCheckpoint = context.checkpoint();
        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        // Prepare
        EndpointRule rule = createEndpointRule();

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList("#jsonPath(#response.content, '$.status') == 'green'"));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Verify
        runner.setStatusHandler(
            (Handler<EndpointStatus>) status -> {
                assertFalse(status.isSuccess());
                wm.verify(getRequestedFor(urlEqualTo("/")));

                // When health-check is false, we store both request and response
                Step result = status.getSteps().get(0);
                assertEquals(HttpMethod.GET, result.getRequest().getMethod());
                assertNotNull(result.getResponse().getBody());

                statusCheckpoint.flag();
            }
        );
        runner.setRescheduleHandler(v -> {
            rescheduleCheckpoint.flag();
        });

        // Run
        runner.handle(null);

        // Wait until completion
        assertTrue(context.awaitCompletion(5, TimeUnit.SECONDS));
        assertTrue(context.completed());
    }

    @Test
    void shouldValidateFromRoot(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/")).willReturn(ok("{\"status\": \"green\"}")));
        final Checkpoint statusCheckpoint = context.checkpoint();
        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        // Prepare
        EndpointRule rule = createEndpointRule("/additional-but-unused-path-for-hc");

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);
        request.setFromRoot(true);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Verify
        runner.setStatusHandler(
            (Handler<EndpointStatus>) status -> {
                wm.verify(getRequestedFor(urlEqualTo("/")));
                wm.verify(0, getRequestedFor(urlEqualTo("/additional-but-unused-path-for-hc")));
                assertTrue(status.isSuccess());
                statusCheckpoint.flag();
            }
        );
        runner.setRescheduleHandler(v -> rescheduleCheckpoint.flag());

        // Run
        runner.handle(null);
    }

    @Test
    void shouldValidateWithUnderscoreInHostname(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/")).withHost(equalTo("my_local_host")).willReturn(ok("{\"status\": \"green\"}")));

        final Checkpoint statusCheckpoint = context.checkpoint();
        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        // Prepare
        EndpointRule rule = createEndpointRule("http://my_local_host", null, true);

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Verify
        runner.setStatusHandler(
            (Handler<EndpointStatus>) status -> {
                assertTrue(status.isSuccess());
                wm.verify(getRequestedFor(urlEqualTo("/")));
                statusCheckpoint.flag();
            }
        );
        runner.setRescheduleHandler(v -> rescheduleCheckpoint.flag());

        // Run
        runner.handle(null);
    }

    @Test
    void shouldValidateWithFixedDelayed(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/")).willReturn(ok("{\"status\": \"green\"}").withFixedDelay(3500)));

        final Checkpoint statusCheckpoint = context.checkpoint();
        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        // Prepare
        EndpointRule rule = createEndpointRule();
        when(rule.schedule()).thenReturn("*/1 * * * * *");

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);
        Date nextExecutionDate = new CronTrigger(rule.schedule()).nextExecutionTime(new SimpleTriggerContext());

        // Verify
        runner.setStatusHandler(
            (Handler<EndpointStatus>) status -> {
                assertTrue(status.isSuccess());
                wm.verify(getRequestedFor(urlEqualTo("/")));
                statusCheckpoint.flag();
            }
        );
        runner.setRescheduleHandler(v -> {
            Date nextExecutionDateAfterDelayedRequest = new CronTrigger(rule.schedule()).nextExecutionTime(new SimpleTriggerContext());
            //at least 3 cron schedules should be ignored
            assertTrue((nextExecutionDateAfterDelayedRequest.getTime() - nextExecutionDate.getTime()) / 1000 > 2);
            rescheduleCheckpoint.flag();
        });

        // Run
        runner.handle(null);
        assertTrue(context.awaitCompletion(5, TimeUnit.SECONDS));
        assertTrue(context.completed());
    }

    @Test
    void shouldRescheduleWithFaultMalformedResponse(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        // Prepare
        EndpointRule rule = createEndpointRule();

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Verify
        runner.setRescheduleHandler(v -> {
            rescheduleCheckpoint.flag();
        });

        // Run
        runner.handle(null);
    }

    @Test
    void shouldValidateWithFaultConnectionReset(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        final Checkpoint statusCheckpoint = context.checkpoint();
        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        // Prepare
        EndpointRule rule = createEndpointRule();

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Verify
        runner.setStatusHandler(
            (Handler<EndpointStatus>) status -> {
                assertFalse(status.isSuccess());
                wm.verify(getRequestedFor(urlEqualTo("/")));
                statusCheckpoint.flag();
            }
        );
        runner.setRescheduleHandler(v -> rescheduleCheckpoint.flag());

        // Run
        runner.handle(null);
    }

    @Test
    void shouldValidateWithFaultRandomDataThenClose(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

        final Checkpoint statusCheckpoint = context.checkpoint();
        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        // Prepare
        EndpointRule rule = createEndpointRule();

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Verify
        runner.setStatusHandler(
            (Handler<EndpointStatus>) status -> {
                assertFalse(status.isSuccess());
                wm.verify(getRequestedFor(urlEqualTo("/")));
                statusCheckpoint.flag();
            }
        );
        runner.setRescheduleHandler(v -> rescheduleCheckpoint.flag());

        // Run
        runner.handle(null);
    }

    @Test
    void shouldValidateWithFaultEmptyResponse(Vertx vertx, VertxTestContext context) throws Throwable {
        // Prepare HTTP endpoint
        wm.stubFor(get(urlEqualTo("/")).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        final Checkpoint statusCheckpoint = context.checkpoint();
        final Checkpoint rescheduleCheckpoint = context.checkpoint();

        // Prepare
        EndpointRule rule = createEndpointRule();

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/", HttpMethod.GET);

        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Verify
        runner.setStatusHandler(
            (Handler<EndpointStatus>) status -> {
                assertFalse(status.isSuccess());
                wm.verify(getRequestedFor(urlEqualTo("/")));
                statusCheckpoint.flag();
            }
        );
        runner.setRescheduleHandler(v -> rescheduleCheckpoint.flag());

        // Run
        runner.handle(null);
    }

    @Test
    void requestOptionsHost_should_use_configured_Host_header_value(Vertx vertx) throws Exception {
        // Unit test for prepareHttpClientRequest (APIM-13512).
        // RequestOptions.host controls the HTTP/2 ":authority" pseudo-header AND is the fallback for the
        // HTTP/1.1 "Host" header when the headers map does not already contain one.
        // When the user configures "Host: custom-host.example.com" in the health check step, RequestOptions.host
        // must be updated accordingly; otherwise the backend may receive the wrong authority.
        //
        // With the current bug: setHost(request.getHost()) is called first and user headers are added after,
        // but RequestOptions.host remains set to the target URL's host — the custom value is never applied.
        // After the fix: when user headers contain a "Host" entry, RequestOptions.host must be set to that value.
        EndpointRule rule = createEndpointRule();
        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest hcRequest = new HealthCheckRequest("/health", HttpMethod.GET);
        hcRequest.setHeaders(List.of(new HttpHeader("Host", "custom-host.example.com")));
        step.setRequest(hcRequest);
        step.setResponse(new HealthCheckResponse());
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler handler = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Access the protected method via reflection (it lives in EndpointRuleHandler superclass).
        Method method = handler.getClass().getSuperclass().getDeclaredMethod("prepareHttpClientRequest", URL.class, HealthCheckStep.class);
        method.setAccessible(true);
        RequestOptions options = (RequestOptions) method.invoke(handler, new URL("http://localhost:8080/health"), step);

        // Bug: options.getHost() returns "localhost" because setHost(target.getHost()) was never overridden
        // by the user-configured "Host" header value.
        // After fix: options.getHost() must equal "custom-host.example.com".
        assertEquals("custom-host.example.com", options.getHost(), "RequestOptions.host must equal the user-configured Host header value");
    }

    @Test
    void shouldSendCustomHostHeaderWhenConfiguredInHealthCheckStep(Vertx vertx, VertxTestContext context) throws Throwable {
        // Stub responds to any GET /health — we verify the Host header via wm.verify() in the main thread,
        // not inside the async handler, so failures propagate correctly to the test runner.
        wm.stubFor(get(urlEqualTo("/health")).willReturn(ok("{\"status\": \"green\"}")));

        EndpointRule rule = createEndpointRule();

        HealthCheckStep step = new HealthCheckStep();
        HealthCheckRequest request = new HealthCheckRequest("/health", HttpMethod.GET);
        // Configure a custom Host header — it must override the host derived from the endpoint URL.
        request.setHeaders(List.of(new HttpHeader("Host", "custom-host.example.com")));
        step.setRequest(request);
        HealthCheckResponse response = new HealthCheckResponse();
        response.setAssertions(Collections.singletonList(HealthCheckResponse.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule, templateEngine, environment);

        // Use a CountDownLatch to block the main thread until the health check finishes.
        // Assertions are done in the main thread so that failures surface correctly.
        CountDownLatch latch = new CountDownLatch(1);
        runner.setStatusHandler((Handler<EndpointStatus>) status -> latch.countDown());
        runner.setRescheduleHandler(v -> {});

        runner.handle(null);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Health check did not complete in time");

        // Bug (APIM-13512): prepareHttpClientRequest() calls RequestOptions.setHost(request.getHost()) which
        // determines the HTTP Host header sent on the wire.  Headers added via putHeader() afterward go into the
        // header map but Vert.x still uses the RequestOptions.host field for the actual Host header, so the
        // user-configured "Host: custom-host.example.com" is silently ignored.
        // This verify call FAILS with the current code and PASSES after the fix.
        wm.verify(getRequestedFor(urlEqualTo("/health")).withHeader("Host", equalTo("custom-host.example.com")));

        context.completeNow();
    }

    private Endpoint createEndpoint(String baseUrl, String targetPath, boolean useSystemProxy) {
        HttpEndpoint aDefault = new HttpEndpoint("default", baseUrl + (targetPath != null ? targetPath : ""));
        aDefault.setHttpClientOptions(new HttpClientOptions());
        if (useSystemProxy) {
            HttpProxy httpProxy = new HttpProxy();
            httpProxy.setUseSystemProxy(true);
            httpProxy.setEnabled(true);
            aDefault.setHttpProxy(httpProxy);
        }

        return aDefault;
    }

    private EndpointRule createEndpointRule() {
        return createEndpointRule(null);
    }

    private EndpointRule createEndpointRule(String targetPath) {
        return createEndpointRule(wm.baseUrl(), targetPath, false);
    }

    private EndpointRule createEndpointRule(String baseUrl, String targetPath, boolean useSystemProxy) {
        EndpointRule rule = mock(EndpointRule.class);
        io.gravitee.definition.model.Api apiDefinition = new io.gravitee.definition.model.Api();
        apiDefinition.setId("an-api");
        Api api = new Api(apiDefinition);
        when(rule.endpoint()).thenReturn(createEndpoint(baseUrl, targetPath, useSystemProxy));
        when(rule.api()).thenReturn(api);
        when(rule.schedule()).thenReturn("0 */10 * ? * *");
        if (useSystemProxy) {
            String proxyHost = System.getProperty("http.proxyHost", "none");
            Integer proxyPort = Integer.valueOf(System.getProperty("http.proxyPort", "80"));

            ProxyOptions proxyOptions = null;
            if (!"none".equalsIgnoreCase(proxyHost)) {
                proxyOptions = new ProxyOptions();
                proxyOptions.setHost(proxyHost);
                proxyOptions.setPort(proxyPort);
            }

            when(rule.getSystemProxyOptions()).thenReturn(proxyOptions);
        }
        return rule;
    }
}
