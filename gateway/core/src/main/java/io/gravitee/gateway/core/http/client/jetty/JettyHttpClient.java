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
package io.gravitee.gateway.core.http.client.jetty;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.http.client.AbstractHttpClient;
import io.gravitee.gateway.core.http.ContentRequest;
import io.gravitee.gateway.core.http.ServerRequest;
import io.gravitee.gateway.core.http.ServerResponse;
import io.gravitee.model.Api;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyHttpClient extends AbstractHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(JettyHttpClient.class);

    private final HttpClient client;

    public JettyHttpClient(final Api api) {
        super(api);
        this.client = construct();
    }

    @Override
    public Observable<Response> invoke(final Request request, final Response response) {
        return Observable.create(
                new Observable.OnSubscribe<Response>() {
                    @Override
                    public void call(final Subscriber<? super Response> observer) {
                        URI rewrittenURI = rewriteURI(request);
                        LOGGER.debug("{} rewriting: {} -> {}", request.id(), request.uri(), rewrittenURI);

                        org.eclipse.jetty.client.api.Request proxyRequest = client
                                .newRequest(rewrittenURI)
                                .method(request.method().name());

                        if (request.hasContent()) {
                            proxyRequest.content(new ProxyInputStreamContentProvider(proxyRequest, (ContentRequest) request));
                        }

                        proxyRequest.send(new ProxyResponseListener(request, (ServerResponse) response, observer));
                    }
                }
        );
    }

    protected URI rewriteURI(Request request) {
        String newTarget = rewriteTarget(request);
        return newTarget == null ? null : URI.create(newTarget);
    }

    protected class ProxyResponseListener extends org.eclipse.jetty.client.api.Response.Listener.Adapter {
        private final Request request;
        private final ServerResponse response;
        private final Subscriber<? super Response> observer;

        protected ProxyResponseListener(Request request, ServerResponse response, Subscriber<? super Response> observer) {
            this.request = request;
            this.response = response;
            this.observer = observer;
        }

        @Override
        public void onBegin(org.eclipse.jetty.client.api.Response proxyResponse) {
            response.setStatus(proxyResponse.getStatus());
        }

        @Override
        public void onHeaders(org.eclipse.jetty.client.api.Response proxyResponse) {
            onServerResponseHeaders(response, proxyResponse);
        }

        @Override
        public void onContent(final org.eclipse.jetty.client.api.Response proxyResponse, ByteBuffer content, final Callback callback) {
            byte[] buffer;
            int offset;
            int length = content.remaining();
            if (content.hasArray()) {
                buffer = content.array();
                offset = content.arrayOffset();
            } else {
                buffer = new byte[length];
                content.get(buffer);
                offset = 0;
            }

            onResponseContent(request, response, proxyResponse, buffer, offset, length, new Callback() {
                @Override
                public void succeeded() {
                    callback.succeeded();
                }

                @Override
                public void failed(Throwable x) {
                    callback.failed(x);
                    proxyResponse.abort(x);
                }
            });
        }

        @Override
        public void onComplete(Result result) {
            if (result.isSucceeded()) {
                onProxyResponseSuccess(request, response, result.getResponse(), observer);
            } else {
                onProxyResponseFailure(request, response, result.getResponse(), result.getFailure(), observer);
            }

            observer.onCompleted();
            LOGGER.debug("{} proxying complete", request.id());
        }
    }

    protected void onProxyResponseSuccess(Request clientRequest, Response proxyResponse, org.eclipse.jetty.client.api.Response serverResponse, Subscriber<? super Response> observer) {
        LOGGER.debug("{} proxying successful", clientRequest.id());
        observer.onNext(proxyResponse);
    }

    protected void onProxyResponseFailure(Request clientRequest, ServerResponse proxyResponse, org.eclipse.jetty.client.api.Response serverResponse, Throwable failure, Subscriber<? super Response> observer) {
        LOGGER.debug(clientRequest.id() + " proxying failed", failure);

        if (failure instanceof TimeoutException)
            proxyResponse.setStatus(504);
        else
            proxyResponse.setStatus(502);

        proxyResponse.headers().put(HttpHeader.CONNECTION.asString(), HttpHeaderValue.CLOSE.asString());

        observer.onNext(proxyResponse);
    }

    protected void onResponseContent(Request request, ServerResponse response, org.eclipse.jetty.client.api.Response proxyResponse, byte[] buffer, int offset, int length, Callback callback) {
        try {
            LOGGER.debug("{} proxying content to downstream: {} bytes", request.id(), length);
            response.getOutputStream().write(buffer, offset, length);
            callback.succeeded();
        } catch (final Exception x) {
            callback.failed(x);
        }
    }

    protected void onServerResponseHeaders(ServerResponse proxyResponse, org.eclipse.jetty.client.api.Response serverResponse) {
        for (HttpField field : serverResponse.getHeaders()) {
            String headerName = field.getName();
            String lowerHeaderName = headerName.toLowerCase(Locale.ENGLISH);
            if (HOP_HEADERS.contains(lowerHeaderName))
                continue;

            String newHeaderValue = field.getValue();
            if (newHeaderValue == null || newHeaderValue.trim().length() == 0)
                continue;

            proxyResponse.headers().put(headerName, newHeaderValue);
        }
    }

    protected class ProxyInputStreamContentProvider extends InputStreamContentProvider {
        private final org.eclipse.jetty.client.api.Request proxyRequest;
        private final ContentRequest request;

        protected ProxyInputStreamContentProvider(org.eclipse.jetty.client.api.Request proxyRequest, ContentRequest request) {
            super(request.getInputStream());
            this.proxyRequest = proxyRequest;
            this.request = request;
        }

        @Override
        public long getLength() {
            return request.getContentLength();
        }

        @Override
        protected ByteBuffer onRead(byte[] buffer, int offset, int length) {
            LOGGER.debug("{} proxying content to upstream: {} bytes", request.id(), length);
            return onRequestContent(proxyRequest, request, buffer, offset, length);
        }

        protected ByteBuffer onRequestContent(org.eclipse.jetty.client.api.Request proxyRequest, final ContentRequest request, byte[] buffer, int offset, int length) {
            return super.onRead(buffer, offset, length);
        }

        @Override
        protected void onReadFailure(Throwable failure) {
            onClientRequestFailure(proxyRequest, request, failure);
        }
    }

    protected void onClientRequestFailure(org.eclipse.jetty.client.api.Request proxyRequest, ServerRequest request, Throwable failure) {
        LOGGER.debug(request.id() + " client request failure", failure);
        proxyRequest.abort(failure);
    }

    // TODO: client should be highly configurable !
    private HttpClient construct() {
        HttpClient client = new HttpClient();

        // Redirects must be proxied as is, not followed
        client.setFollowRedirects(false);

        // Must not store cookies, otherwise cookies of different clients will mix
        client.setCookieStore(new HttpCookieStore.Empty());

        // Be careful : max threads can't be less than 2 -> deadlock
        QueuedThreadPool qtp = new QueuedThreadPool(200);

        qtp.setName("dispatcher" + api.getName());

        client.setExecutor(qtp);
        client.setIdleTimeout(30000);
        client.setRequestBufferSize(16384);
        client.setResponseBufferSize(163840);

        try {
            client.start();
        } catch (final Exception e) {
            LOGGER.error("An error occurred while trying to start the Jetty client", e);
        }

        // Content must not be decoded, otherwise the client gets confused
        client.getContentDecoderFactories().clear();

        return client;
    }
}
