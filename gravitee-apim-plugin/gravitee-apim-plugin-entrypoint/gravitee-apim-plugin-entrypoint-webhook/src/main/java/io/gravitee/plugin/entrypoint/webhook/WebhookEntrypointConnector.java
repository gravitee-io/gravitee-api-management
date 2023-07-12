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

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosCapability;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorConfiguration;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorSubscriptionConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.net.URL;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class WebhookEntrypointConnector extends EntrypointAsyncConnector {

    public static final String ENTRYPOINT_ID = "webhook";
    protected static final String INTERNAL_ATTR_WEBHOOK_REQUEST_URI = "webhook.requestUri";
    protected static final String INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT = "webhook.httpClient";
    protected static final String INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG = "webhook.subscriptionConfiguration";
    protected static final String STOPPING_MESSAGE = "Stopping, please reconnect";
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.SUBSCRIBE);
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NONE, Qos.AUTO, Qos.AT_MOST_ONCE, Qos.AT_LEAST_ONCE);
    private static final char URI_QUERY_DELIMITER_CHAR = '?';
    protected final WebhookEntrypointConnectorConfiguration configuration;
    private final ConnectorHelper connectorHelper;
    private QosRequirement qosRequirement;

    public WebhookEntrypointConnector(
        final ConnectorHelper connectorHelper,
        final Qos qos,
        final WebhookEntrypointConnectorConfiguration configuration
    ) {
        this.connectorHelper = connectorHelper;
        computeQosRequirement(qos);
        this.configuration = configuration;
    }

    protected void computeQosRequirement(final Qos qos) {
        QosRequirement.QosRequirementBuilder qosRequirementBuilder = QosRequirement.builder().qos(qos);
        if (qos == Qos.AT_MOST_ONCE || qos == Qos.AT_LEAST_ONCE) {
            qosRequirementBuilder.capabilities(Set.of(QosCapability.MANUAL_ACK)).build();
        }
        this.qosRequirement = qosRequirementBuilder.build();
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
    public QosRequirement qosRequirement() {
        return qosRequirement;
    }

    @Override
    public Completable handleRequest(final ExecutionContext ctx) {
        return computeSubscriptionContextAttributes(ctx);
    }

    @Override
    public Completable handleResponse(final ExecutionContext ctx) {
        return Completable.fromRunnable(() -> {
            final String requestUri = ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI);
            final HttpClient httpClient = ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);
            final WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration = ctx.getInternalAttribute(
                INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG
            );

            // Basically produces no response chunks since messages are consumed, sent to the remote webhook then discarded because subscription mode does not need producing content.
            ctx
                .response()
                .chunks(
                    ctx
                        .response()
                        .messages()
                        .compose(applyStopHook())
                        .flatMapCompletable(message -> {
                            if (message.error()) {
                                return Completable.error(new Exception(message.content().toString()));
                            }
                            return sendAndDiscard(requestUri, httpClient, subscriptionConfiguration, message, ctx.response().headers());
                        })
                        .doFinally(httpClient::close)
                        .toFlowable()
                );
        });
    }

    @Override
    public WebhookEntrypointConnector preStop() {
        emitStopMessage();
        return this;
    }

    private Completable sendAndDiscard(
        String requestUri,
        HttpClient httpClient,
        WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration,
        Message message,
        HttpHeaders responseHeaders
    ) {
        // Consume the message in order to send it to the remote webhook and discard it to preserve memory.
        return httpClient
            .rxRequest(HttpMethod.POST, requestUri)
            .flatMap(request -> {
                if (responseHeaders != null) {
                    responseHeaders.forEach(header -> request.putHeader(header.getKey(), header.getValue()));
                }
                if (message.headers() != null) {
                    message.headers().forEach(header -> putHeaderIfAbsent(request, header.getKey(), header.getValue()));
                }
                if (subscriptionConfiguration.getHeaders() != null) {
                    subscriptionConfiguration
                        .getHeaders()
                        .forEach(header -> putHeaderIfAbsent(request, header.getName(), header.getValue()));
                }
                if (message.content() != null) {
                    return request.rxSend(Buffer.buffer(message.content().getNativeBuffer()));
                } else {
                    return request.rxSend();
                }
            })
            .doOnSuccess(httpClientResponse -> message.ack())
            .ignoreElement()
            .doOnError(throwable -> log.error("An error occurred when trying to send webhook message.", throwable))
            .onErrorComplete();
    }

    private void putHeaderIfAbsent(HttpClientRequest request, String headerName, String headerValue) {
        if (!request.headers().contains(headerName)) {
            request.putHeader(headerName, headerValue);
        }
    }

    private Completable computeSubscriptionContextAttributes(final ExecutionContext ctx) {
        return Completable.defer(() -> {
            WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration = null;

            final Subscription subscription = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION);
            try {
                subscriptionConfiguration =
                    connectorHelper.readConfiguration(
                        WebhookEntrypointConnectorSubscriptionConfiguration.class,
                        subscription.getConfiguration()
                    );

                final URL targetUrl = new URL(null, subscriptionConfiguration.getCallbackUrl());

                ctx.setInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG, subscriptionConfiguration);
                ctx.setInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI, extractRequestUri(targetUrl));
                ctx.setInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT, createHttpClient(ctx, targetUrl));

                return Completable.complete();
            } catch (Exception ex) {
                return Completable.error(
                    new IllegalArgumentException(
                        "Unable to prepare the execution context for the webhook, with subscription id [" + subscription.getId() + "]",
                        ex
                    )
                );
            }
        });
    }

    private String extractRequestUri(URL targetUrl) {
        return targetUrl.getQuery() == null ? targetUrl.getPath() : targetUrl.getPath() + URI_QUERY_DELIMITER_CHAR + targetUrl.getQuery();
    }

    private HttpClient createHttpClient(ExecutionContext ctx, URL targetUrl) {
        final HttpClientOptions options = new HttpClientOptions();
        String protocol = targetUrl.getProtocol();
        if (protocol.charAt(protocol.length() - 1) == 's') {
            options.setSsl(true).setUseAlpn(true);
        }
        options.setDefaultHost(targetUrl.getHost());
        if (targetUrl.getPort() == -1) {
            options.setDefaultPort(options.isSsl() ? 443 : 80);
        } else {
            options.setDefaultPort(targetUrl.getPort());
        }
        return ctx.getComponent(Vertx.class).createHttpClient(options);
    }
}
