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
package io.gravitee.plugin.entrypoint.http.get;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.gateway.jupiter.api.qos.QosCapability;
import io.gravitee.gateway.jupiter.api.qos.QosRequirement;
import io.gravitee.plugin.entrypoint.http.get.configuration.HttpGetEntrypointConnectorConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Slf4j
public class HttpGetEntrypointConnector extends EntrypointAsyncConnector {

    /**
     * Internal attribute used to store the id of the last message sent to a client. Restricted for this entrypoint.
     */
    static final String ATTR_INTERNAL_LAST_MESSAGE_ID = "last.message.id";
    /**
     * Internal attribute used to store the content type of the response.
     */
    static final String ATTR_INTERNAL_RESPONSE_CONTENT_TYPE = "response.content-type";
    static final String CURSOR_QUERY_PARAM = "cursor";
    static final String LIMIT_QUERY_PARAM = "limit";
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.SUBSCRIBE);
    static final Set<Qos> SUPPORTED_QOS = Set.of(Qos.AUTO, Qos.AT_MOST_ONCE, Qos.AT_LEAST_ONCE);
    static final ListenerType SUPPORTED_LISTENER_TYPE = ListenerType.HTTP;
    private static final String ENTRYPOINT_ID = "http-get";
    protected HttpGetEntrypointConnectorConfiguration configuration;
    private QosRequirement qosRequirement;

    public HttpGetEntrypointConnector(final Qos qos, final HttpGetEntrypointConnectorConfiguration configuration) {
        computeQosRequirement(qos);
        this.configuration = configuration;
        if (this.configuration == null) {
            this.configuration = new HttpGetEntrypointConnectorConfiguration();
        }
    }

    private static boolean isMediaTypeSupported(MediaType mediaType) {
        return isMediaTypeSupported(mediaType.toMediaString());
    }

    private static boolean isMediaTypeSupported(String mediaType) {
        return (
            mediaType.equals(MediaType.APPLICATION_JSON) ||
            mediaType.equals(MediaType.APPLICATION_XML) ||
            mediaType.equals(MediaType.TEXT_PLAIN)
        );
    }

    private void computeQosRequirement(final Qos qos) {
        QosRequirement.QosRequirementBuilder qosRequirementBuilder = QosRequirement.builder().qos(qos);
        switch (qos) {
            case AT_LEAST_ONCE:
            case AT_MOST_ONCE:
                qosRequirementBuilder.capabilities(Set.of(QosCapability.RECOVER)).build();
                break;
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
        return 1;
    }

    @Override
    public boolean matches(final ExecutionContext ctx) {
        return (HttpMethod.GET == ctx.request().method());
    }

    /**
     * Selects the Content-Type based on the ACCEPT header values. Supports quality parameters.
     * Supported types are "application/json", "application/xml", "text/plain" and WILDCARD.
     * A WILDCARD ACCEPT header will fall back to an "application/json" Content-Type.
     * If ACCEPT header is null or empty, "text/plain" will be chosen by default.
     * @param acceptHeaderValues is the list of ACCEPT header values to parse in order to select the best one.
     * @return the {@link MediaType} as a {@link String}
     */
    private String selectContentType(final List<String> acceptHeaderValues) {
        final List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeaderValues);
        MediaType.sortByQualityValue(mediaTypes);
        return mediaTypes
            .stream()
            // First check if a supported header is present and select the best based on its quality parameter
            .filter(HttpGetEntrypointConnector::isMediaTypeSupported)
            .map(MediaType::toMediaString)
            .findFirst()
            .orElseGet(
                () -> {
                    // If no supported header, we use the first of the list
                    // A null or empty header will return "text/plain"
                    // A WILDCARD header will return "application/json"
                    // else we simply return it
                    String bestAcceptHeader = !mediaTypes.isEmpty() ? mediaTypes.get(0).toMediaString() : null;
                    if (bestAcceptHeader == null || bestAcceptHeader.isEmpty()) {
                        return MediaType.TEXT_PLAIN;
                    } else if (bestAcceptHeader.equals(MediaType.WILDCARD)) {
                        return MediaType.APPLICATION_JSON;
                    } else {
                        return bestAcceptHeader;
                    }
                }
            );
    }

    @Override
    public QosRequirement qosRequirement() {
        return qosRequirement;
    }

    @Override
    public Completable handleRequest(final ExecutionContext ctx) {
        return Completable.defer(
            () -> {
                List<String> acceptHeaderValues = ctx.request().headers().getAll(HttpHeaderNames.ACCEPT);
                final String contentType = selectContentType(acceptHeaderValues);
                if (!isMediaTypeSupported(contentType)) {
                    return ctx.interruptWith(
                        new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400).message("Unsupported accept header: " + acceptHeaderValues)
                    );
                }
                ctx.putInternalAttribute(ATTR_INTERNAL_RESPONSE_CONTENT_TYPE, contentType);

                ctx.putInternalAttribute(
                    InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS,
                    configuration.getMessagesLimitDurationMs()
                );

                int messagesLimitCount = configuration.getMessagesLimitCount();
                if (ctx.request().parameters().containsKey(LIMIT_QUERY_PARAM)) {
                    String limit = ctx.request().parameters().getFirst(LIMIT_QUERY_PARAM);
                    if (limit != null && !limit.isEmpty()) {
                        messagesLimitCount = Math.min(messagesLimitCount, Integer.parseInt(limit));
                    }
                }
                ctx.putInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT, messagesLimitCount);

                if (ctx.request().parameters().containsKey(CURSOR_QUERY_PARAM)) {
                    String cursor = ctx.request().parameters().getFirst(CURSOR_QUERY_PARAM);
                    if (cursor != null && !cursor.isEmpty()) {
                        ctx.putInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_RECOVERY_LAST_ID, cursor);
                    }
                }

                return Completable.complete();
            }
        );
    }

    @Override
    public Completable handleResponse(final ExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                String contentType = ctx.getInternalAttribute(ATTR_INTERNAL_RESPONSE_CONTENT_TYPE);
                ctx.response().headers().add(HttpHeaderNames.CONTENT_TYPE, contentType);
                ctx.response().chunks(messagesToBuffer(ctx, contentType));
            }
        );
    }

    @Override
    public void doStop() {
        emitStopMessage();
    }

    private Flowable<Buffer> messagesToBuffer(ExecutionContext ctx, final String contentType) {
        final AtomicBoolean first = new AtomicBoolean(true);
        Long messagesLimitDurationMs = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS);
        Integer messagesLimitCount = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT);

        Flowable<Message> limitedMessageFlowable = ctx.response().messages().compose(applyStopHook());

        if (messagesLimitCount != null) {
            limitedMessageFlowable = limitedMessageFlowable.take(messagesLimitCount);
        }

        if (messagesLimitDurationMs != null && messagesLimitDurationMs > 0) {
            limitedMessageFlowable = limitedMessageFlowable.take(messagesLimitDurationMs, TimeUnit.MILLISECONDS);
        }

        AtomicReference<Message> hasError = new AtomicReference<>();
        if (contentType.equals(MediaType.APPLICATION_JSON)) {
            return Flowable
                .just(Buffer.buffer("{\"items\":["))
                .concatWith(
                    limitedMessageFlowable.flatMapMaybe(
                        message -> {
                            if (!message.error()) {
                                ctx.putInternalAttribute(ATTR_INTERNAL_LAST_MESSAGE_ID, message.id());
                                return Maybe.just(toJsonBuffer(message, first.getAndSet(false)));
                            } else {
                                hasError.set(message);
                                return Maybe.empty();
                            }
                        }
                    )
                )
                .concatWith(Flowable.just(Buffer.buffer("]")))
                .concatWith(
                    Flowable.defer(
                        () -> {
                            Message errorMessage = hasError.getAndSet(null);
                            if (errorMessage != null) {
                                return Flowable.fromArray(Buffer.buffer(",\"error\":"), toJsonBuffer(errorMessage, true));
                            }
                            return Flowable.empty();
                        }
                    )
                )
                .concatWith(computePagination(ctx, contentType))
                .concatWith(Flowable.just(Buffer.buffer("}")));
        } else if (contentType.equals(MediaType.APPLICATION_XML)) {
            return Flowable
                .just(Buffer.buffer("<response><items>"))
                .concatWith(
                    limitedMessageFlowable.flatMapMaybe(
                        message -> {
                            if (!message.error()) {
                                ctx.putInternalAttribute(ATTR_INTERNAL_LAST_MESSAGE_ID, message.id());
                                return Maybe.just(this.toXmlBuffer(message, "item"));
                            } else {
                                hasError.set(message);
                                return Maybe.empty();
                            }
                        }
                    )
                )
                .concatWith(Flowable.just(Buffer.buffer("</items>")))
                .concatWith(
                    Maybe.defer(
                        () -> {
                            Message errorMessage = hasError.getAndSet(null);
                            if (errorMessage != null) {
                                return Maybe.just(toXmlBuffer(errorMessage, "error"));
                            }
                            return Maybe.empty();
                        }
                    )
                )
                .concatWith(computePagination(ctx, contentType))
                .concatWith(Flowable.just(Buffer.buffer("</response>")));
        } else {
            return Flowable
                .just(Buffer.buffer("items\n"))
                .concatWith(
                    limitedMessageFlowable.flatMapMaybe(
                        message -> {
                            if (!message.error()) {
                                ctx.putInternalAttribute(ATTR_INTERNAL_LAST_MESSAGE_ID, message.id());
                                return Maybe.just(toPlainTextBuffer(message, "item", first.getAndSet(false)));
                            } else {
                                hasError.set(message);
                                return Maybe.empty();
                            }
                        }
                    )
                )
                .concatWith(
                    Maybe.defer(
                        () -> {
                            Message errorMessage = hasError.getAndSet(null);
                            if (errorMessage != null) {
                                return Maybe.just(toPlainTextBuffer(errorMessage, "error", false));
                            }
                            return Maybe.empty();
                        }
                    )
                )
                .concatWith(computePagination(ctx, contentType));
        }
    }

    private Buffer toJsonBuffer(Message message, boolean isFirstElement) {
        JsonObject jsonMessage = new JsonObject();

        if (message.id() != null) {
            jsonMessage.put("id", message.id());
        }
        jsonMessage.put("content", message.content().toString());

        if (configuration.isHeadersInPayload()) {
            JsonObject headers = JsonObject.mapFrom(message.headers());
            jsonMessage.put("headers", headers);
        }

        if (configuration.isMetadataInPayload()) {
            JsonObject metadata = JsonObject.mapFrom(message.metadata());
            jsonMessage.put("metadata", metadata);
        }

        String messageString = (!isFirstElement ? "," : "") + jsonMessage.encode();
        return Buffer.buffer(messageString);
    }

    private Buffer toXmlBuffer(Message message, final String type) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("<");
        messageBuilder.append(type);
        messageBuilder.append(">");

        if (message.id() != null) {
            messageBuilder.append("<id>").append(message.id()).append("</id>");
        }
        messageBuilder.append("<content><![CDATA[").append(message.content().toString()).append("]]></content>");

        if (configuration.isHeadersInPayload()) {
            messageBuilder.append("<headers>");
            message
                .headers()
                .toListValuesMap()
                .forEach(
                    (header, values) ->
                        messageBuilder
                            .append("<")
                            .append(header)
                            .append(">")
                            .append(String.join(",", values))
                            .append("</")
                            .append(header)
                            .append(">")
                );
            messageBuilder.append("</headers>");
        }
        if (configuration.isMetadataInPayload()) {
            messageBuilder.append("<metadata>");
            message
                .metadata()
                .forEach(
                    (key, value) -> messageBuilder.append("<").append(key).append(">").append(value).append("</").append(key).append(">")
                );
            messageBuilder.append("</metadata>");
        }

        messageBuilder.append("</");
        messageBuilder.append(type);
        messageBuilder.append(">");
        return Buffer.buffer(messageBuilder.toString());
    }

    private Buffer toPlainTextBuffer(Message message, final String type, boolean isFirstElement) {
        StringBuilder messageBuilder = new StringBuilder();
        if (!isFirstElement) {
            messageBuilder.append("\n");
        }
        messageBuilder.append(type);
        messageBuilder.append("\n");
        if (message.id() != null) {
            messageBuilder.append("id: ");
            messageBuilder.append(message.id());
            messageBuilder.append("\n");
        }
        messageBuilder.append("content: ");
        messageBuilder.append(message.content());
        messageBuilder.append("\n");

        if (configuration.isHeadersInPayload()) {
            messageBuilder.append("headers: ");
            messageBuilder.append(message.headers().toListValuesMap());
            messageBuilder.append("\n");
        }

        if (configuration.isMetadataInPayload()) {
            messageBuilder.append("metadata: ");
            messageBuilder.append(message.metadata());
            messageBuilder.append("\n");
        }
        return Buffer.buffer(messageBuilder.toString());
    }

    private Flowable<Buffer> computePagination(ExecutionContext ctx, String contentType) {
        return Flowable.defer(
            () -> {
                String nextCursor = ctx.getInternalAttribute(ATTR_INTERNAL_LAST_MESSAGE_ID);
                if (nextCursor != null && !nextCursor.isEmpty()) {
                    String limit = ctx.request().parameters().getFirst(LIMIT_QUERY_PARAM);

                    if (contentType.equals(MediaType.APPLICATION_JSON)) {
                        List<String> paginationString = new ArrayList<>();
                        paginationString.add("\"nextCursor\":\"" + nextCursor + "\"");
                        if (limit != null && !limit.isEmpty()) {
                            paginationString.add("\"limit\":\"" + limit + "\"");
                        }
                        return Flowable.just(Buffer.buffer(",\"pagination\":{" + String.join(",", paginationString) + "}"));
                    } else if (contentType.equals(MediaType.APPLICATION_XML)) {
                        StringBuilder paginationString = new StringBuilder("<pagination>");
                        paginationString.append("<nextCursor>").append(nextCursor).append("</nextCursor>");
                        if (limit != null && !limit.isEmpty()) {
                            paginationString.append("<limit>").append(limit).append("</limit>");
                        }
                        paginationString.append("</pagination>");
                        return Flowable.just(Buffer.buffer(paginationString.toString()));
                    } else {
                        StringBuilder paginationBuilder = new StringBuilder();
                        paginationBuilder.append("\npagination");
                        if (nextCursor != null && !nextCursor.isEmpty()) {
                            paginationBuilder.append("\nnextCursor: ").append(nextCursor);
                        }
                        if (limit != null && !limit.isEmpty()) {
                            paginationBuilder.append("\nlimit: ").append(limit);
                        }
                        return Flowable.just(Buffer.buffer(paginationBuilder.toString()));
                    }
                }
                return Flowable.empty();
            }
        );
    }
}
