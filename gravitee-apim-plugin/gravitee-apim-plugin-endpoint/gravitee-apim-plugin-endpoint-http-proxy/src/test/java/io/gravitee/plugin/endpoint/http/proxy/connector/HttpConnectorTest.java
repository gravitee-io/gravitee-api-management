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
package io.gravitee.plugin.endpoint.http.proxy.connector;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.requestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.gravitee.gateway.api.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.gravitee.gateway.api.http.HttpHeaderNames.HOST;
import static io.gravitee.gateway.api.http.HttpHeaderNames.TRANSFER_ENCODING;
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_REQUEST_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpResponse;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.failure.ConnectionFailureClassifier;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.rxjava3.core.Vertx;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpConnectorTest {

    protected static final String REQUEST_BODY = "Post body content";
    protected static final String REQUEST_BODY_CHUNK1 = "Post ";
    protected static final String REQUEST_BODY_CHUNK2 = "body chunk ";
    protected static final String REQUEST_BODY_CHUNK3 = "content";

    protected static final int REQUEST_BODY_LENGTH = REQUEST_BODY.getBytes().length;
    protected static final String BACKEND_RESPONSE_BODY = "response from backend";
    public static final int TIMEOUT_SECONDS = 60;
    private static final String ERROR_ENDPOINT = "/error";
    /**
     * Path of the stubs that answer late. Their serve outlives the test that started it and re-enters the request
     * journal after the class-level reset, so it is kept off the path every other test counts requests on.
     */
    private static final String SLOW_ENDPOINT = "/slow";
    private static final int BACKEND_FIXED_DELAY_MS = 2_000;
    private static final long READ_TIMEOUT_MS = 500;
    private static final long NO_FURTHER_REQUEST_WINDOW_MS = 1_000;
    private static final long REQUEST_JOURNAL_POLL_INTERVAL_MS = 10;
    /** Port nothing listens on, to exercise a connection that cannot be acquired. */
    private static final int UNBOUND_PORT = 1;
    /**
     * How long to let doFinally's action land. Draining the chunks returns as soon as the subscriber sees the terminal
     * signal, which doFinally propagates before running its own action — so the measure is set just after, on a
     * schedule the test does not control.
     */
    private static final long VERIFY_TIMEOUT_MS = 5_000;
    private static final String POOLED_CONNECTION_SCENARIO = "pooled keep-alive connection";
    private static final String PEER_CLOSED_THE_POOLED_CONNECTION = "peer closed the pooled connection";
    private static final String PEER_ACCEPTS_A_FRESH_CONNECTION = "peer accepts a fresh connection";
    private static WireMockServer wiremock;
    private static Vertx vertx;

    @Mock
    private DeploymentContext deploymentCtx;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private HttpExecutionContext ctx;

    @Mock
    private HttpRequest request;

    @Mock
    private HttpResponse response;

    @Mock
    private Metrics metrics;

    @Mock
    private Tracer tracer;

    private HttpHeaders requestHeaders;
    private HttpHeaders responseHeaders;
    private HttpProxyEndpointConnectorConfiguration configuration;
    private HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration;
    private HttpConnector cut;

    @BeforeAll
    static void setup() {
        final WireMockConfiguration wireMockConfiguration = wireMockConfig().dynamicPort().dynamicHttpsPort();
        wiremock = new WireMockServer(wireMockConfiguration);
        wiremock.start();
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void tearDown() {
        wiremock.stop();
        wiremock.shutdownServer();
        vertx.close().blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @BeforeEach
    void init() {
        final WireMockConfiguration wireMockConfiguration = wireMockConfig().dynamicPort().dynamicHttpsPort();
        wiremock = new WireMockServer(wireMockConfiguration);
        wiremock.start();

        lenient().when(deploymentCtx.getTemplateEngine()).thenReturn(templateEngine);

        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(ctx.metrics()).thenReturn(metrics);
        lenient().when(ctx.getTracer()).thenReturn(new Tracer(null, new NoOpTracer()));
        // withLogger is an interface default method, so Mockito's mock doesn't run it and returns null unless stubbed.
        lenient().when(ctx.withLogger(any())).thenReturn(mock(Logger.class));

        requestHeaders = HttpHeaders.create();
        // request.parameters() can't be null. See https://github.com/gravitee-io/gravitee-common/blob/master/src/main/java/io/gravitee/common/util/URIUtils.java#L74
        lenient().when(request.parameters()).thenReturn(new LinkedMultiValueMap<>());
        lenient().when(request.pathInfo()).thenReturn("");
        lenient().when(request.headers()).thenReturn(requestHeaders);
        lenient().when(request.chunks()).thenReturn(Flowable.empty());

        responseHeaders = HttpHeaders.create();
        lenient().when(response.headers()).thenReturn(responseHeaders);

        lenient().when(ctx.getComponent(Vertx.class)).thenReturn(vertx);
        lenient().when(ctx.getComponent(Configuration.class)).thenReturn(mock(Configuration.class));

        configuration = new HttpProxyEndpointConnectorConfiguration();
        configuration.setTarget("http://localhost:" + wiremock.port() + "/team");
        sharedConfiguration = new HttpProxyEndpointConnectorSharedConfiguration();
        cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory());
    }

    @AfterEach
    void cleanUp() {
        wiremock.resetAll();
    }

    @Test
    void should_execute_get_request() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
        // Once the upstream connection/stream is acquired, the flag is set so a later timeout is classified as a read
        // timeout rather than a connect timeout (APIM-12769). A refactor dropping the doOnSuccess must fail here.
        verify(ctx).setInternalAttribute(HttpConnector.ATTR_INTERNAL_UPSTREAM_CONNECTION_ACQUIRED, Boolean.TRUE);
    }

    @Test
    void should_record_backend_connection_reset_on_metrics_when_response_stream_fails() {
        when(metrics.getErrorKey()).thenReturn(null);

        cut.recordBackendResponseStreamFailure(ctx, new IOException("Connection reset by peer"));

        verify(metrics).setErrorKey("GATEWAY_CLIENT_CONNECTION_RESET");
        verify(metrics).setErrorMessage("Connection reset by peer");
        // Status was already committed before streaming, so it must not be touched.
        verify(response, never()).status(anyInt());
    }

    @Test
    void should_not_overwrite_an_already_recorded_error_on_response_stream_failure() {
        when(metrics.getErrorKey()).thenReturn("CLIENT_ABORTED_DURING_RESPONSE_ERROR");

        cut.recordBackendResponseStreamFailure(ctx, new IOException("Connection reset by peer"));

        verify(metrics, never()).setErrorKey(anyString());
        verify(metrics, never()).setErrorMessage(anyString());
    }

    @Test
    void should_execute_get_request_with_merged_query_parameters() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo", "otherBar");
        parameters.add("other", "otherValue");

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.parameters()).thenReturn(parameters);

        configuration.setTarget("http://localhost:" + wiremock.port() + "/team?foo=bar");
        cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory());

        wiremock.stubFor(get(urlPathEqualTo("/team")).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/team"))
                .withQueryParam("foo", equalTo("bar"))
                .withQueryParam("foo", equalTo("otherBar"))
                .withQueryParam("other", equalTo("otherValue"))
                .withHost(equalTo("localhost"))
        );
    }

    @Test
    void should_execute_get_request_when_endpoint_attribute_overriden_with_absolute_url() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("http://127.0.0.1:" + wiremock.port());

        wiremock.stubFor(get("/").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/")).withHost(equalTo("127.0.0.1")));
    }

    @Test
    void should_execute_get_request_when_endpoint_attribute_overriden_with_absolute_url_and_query_parameters() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo", "otherBar");
        parameters.add("other", "otherValue");

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.parameters()).thenReturn(parameters);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("http://127.0.0.1:" + wiremock.port() + "/?foo=bar");

        wiremock.stubFor(get(urlPathEqualTo("/")).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/"))
                .withQueryParam("foo", equalTo("bar"))
                .withQueryParam("foo", equalTo("otherBar"))
                .withQueryParam("other", equalTo("otherValue"))
                .withHost(equalTo("127.0.0.1"))
        );
    }

    @Test
    void should_execute_get_request_when_attribute_overriden_with_absolute_url_and_path() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("http://127.0.0.1:" + wiremock.port() + "/team/subPath");

        wiremock.stubFor(get(urlPathEqualTo("/team/subPath")).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team/subPath")).withHost(equalTo("127.0.0.1")));
    }

    @Test
    void should_execute_get_request_when_attribute_overriden_with_absolute_url_and_path_and_query_parameters() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo", "otherBar");
        parameters.add("other", "otherValue");

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.parameters()).thenReturn(parameters);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("http://127.0.0.1:" + wiremock.port() + "/team/subPath?foo=bar");

        wiremock.stubFor(get(urlPathEqualTo("/team/subPath")).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/team/subPath"))
                .withQueryParam("foo", equalTo("bar"))
                .withQueryParam("foo", equalTo("otherBar"))
                .withQueryParam("other", equalTo("otherValue"))
                .withHost(equalTo("127.0.0.1"))
        );
    }

    @Test
    void should_execute_get_request_when_attribute_overriden_with_relative_url_and_path_and_query_parameters() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo", "otherBar");
        parameters.add("other", "otherValue");

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.parameters()).thenReturn(parameters);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("/subPath?foo=bar");

        wiremock.stubFor(get(urlPathEqualTo("/team/subPath")).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/team/subPath"))
                .withQueryParam("foo", equalTo("bar"))
                .withQueryParam("foo", equalTo("otherBar"))
                .withQueryParam("other", equalTo("otherValue"))
                .withHost(equalTo("localhost")) // -> use the host defined by the configuration 'target'.
        );
    }

    @Test
    void should_execute_post_request() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.POST);
        when(request.headers()).thenReturn(HttpHeaders.create().add(TRANSFER_ENCODING, "chunked"));
        when(request.chunks()).thenReturn(
            Flowable.just(Buffer.buffer(REQUEST_BODY_CHUNK1), Buffer.buffer(REQUEST_BODY_CHUNK2), Buffer.buffer(REQUEST_BODY_CHUNK3))
        );

        wiremock.stubFor(post(urlPathEqualTo("/team")).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();
        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo("/team"))
                .withHeader(TRANSFER_ENCODING, new EqualToPattern("chunked"))
                .withRequestBody(new EqualToPattern(REQUEST_BODY_CHUNK1 + REQUEST_BODY_CHUNK2 + REQUEST_BODY_CHUNK3))
        );
    }

    @Test
    void should_execute_post_request_chunked() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.POST);
        when(request.headers()).thenReturn(HttpHeaders.create().add(TRANSFER_ENCODING, "chunked"));
        when(request.chunks()).thenReturn(Flowable.just(Buffer.buffer(REQUEST_BODY)));

        wiremock.stubFor(post("/team").withRequestBody(new EqualToPattern(REQUEST_BODY)).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo("/team"))
                .withHeader(TRANSFER_ENCODING, new EqualToPattern("chunked"))
                .withRequestBody(new EqualToPattern(REQUEST_BODY.trim()))
        );
    }

    @Test
    void should_drop_transfer_encoding_when_content_length_also_present() throws InterruptedException {
        // A policy set a fresh Content-Length on a chunked request. The backend must receive
        // Content-Length only, never both (RFC 9112 §6.1).
        when(request.method()).thenReturn(HttpMethod.POST);
        when(request.headers()).thenReturn(
            HttpHeaders.create().add(TRANSFER_ENCODING, "chunked").add(CONTENT_LENGTH, Integer.toString(REQUEST_BODY_LENGTH))
        );
        when(request.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer(REQUEST_BODY)));

        wiremock.stubFor(post("/team").withRequestBody(new EqualToPattern(REQUEST_BODY)).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo("/team"))
                .withoutHeader(TRANSFER_ENCODING)
                .withHeader(CONTENT_LENGTH, new EqualToPattern(Integer.toString(REQUEST_BODY_LENGTH)))
                .withRequestBody(new EqualToPattern(REQUEST_BODY))
        );
    }

    @Test
    void should_propagate_request_headers_and_remove_hop_headers() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);

        requestHeaders.add("X-Custom", List.of("value1", "value2"));
        HttpConnector.HOP_HEADERS.forEach(header -> requestHeaders.add(header.toString(), "should be removed"));

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlPathEqualTo("/team"))
            .withHeader("X-Custom", new EqualToPattern("value1"))
            .withHeader("X-Custom", new EqualToPattern("value2"));

        for (CharSequence header : HttpConnector.HOP_HEADERS) {
            requestPatternBuilder = requestPatternBuilder.withoutHeader(header.toString());
        }

        wiremock.verify(1, requestPatternBuilder);
    }

    @Test
    void should_add_or_replace_request_headers_with_configuration() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        sharedConfiguration.setHeaders(List.of(new HttpHeader("X-To-Be-Overriden", "Override"), new HttpHeader("X-To-Be-Added", "Added")));

        requestHeaders.add("X-Custom", "value1");
        requestHeaders.add("X-To-Be-Overriden", List.of("toOverrideValue1", "toOverrideValue2"));

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlPathEqualTo("/team"))
            .withHeader("X-Custom", new EqualToPattern("value1"))
            .withHeader("X-To-Be-Overriden", new EqualToPattern("Override"))
            .withHeader("X-To-Be-Added", new EqualToPattern("Added"));

        for (CharSequence header : HttpConnector.HOP_HEADERS) {
            requestPatternBuilder = requestPatternBuilder.withoutHeader(header.toString());
        }

        wiremock.verify(1, requestPatternBuilder);
    }

    @Test
    void should_override_host_with_request_host_header() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.originalHost()).thenReturn("localhost:8082");

        // Simulated a policy that force the host header to use when calling the backend endpoint.
        when(request.host()).thenReturn("api.gravitee.io");
        requestHeaders.add("X-Custom", "value1");

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlPathEqualTo("/team"))
            .withHeader(HOST, new EqualToPattern("api.gravitee.io"))
            .withHeader("X-Custom", new EqualToPattern("value1"));

        for (CharSequence header : HttpConnector.HOP_HEADERS) {
            requestPatternBuilder = requestPatternBuilder.withoutHeader(header.toString());
        }

        wiremock.verify(1, requestPatternBuilder);
    }

    @Test
    void should_not_override_request_host_header_when_same_as_request_original_host() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.originalHost()).thenReturn("api.gravitee.io");
        when(request.host()).thenReturn("api.gravitee.io");

        requestHeaders.add("X-Custom", "value1");

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlPathEqualTo("/team"))
            .withHeader(HOST, new EqualToPattern("localhost:" + wiremock.port()))
            .withHeader("X-Custom", new EqualToPattern("value1"));

        for (CharSequence header : HttpConnector.HOP_HEADERS) {
            requestPatternBuilder = requestPatternBuilder.withoutHeader(header.toString());
        }

        wiremock.verify(1, requestPatternBuilder);
    }

    @Test
    void should_override_host_header_with_PropagateClientHost_option_and_originalHost_equal_to_host() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.originalHost()).thenReturn("api.gravitee.io");
        when(request.host()).thenReturn("api.gravitee.io");

        sharedConfiguration.getHttpOptions().setPropagateClientHost(true);

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlPathEqualTo("/team")).withHeader(
            HOST,
            new EqualToPattern("api.gravitee.io")
        );

        wiremock.verify(1, requestPatternBuilder);
    }

    @Test
    void should_override_host_header_with_PropagateClientHost_option_and_host_different_from_originalHost() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.originalHost()).thenReturn("api.gravitee.io");
        when(request.host()).thenReturn("new.host.com");

        sharedConfiguration.getHttpOptions().setPropagateClientHost(true);

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlPathEqualTo("/team")).withHeader(
            HOST,
            new EqualToPattern("new.host.com")
        );

        wiremock.verify(1, requestPatternBuilder);
    }

    @Test
    void should_not_override_host_header_with_PropagateClientHost_option_and_null_host() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.originalHost()).thenReturn("api.gravitee.io");
        when(request.host()).thenReturn(null);

        sharedConfiguration.getHttpOptions().setPropagateClientHost(true);

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlPathEqualTo("/team")).withHeader(
            HOST,
            new EqualToPattern("localhost:" + wiremock.port())
        );

        wiremock.verify(1, requestPatternBuilder);
    }

    @Test
    void should_connect_to_hc_absolute_target_not_to_endpoint_default_host_when_custom_host_header_is_configured()
        throws InterruptedException {
        // Regression: setServer() must use options.getHost() (actual HC target) not defaultHost (endpoint config).
        // Using defaultHost when the two differ causes the TCP connection to go to the wrong server.
        configuration.setTarget("http://endpoint-host.invalid/");
        sharedConfiguration.setHeaders(List.of(new HttpHeader("Host", "custom-backend.example.com")));
        cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory());

        when(request.method()).thenReturn(HttpMethod.GET);
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("http://127.0.0.1:" + wiremock.port() + "/health");

        wiremock.stubFor(get("/health").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/health")).withHeader(HOST, equalTo("custom-backend.example.com")));
    }

    @Test
    void should_propagate_request_vertx_http_header_without_temporary_copy() throws InterruptedException {
        requestHeaders = new VertxHttpHeaders(new HeadersMultiMap());
        when(request.headers()).thenReturn(requestHeaders);
        when(request.method()).thenReturn(HttpMethod.GET);

        requestHeaders.add("X-Custom", List.of("value1", "value2"));
        HttpConnector.HOP_HEADERS.forEach(header -> requestHeaders.add(header.toString(), "should be removed"));

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        RequestPatternBuilder requestPatternBuilder = getRequestedFor(urlPathEqualTo("/team"))
            .withHeader("X-Custom", new EqualToPattern("value1"))
            .withHeader("X-Custom", new EqualToPattern("value2"));

        for (CharSequence header : HttpConnector.HOP_HEADERS) {
            requestPatternBuilder = requestPatternBuilder.withoutHeader(header.toString());
        }

        wiremock.verify(1, requestPatternBuilder);
    }

    @Test
    void should_propagate_response_headers() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);

        wiremock.stubFor(
            get("/team").willReturn(
                ok(BACKEND_RESPONSE_BODY).withHeader("X-Response-Header", "Value1", "Value2").withHeader("X-Other", "OtherValue")
            )
        );

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        assertEquals(List.of("Value1", "Value2"), responseHeaders.getAll("X-Response-Header"));
        assertEquals(List.of("OtherValue"), responseHeaders.getAll("X-Other"));
        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    void should_propagate_response_headers_when_vertx_response_header() throws InterruptedException {
        responseHeaders = new VertxHttpHeaders(new HeadersMultiMap());

        when(response.headers()).thenReturn(responseHeaders);
        when(request.method()).thenReturn(HttpMethod.GET);

        wiremock.stubFor(
            get("/team").willReturn(
                ok(BACKEND_RESPONSE_BODY).withHeader("X-Response-Header", "Value1", "Value2").withHeader("X-Other", "OtherValue")
            )
        );

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        assertEquals(List.of("Value1", "Value2"), responseHeaders.getAll("X-Response-Header"));
        assertEquals(List.of("OtherValue"), responseHeaders.getAll("X-Other"));
        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    void should_throw_illegal_argument_exception_with_null_target() {
        configuration.setTarget(null);

        assertThrows(IllegalArgumentException.class, () ->
            cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory())
        );
    }

    @Test
    void should_execute_request_with_query_parameters() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo1", "bar1");
        parameters.add("foo2", "bar2");

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.parameters()).thenReturn(parameters);

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/team"))
                .withQueryParam("foo1", new EqualToPattern("bar1"))
                .withQueryParam("foo2", new EqualToPattern("bar2"))
        );
    }

    @Test
    void should_executeRequestWithQueryParametersMergedWithTargetQueryParams() throws InterruptedException {
        var parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("foo1", "bar1");
        parameters.add("foo2", "bar2");
        parameters.add("foo3", null);

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.parameters()).thenReturn(parameters);

        configuration.setTarget("http://localhost:" + wiremock.port() + "/team?param1=value1&param2=value2");
        cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory());

        wiremock.stubFor(get(urlPathEqualTo("/team")).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(
            1,
            getRequestedFor(urlPathEqualTo("/team"))
                .withQueryParam("foo1", new EqualToPattern("bar1"))
                .withQueryParam("foo2", new EqualToPattern("bar2"))
                .withQueryParam("param1", new EqualToPattern("value1"))
                .withQueryParam("param2", new EqualToPattern("value2"))
                .withQueryParam("foo3", new EqualToPattern(""))
        );
    }

    @Test
    void should_error_when_exception_is_thrown() {
        configuration.setTarget("http://localhost:" + wiremock.port() + "/team");

        cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory());

        final TestObserver<Void> obs = cut.connect(ctx).test();
        obs.assertError(NullPointerException.class);
    }

    @Test
    void shouldHandleServerError() throws InterruptedException {
        lenient().when(request.method()).thenReturn(HttpMethod.GET);
        // Configure endpoint that returns server error
        configuration.setTarget("http://localhost:" + wiremock.port() + ERROR_ENDPOINT);
        cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory());

        wiremock.stubFor(get(ERROR_ENDPOINT).willReturn(serverError()));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();
        verify(tracer, never()).endOnError(any(Span.class), any(Throwable.class));
        verify(ctx, never()).setInternalAttribute(
            eq(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE),
            any(ExecutionFailure.class)
        );
        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/error")));
    }

    @ParameterizedTest
    @EnumSource(value = Fault.class, names = { "EMPTY_RESPONSE", "CONNECTION_RESET_BY_PEER" })
    void should_retry_on_a_fresh_connection_when_the_pooled_connection_was_closed_or_reset_by_the_peer(final Fault fault)
        throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);

        wiremock.stubFor(
            get("/team")
                .inScenario(POOLED_CONNECTION_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(ok(BACKEND_RESPONSE_BODY))
                .willSetStateTo(PEER_CLOSED_THE_POOLED_CONNECTION)
        );
        wiremock.stubFor(
            get("/team")
                .inScenario(POOLED_CONNECTION_SCENARIO)
                .whenScenarioStateIs(PEER_CLOSED_THE_POOLED_CONNECTION)
                .willReturn(aResponse().withFault(fault))
                .willSetStateTo(PEER_ACCEPTS_A_FRESH_CONNECTION)
        );
        wiremock.stubFor(
            get("/team")
                .inScenario(POOLED_CONNECTION_SCENARIO)
                .whenScenarioStateIs(PEER_ACCEPTS_A_FRESH_CONNECTION)
                .willReturn(ok(BACKEND_RESPONSE_BODY))
        );

        primePooledConnection();

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();
        verify(response).status(HttpStatusCode.OK_200);
        assertThat(drainChunks(response).values().stream().map(Buffer::toString).collect(Collectors.joining())).isEqualTo(
            BACKEND_RESPONSE_BODY
        );
        wiremock.verify(3, getRequestedFor(urlPathEqualTo("/team")));
    }

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, names = { "GET", "HEAD", "OPTIONS", "PUT", "DELETE" })
    void should_retry_a_body_less_idempotent_request_with_the_exact_same_request(final HttpMethod method) throws InterruptedException {
        when(request.method()).thenReturn(method);
        final AtomicInteger chunksSubscriptions = new AtomicInteger();
        when(request.chunks()).thenReturn(Flowable.<Buffer>empty().doOnSubscribe(subscription -> chunksSubscriptions.incrementAndGet()));
        // Identifies the failed attempt and its retry in the request journal below; the priming request never carries it.
        final String traceId = "3f1c9e2a";
        requestHeaders.set("X-Test-Trace-Id", traceId);

        // The priming request always uses GET (see primePooledConnection), so this transitional stub matches any
        // method to consume it and drive the scenario into the peer-closed state before the request under test begins.
        wiremock.stubFor(
            any(urlPathEqualTo("/team"))
                .inScenario(POOLED_CONNECTION_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(ok())
                .willSetStateTo(PEER_CLOSED_THE_POOLED_CONNECTION)
        );
        wiremock.stubFor(
            request(method.name(), urlPathEqualTo("/team"))
                .inScenario(POOLED_CONNECTION_SCENARIO)
                .whenScenarioStateIs(PEER_CLOSED_THE_POOLED_CONNECTION)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo(PEER_ACCEPTS_A_FRESH_CONNECTION)
        );
        wiremock.stubFor(
            request(method.name(), urlPathEqualTo("/team"))
                .inScenario(POOLED_CONNECTION_SCENARIO)
                .whenScenarioStateIs(PEER_ACCEPTS_A_FRESH_CONNECTION)
                .willReturn(ok())
        );

        primePooledConnection();

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();
        verify(response).status(HttpStatusCode.OK_200);

        // The priming request always uses GET (see above), so it never carries this header: filtering on it isolates
        // exactly the failed attempt and its retry, proving the retry happened for each idempotent method — the retry
        // is now gated on the method's idempotence, so nothing else here would catch a regression narrowing it to GET.
        final List<LoggedRequest> retriableAttempts = wiremock
            .findAll(requestedFor(method.name(), urlPathEqualTo("/team")))
            .stream()
            .filter(loggedRequest -> loggedRequest.containsHeader("X-Test-Trace-Id"))
            .collect(Collectors.toList());
        // One for the failed attempt, one for its retry: the options built for the first attempt are reused as-is,
        // not rebuilt, so proving both carry this request-specific header rules out a silent divergence between them.
        assertThat(retriableAttempts).hasSize(2);
        final LoggedRequest failedAttempt = retriableAttempts.get(0);
        final LoggedRequest retriedAttempt = retriableAttempts.get(1);
        assertThat(retriedAttempt.getMethod()).isEqualTo(failedAttempt.getMethod());
        assertThat(retriedAttempt.getUrl()).isEqualTo(failedAttempt.getUrl());
        assertThat(failedAttempt.getHeader("X-Test-Trace-Id")).isEqualTo(traceId);
        assertThat(retriedAttempt.getHeader("X-Test-Trace-Id")).isEqualTo(traceId);
        assertThat(chunksSubscriptions.get())
            .as("Client body should be consumed exactly once across the retry, neither lost nor read again")
            .isEqualTo(1);
    }

    @Test
    void should_not_resend_a_body_less_post_when_the_pooled_connection_was_closed_by_the_peer() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.POST);

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));
        wiremock.stubFor(post("/team").willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        primePooledConnection();

        final AtomicReference<Throwable> connectFailure = new AtomicReference<>();
        final TestObserver<Void> obs = cut.connect(ctx).doOnError(connectFailure::set).test();

        assertNoTimeout(obs);
        obs.assertNotComplete();
        assertThat(ConnectionFailureClassifier.classify(connectFailure.get()).key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_CLOSED");
        wiremock.verify(1, postRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    void should_not_resend_the_request_when_the_connection_failed_after_the_body_was_streamed() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.POST);
        when(request.headers()).thenReturn(HttpHeaders.create().add(TRANSFER_ENCODING, "chunked"));
        when(request.chunks()).thenReturn(
            Flowable.just(Buffer.buffer(REQUEST_BODY_CHUNK1), Buffer.buffer(REQUEST_BODY_CHUNK2), Buffer.buffer(REQUEST_BODY_CHUNK3))
        );

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));
        // The peer takes the whole body, then ends the connection without answering: the bytes of a non-idempotent
        // request have already reached the backend, so this failure is not the pre-write pool-reuse race and must
        // never be answered by sending the request a second time.
        wiremock.stubFor(post("/team").willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        primePooledConnection();

        final AtomicReference<Throwable> connectFailure = new AtomicReference<>();
        final TestObserver<Void> obs = cut.connect(ctx).doOnError(connectFailure::set).test();

        assertNoTimeout(obs);
        obs.assertNotComplete();
        assertThat(ConnectionFailureClassifier.classify(connectFailure.get()).key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_CLOSED");
        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo("/team")).withRequestBody(
                new EqualToPattern(REQUEST_BODY_CHUNK1 + REQUEST_BODY_CHUNK2 + REQUEST_BODY_CHUNK3)
            )
        );
    }

    @Test
    void should_not_resend_an_http2_request_when_the_pooled_connection_was_closed_by_the_peer() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        // An HTTP/2 request signals its body with Data frames rather than with Content-Length or Transfer-Encoding, so
        // the gateway streams a body it cannot replay even though no body header says so — leaving the headers unset
        // is what makes the protocol alone drive the exclusion here.
        when(request.version()).thenReturn(HttpVersion.HTTP_2);
        // Identifies the single attempt in the request journal below; the priming request never carries it.
        requestHeaders.set("X-Test-Trace-Id", "7b4d0c15");

        wiremock.stubFor(
            get("/team")
                .inScenario(POOLED_CONNECTION_SCENARIO)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(ok())
                .willSetStateTo(PEER_CLOSED_THE_POOLED_CONNECTION)
        );
        // No follow-up success stub: a resend would hit this same fault and be recorded, so the journal count below is
        // what proves the request was never sent twice.
        wiremock.stubFor(
            get("/team")
                .inScenario(POOLED_CONNECTION_SCENARIO)
                .whenScenarioStateIs(PEER_CLOSED_THE_POOLED_CONNECTION)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
        );

        primePooledConnection();

        final AtomicReference<Throwable> connectFailure = new AtomicReference<>();
        final TestObserver<Void> obs = cut.connect(ctx).doOnError(connectFailure::set).test();

        assertNoTimeout(obs);
        obs.assertNotComplete();
        assertThat(ConnectionFailureClassifier.classify(connectFailure.get()).key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_CLOSED");

        final List<LoggedRequest> attempts = wiremock
            .findAll(getRequestedFor(urlPathEqualTo("/team")))
            .stream()
            .filter(loggedRequest -> loggedRequest.containsHeader("X-Test-Trace-Id"))
            .collect(Collectors.toList());
        assertThat(attempts).hasSize(1);
    }

    @Test
    void should_not_retry_a_second_time_when_the_retried_connection_is_also_closed_by_the_peer() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);

        // Every attempt hits the same fault, so the retried one fails the same way the first did instead of being
        // answered by a scripted success — which is what makes the second failure reach the attempt cap.
        wiremock.stubFor(get("/team").willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        final AtomicReference<Throwable> connectFailure = new AtomicReference<>();
        final TestObserver<Void> obs = cut.connect(ctx).doOnError(connectFailure::set).test();

        assertNoTimeout(obs);
        obs.assertNotComplete();
        assertThat(ConnectionFailureClassifier.classify(connectFailure.get()).key()).isEqualTo("GATEWAY_CLIENT_CONNECTION_CLOSED");
        // The failure surfaces to the client after a single retry: one original attempt and one retry, never a third.
        wiremock.verify(2, getRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    void should_not_retry_a_body_less_request_when_the_failure_is_not_a_closed_or_reset_connection() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        sharedConfiguration.getHttpOptions().setReadTimeout(READ_TIMEOUT_MS);
        configuration.setTarget("http://localhost:" + wiremock.port() + SLOW_ENDPOINT);
        cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory());

        // Answers well past the read timeout above, so the exchange fails on a timeout rather than on a connection the
        // peer closed or reset.
        wiremock.stubFor(get(SLOW_ENDPOINT).willReturn(ok(BACKEND_RESPONSE_BODY).withFixedDelay(BACKEND_FIXED_DELAY_MS)));

        final AtomicReference<Throwable> connectFailure = new AtomicReference<>();
        final TestObserver<Void> obs = cut.connect(ctx).doOnError(connectFailure::set).test();

        assertNoTimeout(obs);
        obs.assertNotComplete();
        assertThat(ConnectionFailureClassifier.classify(connectFailure.get()).key()).isEqualTo("GATEWAY_CLIENT_READ_TIMEOUT");
        // A retry would have been sent before the failure reached the client, so the journal is final here.
        wiremock.verify(1, getRequestedFor(urlPathEqualTo(SLOW_ENDPOINT)));
    }

    @Test
    void should_not_retry_when_the_in_flight_request_is_reset_by_our_own_cancellation() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        configuration.setTarget("http://localhost:" + wiremock.port() + SLOW_ENDPOINT);
        cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory());

        wiremock.stubFor(get(SLOW_ENDPOINT).willReturn(ok(BACKEND_RESPONSE_BODY).withFixedDelay(BACKEND_FIXED_DELAY_MS)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        final RequestPatternBuilder slowRequests = getRequestedFor(urlPathEqualTo(SLOW_ENDPOINT));
        awaitRequestReceived(slowRequests);
        // The disposal below has to race a live upstream call: on an already resolved exchange it resets nothing and
        // the rest of this test would pass vacuously.
        obs.assertNotComplete();

        obs.dispose();

        // Resetting the in-flight stream ourselves classifies exactly like a peer-initiated close, so only the flag
        // doOnDispose sets keeps the retry predicate from sending the request a second time.
        assertRequestCountStaysAt(slowRequests, 1);
    }

    private void awaitRequestReceived(final RequestPatternBuilder pattern) throws InterruptedException {
        final long deadlineNs = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (wiremock.findAll(pattern).isEmpty() && System.nanoTime() - deadlineNs < 0) {
            Thread.sleep(REQUEST_JOURNAL_POLL_INTERVAL_MS);
        }
        assertThat(wiremock.findAll(pattern)).as("Backend should have received the request before timeout").isNotEmpty();
    }

    private void assertRequestCountStaysAt(final RequestPatternBuilder pattern, final int expectedCount) throws InterruptedException {
        final long deadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(NO_FURTHER_REQUEST_WINDOW_MS);
        while (System.nanoTime() - deadlineNs < 0) {
            assertThat(wiremock.findAll(pattern)).hasSize(expectedCount);
            Thread.sleep(REQUEST_JOURNAL_POLL_INTERVAL_MS);
        }
    }

    /**
     * Runs a first request through the connector so its client leaves an idle keep-alive connection in the pool, which
     * is the connection the request under test then reuses. Uses its own mocks so the class-level ones stay dedicated
     * to the request under test.
     */
    private void primePooledConnection() throws InterruptedException {
        final HttpExecutionContext primingCtx = mock(HttpExecutionContext.class);
        final HttpRequest primingRequest = mock(HttpRequest.class);
        final HttpResponse primingResponse = mock(HttpResponse.class);

        when(primingCtx.request()).thenReturn(primingRequest);
        when(primingCtx.response()).thenReturn(primingResponse);
        when(primingCtx.metrics()).thenReturn(mock(Metrics.class));
        when(primingCtx.getTracer()).thenReturn(new Tracer(null, new NoOpTracer()));
        when(primingCtx.getComponent(Vertx.class)).thenReturn(vertx);
        when(primingCtx.getComponent(Configuration.class)).thenReturn(mock(Configuration.class));
        when(primingRequest.method()).thenReturn(HttpMethod.GET);
        when(primingRequest.parameters()).thenReturn(new LinkedMultiValueMap<>());
        when(primingRequest.pathInfo()).thenReturn("");
        when(primingRequest.headers()).thenReturn(HttpHeaders.create());
        when(primingRequest.chunks()).thenReturn(Flowable.empty());
        when(primingResponse.headers()).thenReturn(HttpHeaders.create());

        final TestObserver<Void> obs = cut.connect(primingCtx).test();

        assertNoTimeout(obs);
        obs.assertComplete();
        drainChunks(primingResponse);
    }

    @SuppressWarnings("unchecked")
    private TestSubscriber<Buffer> drainChunks(final HttpResponse target) {
        final ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(target).chunks(chunksCaptor.capture());

        return chunksCaptor.getValue().test().awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS).assertComplete();
    }

    private void assertNoTimeout(TestObserver<Void> obs) throws InterruptedException {
        assertThat(obs.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("Should complete before timeout").isTrue();
    }
}
