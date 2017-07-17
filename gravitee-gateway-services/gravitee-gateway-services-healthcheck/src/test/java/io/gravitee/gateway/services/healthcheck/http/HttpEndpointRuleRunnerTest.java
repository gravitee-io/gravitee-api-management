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
import io.gravitee.definition.model.services.healthcheck.Request;
import io.gravitee.definition.model.services.healthcheck.Response;
import io.gravitee.definition.model.services.healthcheck.Step;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.health.EndpointHealthStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEndpointRuleRunnerTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Test
    public void shouldNotValidate_invalidEndpoint() {
        // Prepare
        EndpointRule rule = mock(EndpointRule.class);
        when(rule.endpoint()).thenReturn(createEndpoint());

        Step step = new Step();
        Request request = new Request();
        request.setPath("/");
        request.setMethod(HttpMethod.GET);

        step.setRequest(request);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleRunner runner = new HttpEndpointRuleRunner(Vertx.vertx(), rule);
        ReporterService reporter = Mockito.spy(ReporterService.class);
        runner.setReporterService(reporter);

        // Run
        Future future = runner.run0();

        // Verify
        future.setHandler(new Handler<AsyncResult<EndpointHealthStatus>>() {
            @Override
            public void handle(AsyncResult<EndpointHealthStatus> healthEvent) {
                EndpointHealthStatus healthStatus = healthEvent.result();

                Assert.assertFalse(healthStatus.isSuccess());
                verify(reporter, Mockito.atLeastOnce()).report(any(Reportable.class));
            }
        });
    }

    @Test
    public void shouldValidate() throws InterruptedException {
        // Prepare HTTP endpoint
        stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"status\": \"green\"}")));

        // Prepare
        EndpointRule rule = mock(EndpointRule.class);
        when(rule.endpoint()).thenReturn(createEndpoint());

        Step step = new Step();
        Request request = new Request();
        request.setPath("/");
        request.setMethod(HttpMethod.GET);

        step.setRequest(request);
        Response response = new Response();
        response.setAssertions(Collections.singletonList(Response.DEFAULT_ASSERTION));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleRunner runner = new HttpEndpointRuleRunner(Vertx.vertx(), rule);
        ReporterService reporter = Mockito.spy(ReporterService.class);
        runner.setReporterService(reporter);

        // Run
        Future future = runner.run0();

        // Verify
        final CountDownLatch lock = new CountDownLatch(1);
        future.setHandler(new Handler<AsyncResult<EndpointHealthStatus>>() {
            @Override
            public void handle(AsyncResult<EndpointHealthStatus> healthEvent) {
                EndpointHealthStatus healthStatus = healthEvent.result();

                Assert.assertTrue(healthStatus.isSuccess());
                verify(reporter, Mockito.atLeastOnce()).report(any(Reportable.class));
                lock.countDown();
            }
        });

        Assert.assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldNotValidate_invalidResponseBody() throws InterruptedException {
        // Prepare HTTP endpoint
        stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"status\": \"yellow\"}")));

        // Prepare
        EndpointRule rule = mock(EndpointRule.class);
        when(rule.endpoint()).thenReturn(createEndpoint());

        Step step = new Step();
        Request request = new Request();
        request.setPath("/");
        request.setMethod(HttpMethod.GET);

        step.setRequest(request);
        Response response = new Response();
        response.setAssertions(Collections.singletonList("#jsonPath(#response.content, '$.status') == 'green'"));
        step.setResponse(response);
        when(rule.steps()).thenReturn(Collections.singletonList(step));

        HttpEndpointRuleRunner runner = new HttpEndpointRuleRunner(Vertx.vertx(), rule);
        ReporterService reporter = Mockito.spy(ReporterService.class);
        runner.setReporterService(reporter);

        // Run
        Future future = runner.run0();

        // Verify
        final CountDownLatch lock = new CountDownLatch(1);
        future.setHandler(new Handler<AsyncResult<EndpointHealthStatus>>() {
            @Override
            public void handle(AsyncResult<EndpointHealthStatus> healthEvent) {
                EndpointHealthStatus healthStatus = healthEvent.result();

                Assert.assertFalse(healthStatus.isSuccess());
                verify(reporter, Mockito.atLeastOnce()).report(any(Reportable.class));
                lock.countDown();
            }
        });

        Assert.assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    private Endpoint createEndpoint() {
        return new Endpoint("default", "http://localhost:" + wireMockRule.port());
    }
}
