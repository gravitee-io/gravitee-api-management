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
package io.gravitee.gateway.core.http.client.ahc;

import com.ning.http.client.*;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.definition.ApiDefinition;
import io.gravitee.gateway.core.definition.HttpClientDefinition;
import io.gravitee.gateway.core.http.HttpServerResponse;
import io.gravitee.gateway.core.http.client.AbstractHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class AHCHttpClient extends AbstractHttpClient {

    private final Logger LOGGER = LoggerFactory.getLogger(AHCHttpClient.class);

    private AsyncHttpClient client;

    @Autowired
    public AHCHttpClient(ApiDefinition apiDefinition) {
        super(apiDefinition);
    }

    protected void copyRequestHeaders(Request clientRequest, AsyncHttpClient.BoundRequestBuilder requestBuilder) {
        for (Map.Entry<String, List<String>> headerValues : clientRequest.headers().entrySet()) {
            String headerName = headerValues.getKey();
            String lowerHeaderName = headerName.toLowerCase(Locale.ENGLISH);

            // Remove hop-by-hop headers.
            if (HOP_HEADERS.contains(lowerHeaderName))
                continue;

            headerValues.getValue().forEach(headerValue -> requestBuilder.addHeader(headerName, headerValue));
        }

        requestBuilder.setHeader(HttpHeaders.HOST, apiDefinition.getProxy().getTarget().getHost());
    }

    private AsyncHttpClient.BoundRequestBuilder requestBuilder(Request request) {
        URI rewrittenURI = rewriteURI(request);
        String url = rewrittenURI.toString();
        LOGGER.debug("{} rewriting: {} -> {}", request.id(), request.uri(), url);

        AsyncHttpClient.BoundRequestBuilder builder = null;
        switch (request.method()) {
            case CONNECT:
                builder = client.prepareConnect(url);
                break;
            case DELETE:
                builder = client.prepareDelete(url);
                break;
            case GET:
                builder = client.prepareGet(url);
                break;
            case HEAD:
                builder = client.prepareHead(url);
                break;
            case OPTIONS:
                builder = client.prepareOptions(url);
                break;
            case PATCH:
                builder = client.preparePatch(url);
                break;
            case POST:
                builder = client.preparePost(url);
                break;
            case PUT:
                builder = client.preparePut(url);
                break;
            case TRACE:
                builder = client.prepareTrace(url);
                break;
        }

        return builder;
    }

    private AsyncHttpClient construct(HttpClientDefinition httpClientDefinition) {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();

        // httpClientCodecMaxChunkSize is set to 8192 by default
        NettyAsyncHttpProviderConfig config = new NettyAsyncHttpProviderConfig();

        builder.setAsyncHttpClientProviderConfig(config);

        builder
                .setRequestTimeout(httpClientDefinition.getRequestTimeout())
                .setReadTimeout(httpClientDefinition.getReadTimeout())
                .setConnectTimeout(httpClientDefinition.getConnectTimeout())
                .setMaxConnectionsPerHost(httpClientDefinition.getMaxConnectionsPerHost())
                .setMaxConnections(httpClientDefinition.getMaxConnections())
                .setMaxRequestRetry(httpClientDefinition.getRequestRetry())
                .setPooledConnectionIdleTimeout(5000)
                .setAllowPoolingConnections(true);

        if (httpClientDefinition.isUseProxy()) {
            boolean useCredentials = (httpClientDefinition.getHttpProxy().getPrincipal() != null);

            ProxyServer proxy = null;

            if (useCredentials) {
                proxy = new ProxyServer(
                        httpClientDefinition.getHttpProxy().getHost(),
                        httpClientDefinition.getHttpProxy().getPort(),
                        httpClientDefinition.getHttpProxy().getPrincipal(),
                        httpClientDefinition.getHttpProxy().getPassword()
                );
            } else {
                proxy = new ProxyServer(
                        httpClientDefinition.getHttpProxy().getHost(),
                        httpClientDefinition.getHttpProxy().getPort()
                );
            }

            builder.setProxyServer(proxy);
        }

        return new AsyncHttpClient(builder.build());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        LOGGER.info("Initializing AsyncHttpClient with {}", apiDefinition.getProxy().getHttpClient());
        client = construct(apiDefinition.getProxy().getHttpClient());
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        LOGGER.info("Close AsyncHttpClient for {}", apiDefinition);
        client.close();
    }

    @Override
    public void invoke(Request request, Response response, Handler handler) {
        AsyncHttpClient.BoundRequestBuilder builder = requestBuilder(request);

        copyRequestHeaders(request, builder);

        if (hasContent(request)) {
            builder.setContentLength((int) request.headers().contentLength());
            RequestBodyGenerator bodyGenerator = new RequestBodyGenerator(request);
            builder.setBody(bodyGenerator);
        }

        com.ning.http.client.Request proxyRequest = builder.build();

        client.executeRequest(proxyRequest, new AsyncCompletionHandler<Object>() {

            @Override
            public void onThrowable(Throwable failure) {
                LOGGER.error(request.id() + " proxying failed", failure);

                if (failure instanceof TimeoutException) {
                    ((HttpServerResponse) response).setStatus(HttpStatusCode.GATEWAY_TIMEOUT_504);
                } else {
                    ((HttpServerResponse) response).setStatus(HttpStatusCode.BAD_GATEWAY_502);
                }

                response.headers().set(HttpHeaders.CONNECTION, "close");

                handler.handle(response);
            }

            @Override
            public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
                LOGGER.debug("{} proxying content to downstream: {} bytes", request.id(), content.length());

                content.writeTo(response.outputStream());
                return STATE.CONTINUE;
            }

            @Override
            public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                headers.getHeaders().forEach(header ->
                        response.headers().put(header.getKey(), header.getValue()));

                return STATE.CONTINUE;
            }

            @Override
            public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
                ((HttpServerResponse) response).setStatus(status.getStatusCode());
                return STATE.CONTINUE;
            }

            @Override
            public Object onCompleted(com.ning.http.client.Response proxyResponse) throws Exception {
                LOGGER.debug("{} proxying successful", request.id());

                handler.handle(response);
                return response;
            }

            @Override
            public STATE onContentWriteCompleted() {
                LOGGER.debug("{} proxying complete", request.id());
                return super.onContentWriteCompleted();
            }
        });
    }
}
