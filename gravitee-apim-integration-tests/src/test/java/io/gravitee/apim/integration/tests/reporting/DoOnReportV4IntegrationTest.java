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
package io.gravitee.apim.integration.tests.reporting;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.integration.tests.fake.ActionReportPolicy;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingContent;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.analytics.logging.LoggingPhase;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.reporter.api.v4.log.Log;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/v4/http/api-report.json" })
class DoOnReportV4IntegrationTest extends AbstractGatewayTest {

    BehaviorSubject<Log> subject;

    @BeforeEach
    void setUp() {
        subject = BehaviorSubject.create();

        FakeReporter fakeReporter = getBean(FakeReporter.class);
        fakeReporter.setReportableHandler(reportable -> {
            if (reportable instanceof Log) {
                subject.onNext((Log) reportable);
            }
        });
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Override
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setContent(LoggingContent.builder().headers(true).payload(true).build());
        logging.setPhase(LoggingPhase.builder().request(true).response(true).build());
        logging.setCondition("{#response.status == 200}");

        var analytics = new Analytics();
        analytics.setEnabled(true);
        analytics.setLogging(logging);

        if (api.getDefinition() instanceof Api) {
            ((Api) api.getDefinition()).setAnalytics(analytics);
        }
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        policies.put("action-report-policy", PolicyBuilder.build("action-report-policy", ActionReportPolicy.class));
    }

    @Test
    @DisplayName("Should prefix all headers and bodies")
    void shouldPrefixAllHeadersAndBodies(HttpClient httpClient, VertxTestContext context) throws InterruptedException {
        wiremock.stubFor(post("/endpoint").willReturn(ok("response from backend").withHeader("X-ResponseHeader", "ResponseHeaderValue")));

        subject
            .doOnNext(log -> {
                assertThat(log.getEntrypointRequest().getBody()).isEqualTo("prefix-body request");
                assertThat(log.getEntrypointRequest().getHeaders().get("X-RequestHeader")).isEqualTo("prefix-RequestHeaderValue");
                assertThat(log.getEndpointRequest().getBody()).isEqualTo("prefix-body request");
                assertThat(log.getEndpointRequest().getHeaders().get("X-RequestHeader")).isEqualTo("prefix-RequestHeaderValue");

                assertThat(log.getEntrypointResponse().getBody()).isEqualTo("prefix-response from backend");
                assertThat(log.getEntrypointResponse().getHeaders().get("X-ResponseHeader")).isEqualTo("prefix-ResponseHeaderValue");
                assertThat(log.getEndpointResponse().getBody()).isEqualTo("prefix-response from backend");
                assertThat(log.getEndpointResponse().getHeaders().get("X-ResponseHeader")).isEqualTo("prefix-ResponseHeaderValue");
            })
            .doOnNext(m -> context.completeNow())
            .doOnError(context::failNow)
            .subscribe();

        httpClient
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(request -> request.putHeader("X-RequestHeader", "RequestHeaderValue").rxSend("body request"))
            .flatMapPublisher(response -> {
                assertThat(response.getHeader("X-ResponseHeader")).isEqualTo("ResponseHeaderValue");
                assertThat(response.statusCode()).isEqualTo(200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body.toString()).isEqualTo("response from backend");
                return true;
            })
            .assertNoErrors();
    }
}
