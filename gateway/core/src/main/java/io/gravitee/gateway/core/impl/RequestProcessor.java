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
package io.gravitee.gateway.core.impl;

import io.gravitee.gateway.core.AsyncHandler;
import io.gravitee.gateway.core.Processor;
import io.gravitee.gateway.http.ContentRequest;
import io.gravitee.gateway.http.Request;
import io.gravitee.gateway.http.Response;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.*;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestProcessor.class);

    protected static final Set<String> HOP_HEADERS;
    static
    {
        Set<String> hopHeaders = new HashSet();
        hopHeaders.add("connection");
        hopHeaders.add("keep-alive");
        hopHeaders.add("proxy-authorization");
        hopHeaders.add("proxy-authenticate");
        hopHeaders.add("proxy-connection");
        hopHeaders.add("transfer-encoding");
        hopHeaders.add("te");
        hopHeaders.add("trailer");
        hopHeaders.add("upgrade");
        HOP_HEADERS = Collections.unmodifiableSet(hopHeaders);
    }

    private final Request request;

    private final AsyncHandler<Response> responseHandler;

    //TODO: do not create a new instance of client for each request !!!
    private static HttpClient client = createHttpClient();

    public RequestProcessor(final Request request, final AsyncHandler<Response> responseHandler) {
        this.request = request;
        this.responseHandler = responseHandler;
    }

    @Override
    public void process() {
        try {

            org.eclipse.jetty.client.api.Request proxyRequest = client.newRequest("http://www.thomas-bayer.com/sqlrest/CUSTOMER/").method(HttpMethod.GET).version(HttpVersion.HTTP_1_1);

            if (request.hasContent()) {
                proxyRequest.content(new ProxyInputStreamContentProvider(proxyRequest, (ContentRequest) request));
            }

            proxyRequest.send(new ProxyResponseListener(request, new Response()));
        } catch (Exception ex) {
            LOGGER.error("An error occurs while proxying request...", ex);
        }
    }

    protected class ProxyResponseListener extends org.eclipse.jetty.client.api.Response.Listener.Adapter
    {
        private final Request request;
        private final Response response;

        protected ProxyResponseListener(Request request, Response response)
        {
            this.request = request;
            this.response = response;
        }

        @Override
        public void onBegin(org.eclipse.jetty.client.api.Response proxyResponse)
        {
            response.setStatus(proxyResponse.getStatus());
        }

        @Override
        public void onHeaders(org.eclipse.jetty.client.api.Response proxyResponse)
        {
            onServerResponseHeaders(response, proxyResponse);
        }

        @Override
        public void onContent(final org.eclipse.jetty.client.api.Response proxyResponse, ByteBuffer content, final Callback callback)
        {
            byte[] buffer;
            int offset;
            int length = content.remaining();
            if (content.hasArray())
            {
                buffer = content.array();
                offset = content.arrayOffset();
            }
            else
            {
                buffer = new byte[length];
                content.get(buffer);
                offset = 0;
            }

            onResponseContent(request, response, proxyResponse, buffer, offset, length, new Callback()
            {
                @Override
                public void succeeded()
                {
                    callback.succeeded();
                }

                @Override
                public void failed(Throwable x)
                {
                    callback.failed(x);
                    proxyResponse.abort(x);
                }
            });
        }

        @Override
        public void onComplete(Result result)
        {
            if (result.isSucceeded()) {
                onProxyResponseSuccess(request, response, result.getResponse());
            } else {
                onProxyResponseFailure(request, response, result.getResponse(), result.getFailure());
            }

            LOGGER.debug("{} proxying complete", request.getId());
        }
    }

    protected void onProxyResponseSuccess(Request clientRequest, Response proxyResponse, org.eclipse.jetty.client.api.Response serverResponse)
    {
        LOGGER.debug("{} proxying successful", clientRequest.getId());
        responseHandler.handle(proxyResponse);
    }

    protected void onProxyResponseFailure(Request clientRequest, Response proxyResponse, org.eclipse.jetty.client.api.Response serverResponse, Throwable failure)
    {
        LOGGER.debug(clientRequest.getId() + " proxying failed", failure);

        if (failure instanceof TimeoutException)
            proxyResponse.setStatus(504);
        else
            proxyResponse.setStatus(502);

        proxyResponse.getHeaders().put(HttpHeader.CONNECTION.asString(), HttpHeaderValue.CLOSE.asString());

        responseHandler.handle(proxyResponse);
    }

    protected void onResponseContent(Request request, Response response, org.eclipse.jetty.client.api.Response proxyResponse, byte[] buffer, int offset, int length, Callback callback)
    {
        try
        {
            LOGGER.debug("{} proxying content to downstream: {} bytes", request.getId(), length);
            response.getOutputStream().write(buffer, offset, length);
            callback.succeeded();
        }
        catch (Throwable x)
        {
            callback.failed(x);
        }
    }

    protected void onServerResponseHeaders(Response proxyResponse, org.eclipse.jetty.client.api.Response serverResponse)
    {
        for (HttpField field : serverResponse.getHeaders())
        {
            String headerName = field.getName();
            String lowerHeaderName = headerName.toLowerCase(Locale.ENGLISH);
            if (HOP_HEADERS.contains(lowerHeaderName))
                continue;

            String newHeaderValue = field.getValue();
            if (newHeaderValue == null || newHeaderValue.trim().length() == 0)
                continue;

            proxyResponse.getHeaders().put(headerName, newHeaderValue);
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
            LOGGER.debug("{} proxying content to upstream: {} bytes", request.getId(), length);
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

    protected void onClientRequestFailure(org.eclipse.jetty.client.api.Request proxyRequest, Request request, Throwable failure) {
        LOGGER.debug(request.getId() + " client request failure", failure);
        proxyRequest.abort(failure);
    }

    protected static HttpClient createHttpClient()  {
        HttpClient client = new HttpClient();

        // Redirects must be proxied as is, not followed
        client.setFollowRedirects(false);

        // Must not store cookies, otherwise cookies of different clients will mix
        client.setCookieStore(new HttpCookieStore.Empty());

        // Be careful : max threads can't be less than 2 -> deadlock
        QueuedThreadPool qtp = new QueuedThreadPool(200);

        qtp.setName("dispatcher");

        client.setExecutor(qtp);
        client.setIdleTimeout(30000);
        client.setRequestBufferSize(16384);
        client.setResponseBufferSize(163840);

        try {
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Content must not be decoded, otherwise the client gets confused
        client.getContentDecoderFactories().clear();

        return client;
    }

}
