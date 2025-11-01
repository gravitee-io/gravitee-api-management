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

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reporter.FakeReporter;
import io.gravitee.common.http.HttpStatusCode;
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
import io.gravitee.reporter.api.v4.log.Log;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoggingV4IntegrationTest {

    private static final String CUSTOM_HEADER = "X-CustomHeader";
    private static final String CUSTOM_HEADER_VALUE = "custom header value";
    BehaviorSubject<Log> subject;

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class ProxyApi extends AbstractGatewayTest {

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

        @Test
        void should_log_everything(HttpClient httpClient, VertxTestContext context) throws Exception {
            JsonObject mockResponseBody = new JsonObject().put("response", "body");
            wiremock.stubFor(
                get("/endpoint").willReturn(okJson(mockResponseBody.toString()).withHeader(CUSTOM_HEADER, CUSTOM_HEADER_VALUE))
            );

            AtomicReference<String> requestHostAndPortRef = new AtomicReference<>();
            JsonObject requestBody = new JsonObject().put("field", "of the pelennor");

            subject
                .doOnNext(log -> {
                    final String transactionAndRequestId = wiremock
                        .getAllServeEvents()
                        .getFirst()
                        .getRequest()
                        .getHeaders()
                        .getHeader("X-Gravitee-Transaction-Id")
                        .firstValue();

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEntrypointRequest().getBody()).isEqualTo(requestBody.toString());
                        soft.assertThat(log.getEntrypointRequest().getUri()).isEqualTo("/test");
                        soft
                            .assertThat(log.getEntrypointRequest().getHeaders().toSingleValueMap())
                            .contains(
                                entry("host", requestHostAndPortRef.get()),
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("content-length", String.valueOf(requestBody.toString().length()))
                            );
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointRequest().getBody()).isEqualTo(requestBody.toString());
                        soft.assertThat(log.getEndpointRequest().getUri()).isEqualTo("http://localhost:" + wiremock.port() + "/endpoint");
                        soft
                            .assertThat(log.getEndpointRequest().getHeaders().toSingleValueMap())
                            .contains(
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("content-length", String.valueOf(requestBody.toString().length()))
                            )
                            .doesNotContainKeys("host", "Host");
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointResponse().getBody()).isEqualTo(mockResponseBody.toString());
                        soft.assertThat(log.getEndpointResponse().getStatus()).isEqualTo(200);
                        soft
                            .assertThat(log.getEndpointResponse().getHeaders().toSingleValueMap())
                            .doesNotContainKeys("X-Gravitee-Transaction-Id", "X-Gravitee-Request-Id")
                            .contains(entry("Content-Type", "application/json"), entry(CUSTOM_HEADER, CUSTOM_HEADER_VALUE));
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEntrypointResponse().getBody()).isEqualTo(mockResponseBody.toString());
                        soft.assertThat(log.getEntrypointResponse().getStatus()).isEqualTo(200);
                        soft
                            .assertThat(log.getEntrypointResponse().getHeaders().toSingleValueMap())
                            .contains(
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("Content-Type", "application/json"),
                                entry(CUSTOM_HEADER, CUSTOM_HEADER_VALUE)
                            )
                            .containsKeys("X-Gravitee-Client-Identifier");
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
        void should_not_log_sse(HttpClient httpClient, VertxTestContext context) throws Exception {
            JsonObject mockResponseBody = new JsonObject().put("response", "body");
            wiremock.stubFor(
                get("/endpoint").willReturn(okJson(mockResponseBody.toString()).withHeader("Content-Type", "text/event-stream"))
            );

            AtomicReference<String> requestHostAndPortRef = new AtomicReference<>();
            JsonObject requestBody = new JsonObject().put("field", "of the pelennor");

            subject
                .doOnNext(log -> {
                    final String transactionAndRequestId = wiremock
                        .getAllServeEvents()
                        .getFirst()
                        .getRequest()
                        .getHeaders()
                        .getHeader("X-Gravitee-Transaction-Id")
                        .firstValue();

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEntrypointRequest().getBody()).isEqualTo(requestBody.toString());
                        soft.assertThat(log.getEntrypointRequest().getUri()).isEqualTo("/test");
                        soft
                            .assertThat(log.getEntrypointRequest().getHeaders().toSingleValueMap())
                            .contains(
                                entry("host", requestHostAndPortRef.get()),
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("content-length", String.valueOf(requestBody.toString().length()))
                            );
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointRequest().getBody()).isEqualTo(requestBody.toString());
                        soft.assertThat(log.getEndpointRequest().getUri()).isEqualTo("http://localhost:" + wiremock.port() + "/endpoint");
                        soft
                            .assertThat(log.getEndpointRequest().getHeaders().toSingleValueMap())
                            .contains(
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("content-length", String.valueOf(requestBody.toString().length()))
                            )
                            .doesNotContainKeys("host", "Host");
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointResponse().getBody()).isNullOrEmpty();
                        soft.assertThat(log.getEndpointResponse().getStatus()).isEqualTo(200);
                        soft
                            .assertThat(log.getEndpointResponse().getHeaders().toSingleValueMap())
                            .doesNotContainKeys("X-Gravitee-Transaction-Id", "X-Gravitee-Request-Id")
                            .contains(entry("Content-Type", "text/event-stream;charset=utf-8"));
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEntrypointResponse().getBody()).isNullOrEmpty();
                        soft.assertThat(log.getEntrypointResponse().getStatus()).isEqualTo(200);
                        soft
                            .assertThat(log.getEntrypointResponse().getHeaders().toSingleValueMap())
                            .contains(
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("Content-Type", "text/event-stream;charset=utf-8")
                            )
                            .containsKeys("X-Gravitee-Client-Identifier");
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

            if (api.getDefinition() instanceof Api apiDefinition) {
                apiDefinition.setAnalytics(analytics);
            }
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class ProxyApiLogCustom extends AbstractGatewayTest {

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

        @Test
        void should_log_sse(HttpClient httpClient, VertxTestContext context) throws Exception {
            JsonObject mockResponseBody = new JsonObject().put("response", "body");
            wiremock.stubFor(get("/endpoint").willReturn(ok(mockResponseBody.toString()).withHeader("Content-Type", "text/event-stream")));

            AtomicReference<String> requestHostAndPortRef = new AtomicReference<>();
            JsonObject requestBody = new JsonObject().put("field", "of the pelennor");

            subject
                .doOnNext(log -> {
                    final String transactionAndRequestId = wiremock
                        .getAllServeEvents()
                        .getFirst()
                        .getRequest()
                        .getHeaders()
                        .getHeader("X-Gravitee-Transaction-Id")
                        .firstValue();

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEntrypointRequest().getBody()).isEqualTo(requestBody.toString());
                        soft.assertThat(log.getEntrypointRequest().getUri()).isEqualTo("/test");
                        soft
                            .assertThat(log.getEntrypointRequest().getHeaders().toSingleValueMap())
                            .contains(
                                entry("host", requestHostAndPortRef.get()),
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("content-length", String.valueOf(requestBody.toString().length()))
                            );
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointRequest().getBody()).isEqualTo(requestBody.toString());
                        soft.assertThat(log.getEndpointRequest().getUri()).isEqualTo("http://localhost:" + wiremock.port() + "/endpoint");
                        soft
                            .assertThat(log.getEndpointRequest().getHeaders().toSingleValueMap())
                            .contains(
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("content-length", String.valueOf(requestBody.toString().length()))
                            )
                            .doesNotContainKeys("host", "Host");
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointResponse().getBody()).isEqualTo(mockResponseBody.toString());
                        soft.assertThat(log.getEndpointResponse().getStatus()).isEqualTo(200);
                        soft
                            .assertThat(log.getEndpointResponse().getHeaders().toSingleValueMap())
                            .doesNotContainKeys("X-Gravitee-Transaction-Id", "X-Gravitee-Request-Id")
                            .contains(entry("Content-Type", "text/event-stream"));
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEntrypointResponse().getBody()).isEqualTo(mockResponseBody.toString());
                        soft.assertThat(log.getEntrypointResponse().getStatus()).isEqualTo(200);
                        soft
                            .assertThat(log.getEntrypointResponse().getHeaders().toSingleValueMap())
                            .contains(
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("Content-Type", "text/event-stream")
                            )
                            .containsKeys("X-Gravitee-Client-Identifier");
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
        void should_not_log_video(HttpClient httpClient, VertxTestContext context) throws Exception {
            JsonObject mockResponseBody = new JsonObject().put("response", "body");
            wiremock.stubFor(get("/endpoint").willReturn(ok(mockResponseBody.toString()).withHeader("Content-Type", "video/mp4")));

            AtomicReference<String> requestHostAndPortRef = new AtomicReference<>();
            JsonObject requestBody = new JsonObject().put("field", "of the pelennor");

            subject
                .doOnNext(log -> {
                    final String transactionAndRequestId = wiremock
                        .getAllServeEvents()
                        .getFirst()
                        .getRequest()
                        .getHeaders()
                        .getHeader("X-Gravitee-Transaction-Id")
                        .firstValue();

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEntrypointRequest().getBody()).isEqualTo(requestBody.toString());
                        soft.assertThat(log.getEntrypointRequest().getUri()).isEqualTo("/test");
                        soft
                            .assertThat(log.getEntrypointRequest().getHeaders().toSingleValueMap())
                            .contains(
                                entry("host", requestHostAndPortRef.get()),
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("content-length", String.valueOf(requestBody.toString().length()))
                            );
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointRequest().getBody()).isEqualTo(requestBody.toString());
                        soft.assertThat(log.getEndpointRequest().getUri()).isEqualTo("http://localhost:" + wiremock.port() + "/endpoint");
                        soft
                            .assertThat(log.getEndpointRequest().getHeaders().toSingleValueMap())
                            .contains(
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("content-length", String.valueOf(requestBody.toString().length()))
                            )
                            .doesNotContainKeys("host", "Host");
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointResponse().getBody()).isNullOrEmpty();
                        soft.assertThat(log.getEndpointResponse().getStatus()).isEqualTo(200);
                        soft
                            .assertThat(log.getEndpointResponse().getHeaders().toSingleValueMap())
                            .doesNotContainKeys("X-Gravitee-Transaction-Id", "X-Gravitee-Request-Id")
                            .contains(entry("Content-Type", "video/mp4"));
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEntrypointResponse().getBody()).isNullOrEmpty();
                        soft.assertThat(log.getEntrypointResponse().getStatus()).isEqualTo(200);
                        soft
                            .assertThat(log.getEntrypointResponse().getHeaders().toSingleValueMap())
                            .contains(
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId),
                                entry("Content-Type", "video/mp4")
                            )
                            .containsKeys("X-Gravitee-Client-Identifier");
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
            logging.setOverrideContentTypeValidation("video.*|audio.*|image.*|application/octet-stream|application/pdf");

            var analytics = new Analytics();
            analytics.setEnabled(true);
            analytics.setLogging(logging);

            if (api.getDefinition() instanceof Api apiDefinition) {
                apiDefinition.setAnalytics(analytics);
            }
        }
    }
}
