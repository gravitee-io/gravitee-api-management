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
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.integration.tests.fake.ActionReportPolicy;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingContent;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.LoggingScope;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.reporter.api.log.Log;
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
@GatewayTest(v2ExecutionMode = ExecutionMode.V4_EMULATION_ENGINE)
@DeployApi({ "/apis/http/api-report.json" })
class DoOnReportV4EmulationIntegrationTest extends AbstractGatewayTest {

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
    public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
        final Logging logging = new Logging();
        logging.setMode(LoggingMode.CLIENT_PROXY);
        logging.setContent(LoggingContent.HEADERS_PAYLOADS);
        logging.setScope(LoggingScope.REQUEST_RESPONSE);
        logging.setCondition("{#response.status == 200}");

        if (api.getDefinition() instanceof Api) {
            ((Api) api.getDefinition()).getProxy().setLogging(logging);
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
                assertThat(log.getClientRequest().getBody()).isEqualTo("prefix-body request");
                assertThat(log.getClientRequest().getHeaders().get("X-RequestHeader")).isEqualTo("prefix-RequestHeaderValue");
                assertThat(log.getProxyRequest().getBody()).isEqualTo("prefix-body request");
                assertThat(log.getProxyRequest().getHeaders().get("X-RequestHeader")).isEqualTo("prefix-RequestHeaderValue");

                assertThat(log.getClientResponse().getBody()).isEqualTo("prefix-response from backend");
                assertThat(log.getClientResponse().getHeaders().get("X-ResponseHeader")).isEqualTo("prefix-ResponseHeaderValue");
                assertThat(log.getProxyResponse().getBody()).isEqualTo("prefix-response from backend");
                assertThat(log.getProxyResponse().getHeaders().get("X-ResponseHeader")).isEqualTo("prefix-ResponseHeaderValue");
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
