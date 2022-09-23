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
package io.gravitee.plugin.entrypoint.webhook;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.ConnectorFactoryHelper;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorConfiguration;
import io.reactivex.Completable;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import java.net.URL;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class WebhookEntrypointConnector extends EntrypointAsyncConnector {

    protected static final String INTERNAL_ATTR_WEBHOOK_REQUEST_URI = "webhook.requestUri";
    protected static final String INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT = "webhook.httpClient";
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.SUBSCRIBE);
    private static final String ENTRYPOINT_ID = "webhook";
    private static final char URI_QUERY_DELIMITER_CHAR = '?';
    protected final WebhookEntrypointConnectorConfiguration configuration;

    public WebhookEntrypointConnector(final WebhookEntrypointConnectorConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return ENTRYPOINT_ID;
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
        return ENTRYPOINT_ID.equalsIgnoreCase(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION_TYPE));
    }

    @Override
    public Completable handleRequest(final ExecutionContext ctx) {
        return prepareClientOptions(ctx);
    }

    @Override
    public Completable handleResponse(final ExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                final String requestUri = ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI);
                final HttpClient httpClient = ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);

                // Basically produces no response chunks since messages are consumed, sent to the remote webhook then discarded because subscription mode does not need producing content.
                ctx
                    .response()
                    .chunks(
                        ctx
                            .response()
                            .messages()
                            .flatMapCompletable(message -> sendAndDiscard(requestUri, httpClient, message))
                            .doFinally(httpClient::close)
                            .toFlowable()
                    );
            }
        );
    }

    private Completable prepareClientOptions(final ExecutionContext ctx) {
        return Completable.defer(
            () -> {
                try {
                    final Vertx vertx = ctx.getComponent(Vertx.class);
                    final Subscription subscription = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION);
                    final ConnectorFactoryHelper connectorFactoryHelper = ctx.getComponent(ConnectorFactoryHelper.class);
                    final WebhookEntrypointConnectorConfiguration configuration = connectorFactoryHelper.getConnectorConfiguration(
                        WebhookEntrypointConnectorConfiguration.class,
                        subscription.getConfiguration()
                    );

                    final HttpClientOptions options = new HttpClientOptions();
                    final String url = configuration.getCallbackUrl();
                    final URL target = new URL(null, url);
                    final String protocol = target.getProtocol();

                    if (protocol.charAt(protocol.length() - 1) == 's') {
                        options.setSsl(true).setUseAlpn(true);
                    }

                    options.setDefaultHost(target.getHost());

                    if (target.getPort() == -1) {
                        options.setDefaultPort(options.isSsl() ? 443 : 80);
                    } else {
                        options.setDefaultPort(target.getPort());
                    }

                    final String requestUri = (target.getQuery() == null)
                        ? target.getPath()
                        : target.getPath() + URI_QUERY_DELIMITER_CHAR + target.getQuery();
                    final HttpClient httpClient = vertx.createHttpClient(options);

                    ctx.setInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI, requestUri);
                    ctx.setInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT, httpClient);

                    return Completable.complete();
                } catch (Exception ex) {
                    return Completable.error(
                        new IllegalArgumentException(
                            "Unable to prepare the HTTP client for the webhook subscription url[" + configuration.getCallbackUrl() + "]",
                            ex
                        )
                    );
                }
            }
        );
    }

    private Completable sendAndDiscard(String requestUri, HttpClient httpClient, Message message) {
        // Consume the message in order to send it to the remote webhook and discard it to preserve memory.
        return httpClient
            .rxRequest(HttpMethod.POST, requestUri)
            .flatMap(
                request -> {
                    if (message.headers() != null) {
                        message.headers().forEach(header -> request.putHeader(header.getKey(), header.getValue()));
                    }
                    if (message.content() != null) {
                        return request.rxSend(Buffer.buffer(message.content().getBytes()));
                    } else {
                        return request.rxSend();
                    }
                }
            )
            .flatMapCompletable(response -> Completable.complete());
    }
}
