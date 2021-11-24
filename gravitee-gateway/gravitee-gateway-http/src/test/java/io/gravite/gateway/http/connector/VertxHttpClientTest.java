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
package io.gravite.gateway.http.connector;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.builder.ProxyRequestBuilder;
import io.gravitee.gateway.http.connector.http.HttpConnector;
import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.WriteStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VertxHttpClientTest {

    Vertx vertx = Vertx.vertx();

    @Mock
    Request request;

    @Mock
    HttpEndpoint endpoint;

    @Mock
    HttpClient httpClient;

    HttpConnector vertxHttpClient = new HttpConnector(null);

    @Before
    public void init() {
        ReflectionTestUtils.setField(vertxHttpClient, "vertx", vertx);
        ReflectionTestUtils.setField(vertxHttpClient, "endpoint", endpoint);
        Map<Context, HttpClient> httpClients = new HashMap<>();
        httpClients.put(Vertx.currentContext(), httpClient);
        ReflectionTestUtils.setField(vertxHttpClient, "httpClients", httpClients);
        when(httpClient.request(eq(io.vertx.core.http.HttpMethod.GET), eq(80), anyString(), anyString())).thenReturn(new MockedHttpClientRequest());
        Metrics metrics = Metrics.on((new Date()).getTime()).build();
        when(request.metrics()).thenReturn(metrics);
    }

    @Test
    public void testUnencodedWithoutQuery() throws Exception {
        HttpClientOptions httpOptions = mock(HttpClientOptions.class);
        when(endpoint.getHttpClientOptions()).thenReturn(httpOptions);
        ProxyRequest proxyRequest = ProxyRequestBuilder.from(request)
                .method(HttpMethod.GET)
                .uri("http://gravitee.io/test")
                .headers(new HttpHeaders())
                .build();

        vertxHttpClient.request(proxyRequest);

        assertEquals("http://gravitee.io/test", request.metrics().getEndpoint());
    }

    @Test
    public void testUnencodedWithQueryUnencoded() throws Exception {
        HttpClientOptions httpOptions = mock(HttpClientOptions.class);
        when(endpoint.getHttpClientOptions()).thenReturn(httpOptions);
        ProxyRequest proxyRequest = ProxyRequestBuilder.from(request)
                .method(HttpMethod.GET)
                .uri("http://gravitee.io/test?foo=bar")
                .headers(new HttpHeaders())
                .build();

        vertxHttpClient.request(proxyRequest);

        assertEquals("http://gravitee.io/test?foo=bar", request.metrics().getEndpoint());
    }

    @Test
    public void testUnencodedWithQueryUnencoded2() throws Exception {
        HttpClientOptions httpOptions = mock(HttpClientOptions.class);
        when(endpoint.getHttpClientOptions()).thenReturn(httpOptions);
        ProxyRequest proxyRequest = ProxyRequestBuilder.from(request)
                .method(HttpMethod.GET)
                .uri("http://gravitee.io/test?foo==bar")
                .headers(new HttpHeaders())
                .build();

        vertxHttpClient.request(proxyRequest);

        assertEquals("http://gravitee.io/test?foo==bar", request.metrics().getEndpoint());
    }

    @Test
    public void testUnencodedWithQueryEncoded() throws Exception {
        HttpClientOptions httpOptions = mock(HttpClientOptions.class);
        when(endpoint.getHttpClientOptions()).thenReturn(httpOptions);
        ProxyRequest proxyRequest = ProxyRequestBuilder.from(request)
                .method(HttpMethod.GET)
                .uri("http://gravitee.io/test?foo=%3Dbar")
                .headers(new HttpHeaders())
                .build();

        vertxHttpClient.request(proxyRequest);

        assertEquals("http://gravitee.io/test?foo=%3Dbar", request.metrics().getEndpoint());
    }

    @Test
    public void testUnencodedWithEmptyQueryParam() throws Exception {
        HttpClientOptions httpOptions = mock(HttpClientOptions.class);
        when(endpoint.getHttpClientOptions()).thenReturn(httpOptions);
        ProxyRequest proxyRequest = ProxyRequestBuilder.from(request)
                .method(HttpMethod.GET)
                .uri("http://gravitee.io/test?foo=&bar=")
                .headers(new HttpHeaders())
                .build();

        vertxHttpClient.request(proxyRequest);

        assertEquals("http://gravitee.io/test?foo=&bar=", request.metrics().getEndpoint());
    }

    @Test
    public void testUnencodedWithNullQueryParam() throws Exception {
        HttpClientOptions httpOptions = mock(HttpClientOptions.class);
        when(endpoint.getHttpClientOptions()).thenReturn(httpOptions);
        ProxyRequest proxyRequest = ProxyRequestBuilder.from(request)
                .method(HttpMethod.GET)
                .uri("http://gravitee.io/test?foo&bar")
                .headers(new HttpHeaders())
                .build();

        vertxHttpClient.request(proxyRequest);

        assertEquals("http://gravitee.io/test?foo&bar", request.metrics().getEndpoint());
    }

    class MockedHttpClientRequest implements HttpClientRequest{
        @Override
        public HttpClientRequest exceptionHandler(Handler<Throwable> handler) {
            return null;
        }

        @Override
        public HttpClientRequest write(Buffer data) {
            return null;
        }

        @Override
        public HttpClientRequest write(Buffer buffer, Handler<AsyncResult<Void>> handler) {
            return null;
        }

        @Override
        public HttpClientRequest setWriteQueueMaxSize(int maxSize) {
            return null;
        }

        @Override
        public boolean writeQueueFull() {
            return false;
        }

        @Override
        public HttpClientRequest drainHandler(Handler<Void> handler) {
            return null;
        }

        @Override
        public HttpClientRequest handler(Handler<HttpClientResponse> handler) {
            return null;
        }

        @Override
        public HttpClientRequest pause() {
            return null;
        }

        @Override
        public HttpClientRequest resume() {
            return null;
        }

        @Override
        public HttpClientRequest fetch(long amount) {
            return null;
        }

        @Override
        public HttpClientRequest endHandler(Handler<Void> endHandler) {
            return null;
        }

        @Override
        public Pipe<HttpClientResponse> pipe() {
            return null;
        }

        @Override
        public void pipeTo(WriteStream<HttpClientResponse> dst) {

        }

        @Override
        public void pipeTo(WriteStream<HttpClientResponse> dst, Handler<AsyncResult<Void>> handler) {

        }

        @Override
        public HttpClientRequest setFollowRedirects(boolean followRedirects) {
            return null;
        }

        @Override
        public HttpClientRequest setMaxRedirects(int maxRedirects) {
            return null;
        }

        @Override
        public HttpClientRequest setChunked(boolean chunked) {
            return null;
        }

        @Override
        public boolean isChunked() {
            return false;
        }

        @Override
        public io.vertx.core.http.HttpMethod method() {
            return null;
        }

        @Override
        public String getRawMethod() {
            return null;
        }

        @Override
        public HttpClientRequest setRawMethod(String method) {
            return null;
        }

        @Override
        public String absoluteURI() {
            return null;
        }

        @Override
        public String uri() {
            return null;
        }

        @Override
        public String path() {
            return null;
        }

        @Override
        public String query() {
            return null;
        }

        @Override
        public HttpClientRequest setHost(String host) {
            return null;
        }

        @Override
        public String getHost() {
            return null;
        }

        @Override
        public MultiMap headers() {
            return null;
        }

        @Override
        public HttpClientRequest putHeader(String name, String value) {
            return null;
        }

        @Override
        public HttpClientRequest putHeader(CharSequence name, CharSequence value) {
            return null;
        }

        @Override
        public HttpClientRequest putHeader(String name, Iterable<String> values) {
            return null;
        }

        @Override
        public HttpClientRequest putHeader(CharSequence name, Iterable<CharSequence> values) {
            return null;
        }

        @Override
        public HttpClientRequest write(String chunk) {
            return null;
        }

        @Override
        public HttpClientRequest write(String s, Handler<AsyncResult<Void>> handler) {
            return null;
        }

        @Override
        public HttpClientRequest write(String chunk, String enc) {
            return null;
        }

        @Override
        public HttpClientRequest write(String s, String s1, Handler<AsyncResult<Void>> handler) {
            return null;
        }

        @Override
        public HttpClientRequest continueHandler(Handler<Void> handler) {
            return null;
        }

        @Override
        public HttpClientRequest sendHead() {
            return null;
        }

        @Override
        public HttpClientRequest sendHead(Handler<HttpVersion> completionHandler) {
            return null;
        }

        @Override
        public void end(String chunk) {

        }

        @Override
        public void end(String s, Handler<AsyncResult<Void>> handler) {

        }

        @Override
        public void end(String chunk, String enc) {

        }

        @Override
        public void end(String s, String s1, Handler<AsyncResult<Void>> handler) {

        }

        @Override
        public void end(Buffer chunk) {

        }

        @Override
        public void end(Buffer buffer, Handler<AsyncResult<Void>> handler) {

        }

        @Override
        public void end() {

        }

        @Override
        public void end(Handler<AsyncResult<Void>> handler) {

        }

        @Override
        public HttpClientRequest setTimeout(long timeoutMs) {
            return null;
        }

        @Override
        public HttpClientRequest pushHandler(Handler<HttpClientRequest> handler) {
            return null;
        }

        @Override
        public boolean reset() {
            return false;
        }

        @Override
        public boolean reset(long code) {
            return false;
        }

        @Override
        public HttpConnection connection() {
            return null;
        }

        @Override
        public HttpClientRequest connectionHandler(Handler<HttpConnection> handler) {
            return null;
        }

        @Override
        public HttpClientRequest writeCustomFrame(int type, int flags, Buffer payload) {
            return null;
        }

        @Override
        public int streamId() {
            return 0;
        }

        @Override
        public HttpClientRequest writeCustomFrame(HttpFrame frame) {
            return null;
        }

        @Override
        public HttpClientRequest setStreamPriority(StreamPriority streamPriority) {
            return null;
        }

        @Override
        public StreamPriority getStreamPriority() {
            return null;
        }
    }

}
