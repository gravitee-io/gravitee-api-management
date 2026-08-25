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
package io.gravitee.apim.integration.tests.http.pathtraversal;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * A rejected request has to leave a trace.
 *
 * <p>The value of a path rejection is largely in telling an operator that someone is probing the
 * platform. Answering 400 and reporting nothing would close the hole while keeping the operator
 * blind to the fact that it was ever attacked, which is the wrong half of the job.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@DeployApi({ "/apis/v4/http/pathtraversal/api-alpha.json" })
class RejectedPathReportingIntegrationTest extends AbstractGatewayTest {

    private static final String TRAVERSAL = "/alpha/api/../../beta/api/echo";

    /**
     * A subject rather than a list read straight after the call. {@code ReporterProcessor} is the
     * last processor of the rejected chain and runs after the response has been ended, so the client
     * can observe the 400 before anything has been reported — asserting on a collection at that
     * moment passes or fails with the machine's load.
     */
    private final BehaviorSubject<Metrics> reported = BehaviorSubject.create();

    @Override
    public void configureGateway(GatewayConfigurationBuilder configurationBuilder) {
        configurationBuilder.set("http.pathHandling", "REJECT");
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @BeforeEach
    void captureReports() {
        getBean(FakeReporter.class).setReportableHandler(reportable -> {
            if (reportable instanceof Metrics metrics) {
                reported.onNext(metrics);
            }
        });
    }

    @Test
    void should_report_a_rejected_request(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(anyUrl()).willReturn(ok("response from backend")));

        final TestObserver<Metrics> metrics = reported.firstOrError().test();

        httpClient
            .rxRequest(HttpMethod.GET, TRAVERSAL)
            .flatMap(HttpClientRequest::rxSend)
            .test()
            .await()
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(400);
                return true;
            })
            .assertNoErrors();

        metrics
            .awaitDone(30, TimeUnit.SECONDS)
            .assertComplete()
            .assertValue(reportable -> {
                assertThat(reportable.getStatus()).isEqualTo(400);
                // The operator needs to see what was actually asked for, not a sanitized version.
                assertThat(reportable.getUri()).isEqualTo(TRAVERSAL);
                return true;
            });
    }
}
