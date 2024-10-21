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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.gravitee.gateway.api.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.gravitee.gateway.api.http.HttpHeaderNames.HOST;
import static io.gravitee.gateway.api.http.HttpHeaderNames.TRANSFER_ENCODING;
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_REQUEST_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.rxjava3.core.Vertx;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpConnectorTest {

    protected static final String REQUEST_BODY = "Post body content";
    protected static final String REQUEST_BODY_CHUNK1 = "Post ";
    protected static final String REQUEST_BODY_CHUNK2 = "body chunk ";
    protected static final String REQUEST_BODY_CHUNK3 = "content";

    protected static final int REQUEST_BODY_LENGTH = REQUEST_BODY.getBytes().length;
    protected static final String BACKEND_RESPONSE_BODY = "response from backend";
    public static final int TIMEOUT_SECONDS = 60;
    private static WireMockServer wiremock;
    private static Vertx vertx;

    @Mock
    private DeploymentContext deploymentCtx;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private Metrics metrics;

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

        requestHeaders = HttpHeaders.create();
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
    void shouldExecuteGetRequest() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());

        wiremock.stubFor(get("/team").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team")));
    }

    @Test
    void shouldExecuteGetRequestWithMergedQueryParameters() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo", "otherBar");
        parameters.add("other", "otherValue");

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.parameters()).thenReturn(parameters);
        when(request.chunks()).thenReturn(Flowable.empty());

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
    void shouldExecuteGetRequestWhenEndpointAttributeOverridenWithAbsoluteUrl() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("http://127.0.0.1:" + wiremock.port());

        wiremock.stubFor(get("/").willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/")).withHost(equalTo("127.0.0.1")));
    }

    @Test
    void shouldExecuteGetRequestWhenEndpointAttributeOverridenWithAbsoluteUrlAndQueryParameters() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo", "otherBar");
        parameters.add("other", "otherValue");

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.parameters()).thenReturn(parameters);
        when(request.chunks()).thenReturn(Flowable.empty());
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
    void shouldExecuteGetRequestWhenAttributeOverridenWithAbsoluteUrlAndPath() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());
        when(ctx.getAttribute(ATTR_REQUEST_ENDPOINT)).thenReturn("http://127.0.0.1:" + wiremock.port() + "/team/subPath");

        wiremock.stubFor(get(urlPathEqualTo("/team/subPath")).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();

        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(1, getRequestedFor(urlPathEqualTo("/team/subPath")).withHost(equalTo("127.0.0.1")));
    }

    @Test
    void shouldExecuteGetRequestWhenAttributeOverridenWithAbsoluteUrlAndPathAndQueryParameters() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo", "otherBar");
        parameters.add("other", "otherValue");

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.parameters()).thenReturn(parameters);
        when(request.chunks()).thenReturn(Flowable.empty());
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
    void shouldExecuteGetRequestWhenAttributeOverridenWithRelativeUrlAndPathAndQueryParameters() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo", "otherBar");
        parameters.add("other", "otherValue");

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.parameters()).thenReturn(parameters);
        when(request.chunks()).thenReturn(Flowable.empty());
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
    void shouldExecutePostRequest() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.POST);
        when(request.chunks())
            .thenReturn(
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
    void shouldExecutePostRequestChunked() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.POST);
        when(request.chunks()).thenReturn(Flowable.just(Buffer.buffer(REQUEST_BODY)));
        requestHeaders.set(CONTENT_LENGTH, "" + REQUEST_BODY_LENGTH);

        wiremock.stubFor(post("/team").withRequestBody(new EqualToPattern(REQUEST_BODY)).willReturn(ok(BACKEND_RESPONSE_BODY)));

        final TestObserver<Void> obs = cut.connect(ctx).test();
        assertNoTimeout(obs);
        obs.assertComplete();

        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo("/team"))
                .withHeader(CONTENT_LENGTH, new EqualToPattern("" + REQUEST_BODY_LENGTH))
                .withRequestBody(new EqualToPattern(REQUEST_BODY))
        );
    }

    @Test
    void shouldPropagateRequestHeadersAndRemoveHopHeaders() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());

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
    void shouldAddOrReplaceRequestHeadersWithConfiguration() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());
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
    void shouldOverrideHostWithRequestHostHeader() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());
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
    void shouldNotOverrideRequestHostHeaderWhenSameAsRequestOriginalHost() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());
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
    void shouldPropagateRequestVertxHttpHeaderWithoutTemporaryCopy() throws InterruptedException {
        requestHeaders = new VertxHttpHeaders(new HeadersMultiMap());
        when(request.headers()).thenReturn(requestHeaders);
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());

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
    void shouldPropagateResponseHeaders() throws InterruptedException {
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());

        wiremock.stubFor(
            get("/team")
                .willReturn(
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
    void shouldPropagateResponseHeadersWhenVertxResponseHeader() throws InterruptedException {
        responseHeaders = new VertxHttpHeaders(new HeadersMultiMap());

        when(response.headers()).thenReturn(responseHeaders);
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());

        wiremock.stubFor(
            get("/team")
                .willReturn(
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
    void shouldThrowIllegalArgumentExceptionWithNullTarget() {
        configuration.setTarget(null);

        assertThrows(
            IllegalArgumentException.class,
            () -> cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory())
        );
    }

    @Test
    void shouldExecuteRequestWithQueryParameters() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo1", "bar1");
        parameters.add("foo2", "bar2");

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());
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
    void shouldExecuteRequestWithQueryParametersMergedWithTargetQueryParams() throws InterruptedException {
        final LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("foo1", "bar1");
        parameters.add("foo2", "bar2");
        parameters.add("foo3", null);

        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.chunks()).thenReturn(Flowable.empty());
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
    void shouldErrorWhenExceptionIsThrown() {
        configuration.setTarget("http://localhost:" + wiremock.port() + "/team");

        cut = new HttpConnector(configuration, sharedConfiguration, new HttpClientFactory());

        final TestObserver<Void> obs = cut.connect(ctx).test();
        obs.assertError(NullPointerException.class);
    }

    private void assertNoTimeout(TestObserver<Void> obs) throws InterruptedException {
        assertThat(obs.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue().as("Should complete before timeout");
    }
}
