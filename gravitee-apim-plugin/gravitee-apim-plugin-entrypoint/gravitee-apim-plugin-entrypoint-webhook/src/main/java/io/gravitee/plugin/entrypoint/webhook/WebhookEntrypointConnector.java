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

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE;
import static io.vertx.core.http.HttpMethod.POST;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointConnectorSubscriptionConfiguration;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.exception.PluginConfigurationException;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosCapability;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorConfiguration;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorSubscriptionConfiguration;
import io.gravitee.plugin.entrypoint.webhook.exception.GoAwayException;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.RequestOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class WebhookEntrypointConnector extends EntrypointAsyncConnector {

    public static final String ENTRYPOINT_ID = "webhook";
    public static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.SUBSCRIBE);
    public static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NONE, Qos.AUTO, Qos.AT_MOST_ONCE, Qos.AT_LEAST_ONCE);
    public static final String INTERNAL_ATTR_WEBHOOK_REQUEST_URI = "webhook.requestUri";
    public static final String INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT = "webhook.httpClient";
    public static final String INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG = "webhook.subscriptionConfiguration";
    public static final String STOPPING_MESSAGE = "Stopping, please reconnect";
    public static final String WEBHOOK_UNREACHABLE_KEY = "WEBHOOK_UNREACHABLE";
    public static final String WEBHOOK_UNREACHABLE_MESSAGE = "Webhook callback url is unreachable";
    public static final String MESSAGE_PROCESSING_FAILED_KEY = "MESSAGE_PROCESSING_FAILED";
    public static final String MESSAGE_PROCESSING_FAILED_MESSAGE = "Error occurred during message processing";
    private static final char URI_QUERY_DELIMITER_CHAR = '?';
    protected final WebhookEntrypointConnectorConfiguration configuration;
    protected final ConnectorHelper connectorHelper;
    protected QosRequirement qosRequirement;

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
        return id().equalsIgnoreCase(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION_TYPE));
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
        return Completable.fromRunnable(
            () -> {
                final HttpClient httpClient = ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);

                // Basically produces no response chunks since messages are consumed, sent to the remote webhook then discarded because subscription mode does not need producing content.
                ctx
                    .response()
                    .chunks(
                        ctx
                            .response()
                            .messages()
                            .compose(applyStopHook())
                            .flatMapSingle(message -> send(ctx, httpClient, message))
                            .compose(upstream -> acknowledge(ctx, upstream))
                            .ignoreElements()
                            .onErrorComplete(throwable -> throwable instanceof GoAwayException)
                            .doFinally(httpClient::close)
                            .toFlowable()
                    );
            }
        );
    }

    protected @NonNull Flowable<Message> acknowledge(final ExecutionContext ctx, final Flowable<Message> upstream) {
        return upstream.flatMapMaybe(
            message -> {
                if (message.error()) {
                    final ExecutionFailure executionFailure = ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE);

                    if (executionFailure != null) {
                        return ctx.interruptMessageWith(executionFailure);
                    }

                    return ctx.interruptMessageWith(
                        new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                            .key(MESSAGE_PROCESSING_FAILED_KEY)
                            .message(MESSAGE_PROCESSING_FAILED_MESSAGE)
                    );
                }

                message.ack();

                return Maybe.just(message);
            }
        );
    }

    @Override
    public WebhookEntrypointConnector preStop() {
        emitStopMessage();
        return this;
    }

    protected Single<Message> send(ExecutionContext ctx, HttpClient httpClient, Message message) {
        if (Objects.equals(STOP_MESSAGE_ID, message.id())) {
            return Single.error(new GoAwayException());
        }

        if (message.error()) {
            return ctx
                .interruptMessageWith(new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500).message(message.content().toString()))
                .toSingle();
        }

        final WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration = ctx.getInternalAttribute(
            INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG
        );
        final HttpHeaders responseHeaders = ctx.response().headers();
        final RequestOptions options = buildRequestOptions(ctx, subscriptionConfiguration);

        // Consume the message in order to send it to the remote webhook and discard it to preserve memory.
        return httpClient
            .rxRequest(options)
            .flatMap(
                request -> {
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
                }
            )
            .flatMap(
                httpClientResponse -> {
                    final int statusCode = httpClientResponse.statusCode();
                    if (statusCode >= 200 && statusCode <= 299) {
                        return Single.just(message);
                    } else if (statusCode >= 500 && statusCode <= 599) {
                        // Interrupt in case of server error.
                        return ctx
                            .interruptMessageWith(
                                new ExecutionFailure(statusCode).key(WEBHOOK_UNREACHABLE_KEY).message(WEBHOOK_UNREACHABLE_MESSAGE)
                            )
                            .toSingle();
                    } else {
                        // Any other http status is considered as a functional error.
                        message.error(true);
                        return Single.just(message);
                    }
                }
            );
    }

    private void putHeaderIfAbsent(HttpClientRequest request, String headerName, String headerValue) {
        if (!request.headers().contains(headerName)) {
            request.putHeader(headerName, headerValue);
        }
    }

    private Completable computeSubscriptionContextAttributes(final ExecutionContext ctx) {
        return Completable.defer(
            () -> {
                final Subscription subscription = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION);
                try {
                    final WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration = readSubscriptionConfiguration(
                        subscription
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
            }
        );
    }

    protected WebhookEntrypointConnectorSubscriptionConfiguration readSubscriptionConfiguration(Subscription subscription)
        throws PluginConfigurationException {
        return connectorHelper.readConfiguration(
            WebhookEntrypointConnectorSubscriptionConfiguration.class,
            subscription.getConfiguration()
        );
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

    protected RequestOptions buildRequestOptions(
        ExecutionContext ctx,
        WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration
    ) {
        return new RequestOptions().setURI(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI)).setMethod(POST);
    }
}
