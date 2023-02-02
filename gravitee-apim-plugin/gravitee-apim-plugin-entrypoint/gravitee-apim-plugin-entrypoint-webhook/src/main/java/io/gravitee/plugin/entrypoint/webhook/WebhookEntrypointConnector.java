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
import io.gravitee.definition.model.v4.http.HttpClientOptions;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
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
import io.gravitee.gateway.jupiter.http.vertx.client.VertxHttpClient;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorConfiguration;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorSubscriptionConfiguration;
import io.gravitee.plugin.entrypoint.webhook.exception.GoAwayException;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Objects;
import java.util.Optional;
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
    public static final ListenerType SUPPORTED_LISTENER_TYPE = ListenerType.SUBSCRIPTION;
    public static final String INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT = "webhook.httpClient";
    public static final String INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG = "webhook.subscriptionConfiguration";
    public static final String STOPPING_MESSAGE = "Stopping, please reconnect";
    public static final String WEBHOOK_UNREACHABLE_KEY = "WEBHOOK_UNREACHABLE";
    public static final String WEBHOOK_UNREACHABLE_MESSAGE = "Webhook callback url is unreachable";
    public static final String MESSAGE_PROCESSING_FAILED_KEY = "MESSAGE_PROCESSING_FAILED";
    public static final String MESSAGE_PROCESSING_FAILED_MESSAGE = "Error occurred during message processing";
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
        return SUPPORTED_LISTENER_TYPE;
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
                            .flatMapSingle(message -> send(ctx, httpClient, message, buildRequestOptions(ctx, message)))
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

    protected Single<Message> send(ExecutionContext ctx, HttpClient httpClient, Message message, RequestOptions requestOptions) {
        if (Objects.equals(STOP_MESSAGE_ID, message.id())) {
            return Single.error(new GoAwayException());
        }

        if (message.error()) {
            return ctx
                .interruptMessageWith(new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500).message(message.content().toString()))
                .toSingle();
        }

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
            .flatMap(httpClientResponse -> handleWebhookResponse(ctx, httpClientResponse, message));
    }

    protected Single<Message> handleWebhookResponse(ExecutionContext ctx, HttpClientResponse httpClientResponse, Message message) {
        final int statusCode = httpClientResponse.statusCode();
        if (statusCode >= 200 && statusCode <= 299) {
            return Single.just(message);
        } else if (statusCode >= 500 && statusCode <= 599) {
            // Interrupt in case of server error.
            return ctx
                .interruptMessageWith(new ExecutionFailure(statusCode).key(WEBHOOK_UNREACHABLE_KEY).message(WEBHOOK_UNREACHABLE_MESSAGE))
                .toSingle();
        } else {
            // Any other http status is considered as a functional error.
            message.error(true);
            return Single.just(message);
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
        // for Webhook, we currently only support Timeout settings
        final var clientOptions = Optional
            .ofNullable(configuration.getHttpOptions())
            .map(
                opt -> {
                    var timeOutOptions = new HttpClientOptions();
                    timeOutOptions.setIdleTimeout(opt.getIdleTimeout());
                    timeOutOptions.setReadTimeout(opt.getReadTimeout());
                    timeOutOptions.setConnectTimeout(opt.getConnectTimeout());
                    return timeOutOptions;
                }
            )
            .orElse(new HttpClientOptions());

        return VertxHttpClient
            .builder()
            .sslOptions(subscriptionConfiguration.getSsl())
            .vertx(ctx.getComponent(Vertx.class))
            .nodeConfiguration(ctx.getComponent(Configuration.class))
            .proxyOptions(configuration.getProxyOptions())
            .httpOptions(clientOptions)
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
            .setURI(subscriptionConfiguration.getCallbackUrl())
            .setHeaders(HeadersMultiMap.httpHeaders())
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
}
