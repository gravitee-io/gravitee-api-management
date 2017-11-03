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
package io.gravitee.gateway.services.healthcheck.http;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.healthcheck.Request;
import io.gravitee.definition.model.services.healthcheck.Response;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(VertxUnitRunner.class)
public class HttpEndpointRuleHandlerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private Vertx vertx;

    @Before
    public void before(TestContext context) {
        vertx = Vertx.vertx();
    }

    @Test
    public void shouldNotValidate_invalidEndpoint(TestContext context) {
        // Prepare
        EndpointRule rule = mock(EndpointRule.class);
        when(rule.endpoint()).thenReturn(createEndpoint());

        io.gravitee.definition.model.services.healthcheck.Step step = new io.gravitee.definition.model.services.healthcheck.Step();
        Request request = new Request();
        request.setPath("/");
        request.setMethod(HttpMethod.GET);

        step.setRequest(request);
        Response response = new Response();
        response.setAssertions(Collections.singletonList(Response.DEFAULT_ASSERTION));
        step.setResponse(response);

        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule);
        Async async = context.async();

        // Verify
        runner.setStatusHandler(new Handler<EndpointStatus>() {
            @Override
            public void handle(EndpointStatus status) {
                Assert.assertFalse(status.isSuccess());
                async.complete();
            }
        });

        // Run
        runner.handle(null);

        // Wait until completion
        async.awaitSuccess();
    }

    @Test
    public void shouldValidate(TestContext context) throws InterruptedException {
        // Prepare HTTP endpoint
        stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"status\": \"green\"}")));

        // Prepare
        EndpointRule rule = mock(EndpointRule.class);
        when(rule.endpoint()).thenReturn(createEndpoint());

        io.gravitee.definition.model.services.healthcheck.Step step = new io.gravitee.definition.model.services.healthcheck.Step();
        Request request = new Request();
        request.setPath("/");
        request.setMethod(HttpMethod.GET);

        step.setRequest(request);
        Response response = new Response();
        response.setAssertions(Collections.singletonList(Response.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule);

        Async async = context.async();

        // Verify
        runner.setStatusHandler(new Handler<EndpointStatus>() {
            @Override
            public void handle(EndpointStatus status) {
                Assert.assertTrue(status.isSuccess());
                async.complete();
            }
        });

        // Run
        runner.handle(null);

        // Wait until completion
        async.awaitSuccess();
    }

    @Test
    public void shouldNotValidate_invalidResponseBody(TestContext context) throws InterruptedException {
        // Prepare HTTP endpoint
        stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"status\": \"yellow\"}")));

        // Prepare
        EndpointRule rule = mock(EndpointRule.class);
        when(rule.endpoint()).thenReturn(createEndpoint());

        io.gravitee.definition.model.services.healthcheck.Step step = new io.gravitee.definition.model.services.healthcheck.Step();
        Request request = new Request();
        request.setPath("/");
        request.setMethod(HttpMethod.GET);

        step.setRequest(request);
        Response response = new Response();
        response.setAssertions(Collections.singletonList("#jsonPath(#response.content, '$.status') == 'green'"));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleHandler runner = new HttpEndpointRuleHandler(vertx, rule);

        Async async = context.async();

        // Verify
        runner.setStatusHandler(new Handler<EndpointStatus>() {
            @Override
            public void handle(EndpointStatus status) {
                Assert.assertFalse(status.isSuccess());

                // When health-check is false, we store both request and response
                Step result = status.getSteps().get(0);
                Assert.assertEquals(HttpMethod.GET, result.getRequest().getMethod());
                Assert.assertNotNull(result.getResponse().getBody());

                async.complete();
            }
        });

        // Run
        runner.handle(null);

        // Wait until completion
        async.awaitSuccess();
    }

    private Endpoint createEndpoint() {
        return new HttpEndpoint("default", "http://localhost:" + wireMockRule.port());
    }
}
