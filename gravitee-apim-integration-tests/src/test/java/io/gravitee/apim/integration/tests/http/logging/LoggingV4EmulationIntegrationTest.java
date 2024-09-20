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
package io.gravitee.apim.integration.tests.http.logging;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayMode;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingContent;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.LoggingScope;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.reporter.api.log.Log;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/http/api.json" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoggingV4EmulationIntegrationTest extends AbstractGatewayTest {

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

    @Test
    void should_log_everything(HttpClient httpClient, VertxTestContext context) throws Exception {
        JsonObject mockResponseBody = new JsonObject().put("response", "body");
        wiremock.stubFor(get("/endpoint").willReturn(okJson(mockResponseBody.toString())));

        AtomicReference<String> requestHostAndPortRef = new AtomicReference<>();
        JsonObject requestBody = new JsonObject().put("field", "of the pelennor");

        subject
            .doOnNext(log -> {
                final String transactionAndRequestId = wiremock
                    .getAllServeEvents()
                    .get(0)
                    .getRequest()
                    .getHeaders()
                    .getHeader("X-Gravitee-Transaction-Id")
                    .firstValue();

                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(log.getClientRequest().getBody()).isEqualTo(requestBody.toString());
                    soft.assertThat(log.getClientRequest().getUri()).isEqualTo("/test");
                    soft
                        .assertThat(log.getClientRequest().getHeaders().toSingleValueMap())
                        .contains(
                            entry("host", requestHostAndPortRef.get()),
                            entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                            entry("X-Gravitee-Request-Id", transactionAndRequestId),
                            entry("content-length", String.valueOf(requestBody.toString().length()))
                        );
                });

                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(log.getProxyRequest().getBody()).isEqualTo(requestBody.toString());
                    soft.assertThat(log.getProxyRequest().getUri()).isEqualTo("http://localhost:" + wiremock.port() + "/endpoint");
                    soft
                        .assertThat(log.getProxyRequest().getHeaders().toSingleValueMap())
                        .contains(
                            entry("Host", "localhost:" + wiremock.port()),
                            entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                            entry("X-Gravitee-Request-Id", transactionAndRequestId),
                            entry("content-length", String.valueOf(requestBody.toString().length()))
                        );
                });

                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(log.getProxyResponse().getBody()).isEqualTo(mockResponseBody.toString());
                    soft.assertThat(log.getProxyResponse().getStatus()).isEqualTo(200);
                    soft
                        .assertThat(log.getProxyResponse().getHeaders().toSingleValueMap())
                        .doesNotContainKeys("X-Gravitee-Transaction-Id", "X-Gravitee-Request-Id")
                        .contains(entry("Content-Type", "application/json"));
                });

                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(log.getClientResponse().getBody()).isEqualTo(mockResponseBody.toString());
                    soft.assertThat(log.getClientResponse().getStatus()).isEqualTo(200);
                    soft
                        .assertThat(log.getClientResponse().getHeaders().toSingleValueMap())
                        .contains(
                            entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                            entry("X-Gravitee-Request-Id", transactionAndRequestId),
                            entry("Content-Type", "application/json")
                        );
                });
            })
            .doOnNext(m -> context.completeNow())
            .doOnError(context::failNow)
            .subscribe();

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.setHost("127.0.0.1");
                requestHostAndPortRef.set(request.getHost() + ":" + request.getPort());
                return request.rxSend(requestBody.toString());
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertValue(body -> {
                assertThat(body).hasToString(mockResponseBody.toString());
                return true;
            });

        wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint")));
    }

    @Test
    void should_not_log_invalid_condition(HttpClient httpClient, VertxTestContext context) throws Exception {
        JsonObject mockResponseBody = new JsonObject().put("response", "body");
        wiremock.stubFor(get("/endpoint").willReturn(badRequest()));

        AtomicReference<String> requestHostAndPortRef = new AtomicReference<>();
        JsonObject requestBody = new JsonObject().put("field", "of the pelennor");

        subject
            .doOnNext(log -> {
                final String transactionAndRequestId = wiremock
                    .getAllServeEvents()
                    .get(0)
                    .getRequest()
                    .getHeaders()
                    .getHeader("X-Gravitee-Transaction-Id")
                    .firstValue();

                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(log.getClientRequest().getBody()).isEqualTo(requestBody.toString());
                    soft.assertThat(log.getClientRequest().getUri()).isEqualTo("/test");
                    soft
                        .assertThat(log.getClientRequest().getHeaders().toSingleValueMap())
                        .contains(
                            entry("host", requestHostAndPortRef.get()),
                            entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                            entry("X-Gravitee-Request-Id", transactionAndRequestId),
                            entry("content-length", String.valueOf(requestBody.toString().length()))
                        );
                });

                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(log.getProxyRequest().getBody()).isEqualTo(requestBody.toString());
                    soft.assertThat(log.getProxyRequest().getUri()).isEqualTo("http://localhost:" + wiremock.port() + "/endpoint");
                    soft
                        .assertThat(log.getProxyRequest().getHeaders().toSingleValueMap())
                        .contains(
                            entry("Host", "localhost:" + wiremock.port()),
                            entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                            entry("X-Gravitee-Request-Id", transactionAndRequestId),
                            entry("content-length", String.valueOf(requestBody.toString().length()))
                        );
                });

                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(log.getProxyResponse().getBody()).isBlank();
                    soft.assertThat(log.getProxyResponse().getStatus()).isEqualTo(400);
                    soft
                        .assertThat(log.getProxyResponse().getHeaders().toSingleValueMap())
                        .doesNotContainKeys("X-Gravitee-Transaction-Id", "X-Gravitee-Request-Id");
                });

                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(log.getClientResponse().getBody()).isBlank();
                    soft.assertThat(log.getClientResponse().getStatus()).isEqualTo(400);
                    soft
                        .assertThat(log.getClientResponse().getHeaders().toSingleValueMap())
                        .contains(
                            entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                            entry("X-Gravitee-Request-Id", transactionAndRequestId)
                        );
                });
            })
            .doOnNext(m -> context.completeNow())
            .doOnError(context::failNow)
            .subscribe();

        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(request -> {
                request.setHost("127.0.0.1");
                requestHostAndPortRef.set(request.getHost() + ":" + request.getPort());
                return request.rxSend(requestBody.toString());
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
                return response.toFlowable();
            })
            .test()
            .await()
            .assertComplete();

        wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint")));
    }
}
