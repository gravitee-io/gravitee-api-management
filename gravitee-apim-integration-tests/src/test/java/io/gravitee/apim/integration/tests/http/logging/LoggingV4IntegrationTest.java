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
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.reporter.api.v4.log.Log;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.HostAndPort;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoggingV4IntegrationTest {

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class ProxyApi extends AbstractLoggingV4IntegrationTest {

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
                    var hostAndPort = HostAndPort.create("127.0.0.1", 8080);
                    request.authority(hostAndPort);
                    requestHostAndPortRef.set(hostAndPort.host() + ":" + hostAndPort.port());
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
                    var hostAndPort = HostAndPort.create("127.0.0.1", 8080);
                    request.authority(hostAndPort);
                    requestHostAndPortRef.set(hostAndPort.host() + ":" + hostAndPort.port());
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
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class Error502 extends AbstractLoggingV4IntegrationTest {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);

            Endpoint endpoint = ((io.gravitee.gateway.reactive.handlers.api.v4.Api) api).getDefinition()
                .getEndpointGroups()
                .get(0)
                .getEndpoints()
                .get(0);
            endpoint.setConfiguration(endpoint.getConfiguration().replace("localhost", "non.existing.host." + UUID.randomUUID()));
        }

        @Test
        void should_log_endpoint_status_0_when_502(HttpClient httpClient, VertxTestContext context) throws Exception {
            AtomicReference<String> requestHostAndPortRef = new AtomicReference<>();
            JsonObject requestBody = new JsonObject().put("field", "of the pelennor");

            subject
                .doOnNext(log -> {
                    SoftAssertions.assertSoftly(soft -> {
                        // No body is captured since the endpoint connection failed (Gravitee streams the data without loading it in memory by default).
                        soft.assertThat(log.getEntrypointRequest().getBody()).isNullOrEmpty();
                        soft.assertThat(log.getEntrypointRequest().getUri()).isEqualTo("/test");
                        soft
                            .assertThat(log.getEntrypointRequest().getHeaders().toSingleValueMap())
                            .contains(
                                entry("host", requestHostAndPortRef.get()),
                                entry("content-length", String.valueOf(requestBody.toString().length()))
                            )
                            .containsKeys("X-Gravitee-Transaction-Id", "X-Gravitee-Request-Id");
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointRequest().getBody()).isNullOrEmpty();
                        soft.assertThat(log.getEndpointRequest().getUri()).matches("http://non.existing.host.*/endpoint");
                        soft
                            .assertThat(log.getEndpointRequest().getHeaders().toSingleValueMap())
                            .contains(entry("content-length", String.valueOf(requestBody.toString().length())))
                            .containsKeys("X-Gravitee-Transaction-Id", "X-Gravitee-Request-Id");
                    });

                    // Since the endpoint did not respond, we expect no body, status 0 and no headers.
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointResponse().getBody()).isNullOrEmpty();
                        soft.assertThat(log.getEndpointResponse().getStatus()).isEqualTo(0);
                        soft.assertThat(log.getEndpointResponse().getHeaders().toSingleValueMap()).isNullOrEmpty();
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEntrypointResponse().getBody()).isNullOrEmpty();
                        soft.assertThat(log.getEntrypointResponse().getStatus()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
                        soft
                            .assertThat(log.getEntrypointResponse().getHeaders().toSingleValueMap())
                            .containsKeys("X-Gravitee-Client-Identifier", "X-Gravitee-Transaction-Id", "X-Gravitee-Request-Id");
                    });
                })
                .doOnNext(m -> context.completeNow())
                .doOnError(context::failNow)
                .subscribe();

            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(request -> {
                    var hostAndPort = HostAndPort.create("127.0.0.1", 8080);
                    request.authority(hostAndPort);
                    requestHostAndPortRef.set(hostAndPort.host() + ":" + hostAndPort.port());
                    return request.rxSend(requestBody.toString());
                })
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete();
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class Error504 extends AbstractLoggingV4IntegrationTest {

        @Override
        public void configureApi(ReactableApi<?> api, Class<?> definitionClass) {
            super.configureApi(api, definitionClass);

            Endpoint endpoint = ((io.gravitee.gateway.reactive.handlers.api.v4.Api) api).getDefinition()
                .getEndpointGroups()
                .get(0)
                .getEndpoints()
                .get(0);
            endpoint.setSharedConfigurationOverride(
                endpoint.getSharedConfigurationOverride().replaceAll("\"readTimeout\"\s?:\s?60000", "\"readTimeout\": 100")
            );
        }

        @Test
        void should_log_endpoint_status_0_when_504(HttpClient httpClient, VertxTestContext context) throws Exception {
            JsonObject mockResponseBody = new JsonObject().put("response", "body");
            wiremock.stubFor(
                get("/endpoint").willReturn(
                    okJson(mockResponseBody.toString()).withFixedDelay(10000).withHeader(CUSTOM_HEADER, CUSTOM_HEADER_VALUE)
                )
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
                        // In case of endpoint timeout, the request body is logged because it is actually consumed and sent to the endpoint.
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

                    // Since the endpoint did not respond, we expect no body, status 0 and no headers.
                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEndpointResponse().getBody()).isNullOrEmpty();
                        soft.assertThat(log.getEndpointResponse().getStatus()).isEqualTo(0);
                        soft.assertThat(log.getEndpointResponse().getHeaders().toSingleValueMap()).isNullOrEmpty();
                    });

                    SoftAssertions.assertSoftly(soft -> {
                        soft.assertThat(log.getEntrypointResponse().getBody()).isNullOrEmpty();
                        soft.assertThat(log.getEntrypointResponse().getStatus()).isEqualTo(HttpStatusCode.GATEWAY_TIMEOUT_504);
                        soft
                            .assertThat(log.getEntrypointResponse().getHeaders().toSingleValueMap())
                            .contains(
                                entry("X-Gravitee-Transaction-Id", transactionAndRequestId),
                                entry("X-Gravitee-Request-Id", transactionAndRequestId)
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
                    var hostAndPort = HostAndPort.create("127.0.0.1", 8080);
                    request.authority(hostAndPort);
                    requestHostAndPortRef.set(hostAndPort.host() + ":" + hostAndPort.port());
                    return request.rxSend(requestBody.toString());
                })
                .flatMapPublisher(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.GATEWAY_TIMEOUT_504);
                    return response.toFlowable();
                })
                .test()
                .await()
                .assertComplete();

            wiremock.verify(getRequestedFor(urlPathEqualTo("/endpoint")));
        }
    }

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/v4/http/api.json" })
    class ProxyApiLogCustom extends AbstractLoggingV4IntegrationTest {

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
                    var hostAndPort = HostAndPort.create("127.0.0.1", 8080);
                    request.authority(hostAndPort);
                    requestHostAndPortRef.set(hostAndPort.host() + ":" + hostAndPort.port());
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
    }

    abstract class AbstractLoggingV4IntegrationTest extends AbstractGatewayTest {

        protected static final String CUSTOM_HEADER = "X-CustomHeader";
        protected static final String CUSTOM_HEADER_VALUE = "custom header value";
        BehaviorSubject<Log> subject;

        @BeforeEach
        void setUp() {
            subject = BehaviorSubject.create();

            FakeReporter fakeReporter = getBean(FakeReporter.class);
            fakeReporter.setReportableHandler(reportable -> {
                if (reportable instanceof Log l) {
                    subject.onNext(l);
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

            if (api.getDefinition() instanceof Api apiDefinition) {
                apiDefinition.setAnalytics(analytics);
            }
        }
    }
}
