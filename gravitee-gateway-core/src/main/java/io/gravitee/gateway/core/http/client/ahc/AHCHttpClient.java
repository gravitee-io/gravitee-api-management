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
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.http.ServerResponse;
import io.gravitee.gateway.core.http.client.AbstractHttpClient;
import io.gravitee.gateway.core.model.Api;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;
import rx.Subscriber;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class AHCHttpClient extends AbstractHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AHCHttpClient.class);

    private AsyncHttpClient client;

    private AHCHttpConfiguration configuration;

    @Autowired
    public AHCHttpClient(Api api, AHCHttpConfiguration configuration) {
        super(api);
        this.configuration = configuration;
    }

    @Override
    public Observable<Response> invoke(final Request request, final Response response) {

        return Observable.create(
                new Observable.OnSubscribe<Response>() {
                    @Override
                    public void call(final Subscriber<? super Response> observer) {
                        AsyncHttpClient.BoundRequestBuilder builder = requestBuilder(request);

                        copyRequestHeaders(request, builder);

                        if (hasContent(request)) {
                            builder.setContentLength((int) request.contentLength());
                            InputStreamBodyGenerator bodyGenerator = new InputStreamBodyGenerator(
                                    request.inputStream());
                            builder.setBody(bodyGenerator);
                        }

                        com.ning.http.client.Request proxyRequest = builder.build();

                            client.executeRequest(proxyRequest, new AsyncCompletionHandler<Object>() {

                                @Override
                                public void onThrowable(Throwable t) {
                                    ((ServerResponse) response).setStatus(502);
                                }

                                @Override
                                public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
                                    content.writeTo(response.outputStream());
                                    return STATE.CONTINUE;
                                }

                                @Override
                                public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
                                    for (Map.Entry<String, List<String>> header : headers.getHeaders().entrySet()) {
                                        response.headers().put(header.getKey(), header.getValue().iterator().next());
                                    }

                                    return STATE.CONTINUE;
                                }

                                @Override
                                public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
                                    ((ServerResponse) response).setStatus(status.getStatusCode());
                                    return STATE.CONTINUE;
                                }

                                @Override
                                public Object onCompleted(com.ning.http.client.Response proxyResponse) throws Exception {
                                    observer.onNext(response);
                                    observer.onCompleted();
                                    return response;
                                }
                            });
                    }
                }
        );
    }

    private boolean hasContent(Request request) {
        return request.contentLength() > 0 ||
                request.contentType() != null ||
                // TODO: create an enum class for common HTTP headers
                request.headers().get("Transfer-Encoding") != null;
    }

    protected void copyRequestHeaders(Request clientRequest, AsyncHttpClient.BoundRequestBuilder requestBuilder)
    {
        for (Map.Entry<String, String> header : clientRequest.headers().entrySet()) {
            String headerName = header.getKey();
            String lowerHeaderName = headerName.toLowerCase(Locale.ENGLISH);

            // Remove hop-by-hop headers.
            if (HOP_HEADERS.contains(lowerHeaderName))
                continue;

            requestBuilder.addHeader(headerName, header.getValue());
        }

        requestBuilder.setHeader(HttpHeader.HOST.toString(), api.getTargetURI().getHost());
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

    private AsyncHttpClient construct(AHCHttpConfiguration configuration) {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();

        builder
                .setAllowPoolingConnections(configuration.isAllowPoolingConnections())
                .setRequestTimeout(configuration.getRequestTimeout())
                .setReadTimeout(configuration.getReadTimeout())
                .setConnectTimeout(configuration.getConnectTimeout())
                .setMaxConnectionsPerHost(configuration.getMaxConnectionsPerHost())
                .setMaxConnections(configuration.getMaxConnections())
                .build();

        return new AsyncHttpClient(builder.build());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        LOGGER.info("Initializing AsyncHttpClient for {} with configuration {}", configuration);
        client = construct(configuration);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        LOGGER.info("Close AsyncHttpClient for {}", api);
        client.close();
    }
}
