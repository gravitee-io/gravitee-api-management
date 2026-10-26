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
package io.gravitee.apim.integration.tests.messages.httppost;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.entrypoint.http.post.HttpPostEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.fakes.PersistentMockEndpointConnectorFactory;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpPostEntrypointMetricsIntegrationTest extends AbstractGatewayTest {

    private ReplaySubject<Metrics> metricsSubject;

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        super.configureEntrypoints(entrypoints);
        entrypoints.putIfAbsent("http-post", EntrypointBuilder.build("http-post", HttpPostEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        super.configureEndpoints(endpoints);
        endpoints.putIfAbsent("mock", EndpointBuilder.build("mock", PersistentMockEndpointConnectorFactory.class));
    }

    @BeforeEach
    void setUp() {
        metricsSubject = ReplaySubject.create();
        getBean(FakeReporter.class).setReportableHandler(reportable -> {
            if (reportable instanceof Metrics metrics) {
                metricsSubject.onNext(metrics.toBuilder().build());
            }
        });
    }

    @Test
    @DeployApi("/apis/v4/messages/http-post/http-post-entrypoint-mock-endpoint-with-logging.json")
    void should_report_the_response_status_returned_to_the_client_in_metrics_and_log(HttpClient client) {
        int clientStatus = client
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(request -> request.rxSend("{\"field\":\"value\"}"))
            .flatMap(response -> response.rxBody().ignoreElement().andThen(io.reactivex.rxjava3.core.Single.just(response.statusCode())))
            .blockingGet();
        assertThat(clientStatus).isEqualTo(202);

        metricsSubject
            .filter(Metrics::isRequestEnded)
            .take(1)
            .test()
            .awaitDone(30, SECONDS)
            .assertValue(metrics -> {
                assertThat(metrics.getLog()).isNotNull();
                assertThat(metrics.getStatus()).as("metrics status").isEqualTo(202);
                assertThat(metrics.getLog().getEntrypointResponse().getStatus()).as("log status").isEqualTo(202);
                return true;
            });
    }
}
