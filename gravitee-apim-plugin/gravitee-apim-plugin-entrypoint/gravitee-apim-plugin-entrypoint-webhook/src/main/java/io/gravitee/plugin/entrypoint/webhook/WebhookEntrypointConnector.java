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
package io.gravitee.plugin.entrypoint.webhook;

import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ContextAttributes;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorConfiguration;
import io.reactivex.*;
import io.reactivex.functions.Function;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class WebhookEntrypointConnector implements EntrypointAsyncConnector {

    static final ApiType SUPPORTED_API = ApiType.ASYNC;
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH);

    private static final String TYPE = "webhook";

    protected final WebhookEntrypointConnectorConfiguration configuration;

    public WebhookEntrypointConnector(final WebhookEntrypointConnectorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ApiType supportedApi() {
        return SUPPORTED_API;
    }

    @Override
    public ListenerType supportedListenerType() {
        return ListenerType.SUBSCRIPTION;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public int matchCriteriaCount() {
        return 0;
    }

    @Override
    public boolean matches(final ExecutionContext ctx) {
        // The context should contain a "subscription_type" internal attribute with the "webhook" value
        return TYPE.equalsIgnoreCase(ctx.getInternalAttribute(ContextAttributes.ATTR_SUBSCRIPTION_TYPE));
    }

    private HttpClient client;
    private String requestUri;

    @Override
    public Completable handleRequest(final ExecutionContext ctx) {
        io.vertx.reactivex.core.Vertx vertx = io.vertx.reactivex.core.Vertx.newInstance(ctx.getComponent(Vertx.class));

        try {
            this.client = vertx.createHttpClient(prepareClientOptions());

            return Completable.complete();
        } catch (Exception ex) {
            log.error("Unable to prepare the HTTP client for the webhook subscription url[{}]", configuration.getCallbackUrl());
            return Completable.error(ex);
        }
    }

    private HttpClientOptions prepareClientOptions() {
        HttpClientOptions options = new HttpClientOptions();

        String url = configuration.getCallbackUrl();

        URL target;
        try {
            target = new URL(null, url);
        } catch (MalformedURLException e) {
            throw new IllegalStateException();
        }

        final String protocol = target.getProtocol();

        if (protocol.charAt(protocol.length() - 1) == 's') {
            // Configure SSL
            options.setSsl(true).setUseAlpn(true);
        }

        options.setDefaultHost(target.getHost());

        if (target.getPort() == -1) {
            options.setDefaultPort(options.isSsl() ? 443 : 80);
        } else {
            options.setDefaultPort(target.getPort());
        }

        requestUri = (target.getQuery() == null) ? target.getPath() : target.getPath() + URI_QUERY_DELIMITER_CHAR + target.getQuery();

        return options;
    }

    private static final char URI_QUERY_DELIMITER_CHAR = '?';

    @Override
    public Completable handleResponse(final ExecutionContext ctx) {
        return Completable.defer(() ->
            ctx
                .response()
                .messages()
                .flatMapSingle(
                    (Function<Message, SingleSource<?>>) message -> {
                        // HTTP headers
                        return client
                            .rxRequest(HttpMethod.POST, requestUri)
                            .flatMap(request -> {
                                if (message.headers() != null) {
                                    message.headers().forEach(header -> request.putHeader(header.getKey(), header.getValue()));
                                }
                                if (message.content() != null) {
                                    return request.rxSend(Buffer.buffer(message.content().getBytes()));
                                } else {
                                    return request.rxSend();
                                }
                            });
                    }
                )
                .onErrorResumeNext(error -> {
                    log.error("Error when dealing with response messages", error);

                    return subscriber -> {};
                })
                .ignoreElements()
                .andThen(client.rxClose())
                .andThen(ctx.response().end())
        );
    }
}
