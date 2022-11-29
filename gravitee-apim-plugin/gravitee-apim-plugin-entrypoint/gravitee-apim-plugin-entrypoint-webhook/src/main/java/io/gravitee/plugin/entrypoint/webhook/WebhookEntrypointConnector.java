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

import static io.vertx.core.http.HttpMethod.POST;

import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.exception.PluginConfigurationException;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosCapability;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.jupiter.http.vertx.client.VertxHttpClient;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorConfiguration;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorSubscriptionConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.http.RequestOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
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
    protected static final String WEBHOOK_401_ERROR_KEY = "webhook.401";
    protected static final String STOPPING_MESSAGE = "Stopping, please reconnect";
    public static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.SUBSCRIBE);
    public static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.NONE, Qos.AUTO, Qos.AT_MOST_ONCE, Qos.AT_LEAST_ONCE);
    protected final WebhookEntrypointConnectorConfiguration configuration;
    protected final ConnectorHelper connectorHelper;
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
                            .flatMapCompletable(
                                message -> {
                                    if (message.error()) {
                                        return Completable.error(new Exception(message.content().toString()));
                                    }
                                    RequestOptions requestOptions = buildRequestOptions(ctx, message);
                                    return sendAndDiscard(ctx, httpClient, message, requestOptions);
                                }
                            )
                            .doFinally(httpClient::close)
                            .retry(1, this::is401error)
                            .doOnError(t -> log.error("Error publishing to webhook", t))
                            .toFlowable()
                    );
            }
        );
    }

    @Override
    public WebhookEntrypointConnector preStop() {
        emitStopMessage();
        return this;
    }

    protected Completable sendAndDiscard(ExecutionContext ctx, HttpClient httpClient, Message message, RequestOptions requestOptions) {
        // Consume the message in order to send it to the remote webhook and discard it to preserve memory.
        return httpClient
            .rxRequest(requestOptions)
            .flatMap(
                request -> {
                    if (message.content() != null) {
                        return request.rxSend(Buffer.buffer(message.content().getNativeBuffer()));
                    } else {
                        return request.rxSend();
                    }
                }
            )
            .flatMapCompletable(httpResponse -> handleWebhookResponse(ctx, httpResponse, message));
    }

    protected Completable handleWebhookResponse(ExecutionContext ctx, HttpClientResponse httpResponse, Message message) {
        return Completable.fromRunnable(() -> message.ack());
    }

    private Completable computeSubscriptionContextAttributes(final ExecutionContext ctx) {
        return Completable.defer(
            () -> {
                WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration = null;

                final Subscription subscription = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION);
                try {
                    subscriptionConfiguration = readSubscriptionConfiguration(subscription);
                    computeSubscriptionContextAttributes(ctx, subscriptionConfiguration);
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

    protected void computeSubscriptionContextAttributes(
        ExecutionContext ctx,
        WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration
    ) {
        ctx.setInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG, subscriptionConfiguration);
        ctx.setInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI, subscriptionConfiguration.getCallbackUrl());
        ctx.setInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT, buildHttpClient(ctx, subscriptionConfiguration));
    }

    protected WebhookEntrypointConnectorSubscriptionConfiguration readSubscriptionConfiguration(Subscription subscription)
        throws PluginConfigurationException {
        return connectorHelper.readConfiguration(
            WebhookEntrypointConnectorSubscriptionConfiguration.class,
            subscription.getConfiguration()
        );
    }

    private HttpClient buildHttpClient(
        ExecutionContext ctx,
        WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration
    ) {
        return VertxHttpClient
            .builder()
            .sslOptions(subscriptionConfiguration.getSslOptions())
            .vertx(ctx.getComponent(Vertx.class))
            .nodeConfiguration(ctx.getComponent(Configuration.class))
            .defaultTarget(subscriptionConfiguration.getCallbackUrl())
            .build()
            .createHttpClient();
    }

    private RequestOptions buildRequestOptions(ExecutionContext ctx, Message message) {
        final WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration = ctx.getInternalAttribute(
            INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG
        );
        final HttpHeaders responseHeaders = ctx.response().headers();

        RequestOptions requestOptions = new RequestOptions()
            .setURI(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI))
            .setMethod(POST);

        if (responseHeaders != null) {
            responseHeaders.forEach(header -> requestOptions.putHeader(header.getKey(), header.getValue()));
        }
        if (message.headers() != null) {
            message.headers().forEach(header -> putHeaderIfAbsent(requestOptions, header.getKey(), header.getValue()));
        }
        if (subscriptionConfiguration.getHeaders() != null) {
            subscriptionConfiguration
                .getHeaders()
                .forEach(header -> putHeaderIfAbsent(requestOptions, header.getName(), header.getValue()));
        }

        return requestOptions;
    }

    private void putHeaderIfAbsent(RequestOptions requestOptions, String headerName, String headerValue) {
        if (!requestOptions.getHeaders().contains(headerName)) {
            requestOptions.putHeader(headerName, headerValue);
        }
    }

    private boolean is401error(Throwable t) {
        return (
            t instanceof InterruptionFailureException &&
            WEBHOOK_401_ERROR_KEY.equals(((InterruptionFailureException) t).getExecutionFailure().key())
        );
    }
}
