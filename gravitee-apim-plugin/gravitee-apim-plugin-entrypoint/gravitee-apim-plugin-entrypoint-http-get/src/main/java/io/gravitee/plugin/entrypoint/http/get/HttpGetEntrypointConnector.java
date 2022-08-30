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
import io.gravitee.plugin.entrypoint.http.get.configuration.HttpGetEntrypointConnectorConfiguration;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Slf4j
public class HttpGetEntrypointConnector implements EntrypointAsyncConnector {

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

    protected final HttpGetEntrypointConnectorConfiguration configuration;

    @Override
    public ListenerType supportedListenerType() {
        return ListenerType.HTTP;
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

    @Override
    public Completable handleRequest(final ExecutionContext ctx) {
        return Completable.defer(
            () -> {
                String acceptHeader = ctx.request().headers().get(HttpHeaderNames.ACCEPT);
                final String contentType = (acceptHeader == null || acceptHeader.isEmpty() || acceptHeader.equals(MediaType.WILDCARD))
                    ? MediaType.TEXT_PLAIN
                    : acceptHeader;
                if (
                    !contentType.equals(MediaType.APPLICATION_JSON) &&
                    !contentType.equals(MediaType.APPLICATION_XML) &&
                    !contentType.equals(MediaType.TEXT_PLAIN)
                ) {
                    return ctx.interruptWith(
                        new ExecutionFailure(HttpStatusCode.BAD_REQUEST_400).message("Unsupported accept header: " + acceptHeader)
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
                        ctx.putInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_RESUME_LASTID, cursor);
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

    private Flowable<Buffer> messagesToBuffer(ExecutionContext ctx, final String contentType) {
        final AtomicBoolean first = new AtomicBoolean(true);
        Long messagesLimitDurationMs = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS);
        Integer messagesLimitCount = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT);

        Flowable<Message> limitedMessageFlowable = ctx.response().messages();

        if (messagesLimitCount != null) {
            limitedMessageFlowable = limitedMessageFlowable.take(messagesLimitCount);
        }

        if (messagesLimitDurationMs != null && messagesLimitDurationMs > 0) {
            long start = ctx.request().timestamp();
            limitedMessageFlowable =
                limitedMessageFlowable.takeUntil(message -> (System.currentTimeMillis()) > start + messagesLimitDurationMs);
        }

        if (contentType.equals(MediaType.APPLICATION_JSON)) {
            return Flowable
                .just(Buffer.buffer("{\"items\":["))
                .concatWith(
                    limitedMessageFlowable.map(
                        message -> {
                            ctx.putInternalAttribute(ATTR_INTERNAL_LAST_MESSAGE_ID, message.id());
                            return toJsonBuffer(message, first.getAndSet(false));
                        }
                    )
                )
                .concatWith(Flowable.just(Buffer.buffer("]")))
                .concatWith(computePagination(ctx, contentType))
                .concatWith(Flowable.just(Buffer.buffer("}")));
        } else if (contentType.equals(MediaType.APPLICATION_XML)) {
            return Flowable
                .just(Buffer.buffer("<response><items>"))
                .concatWith(
                    limitedMessageFlowable.map(
                        message -> {
                            ctx.putInternalAttribute(ATTR_INTERNAL_LAST_MESSAGE_ID, message.id());
                            return this.toXmlBuffer(message);
                        }
                    )
                )
                .concatWith(Flowable.just(Buffer.buffer("</items>")))
                .concatWith(computePagination(ctx, contentType))
                .concatWith(Flowable.just(Buffer.buffer("</response>")));
        } else {
            return Flowable
                .just(Buffer.buffer("items="))
                .concatWith(
                    limitedMessageFlowable.map(
                        message -> {
                            ctx.putInternalAttribute(ATTR_INTERNAL_LAST_MESSAGE_ID, message.id());
                            return toPlainTextBuffer(message, first.getAndSet(false));
                        }
                    )
                )
                .concatWith(computePagination(ctx, contentType));
        }
    }

    private Buffer toJsonBuffer(Message message, boolean isFirstElement) {
        JsonObject jsonMessage = new JsonObject();

        if (configuration.isHeadersInPayload()) {
            JsonObject headers = JsonObject.mapFrom(message.headers());
            jsonMessage.put("headers", headers);
        }
        jsonMessage.put("id", message.id());
        jsonMessage.put("content", message.content().toString());

        if (configuration.isMetadataInPayload()) {
            JsonObject metadata = JsonObject.mapFrom(message.metadata());
            jsonMessage.put("metadata", metadata);
        }

        String messageString = (!isFirstElement ? "," : "") + jsonMessage.encode();
        return Buffer.buffer(messageString);
    }

    private Buffer toXmlBuffer(Message message) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("<item>");

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

        messageBuilder.append("<id>").append(message.id()).append("</id>");
        messageBuilder.append("<content><![CDATA[").append(message.content().toString()).append("]]></content>");

        if (configuration.isMetadataInPayload()) {
            messageBuilder.append("<metadata>");
            message
                .metadata()
                .forEach(
                    (key, value) -> messageBuilder.append("<").append(key).append(">").append(value).append("</").append(key).append(">")
                );
            messageBuilder.append("</metadata>");
        }

        messageBuilder.append("</item>");
        return Buffer.buffer(messageBuilder.toString());
    }

    private Buffer toPlainTextBuffer(Message message, boolean isFirstElement) {
        StringBuilder messageBuilder = new StringBuilder();
        if (!isFirstElement) {
            messageBuilder.append(", ");
        }
        messageBuilder.append("(");
        if (configuration.isHeadersInPayload()) {
            messageBuilder.append(message.headers().toListValuesMap());
            messageBuilder.append(", ");
        }
        messageBuilder.append("id=");
        messageBuilder.append(message.id());
        messageBuilder.append(", content=");
        messageBuilder.append(message.content());

        if (configuration.isMetadataInPayload()) {
            messageBuilder.append(", ");
            messageBuilder.append(message.metadata());
        }

        messageBuilder.append(")");
        return Buffer.buffer(messageBuilder.toString());
    }

    private Flowable<Buffer> computePagination(ExecutionContext ctx, String contentType) {
        return Flowable.defer(
            () -> {
                String limit = ctx.request().parameters().getFirst(LIMIT_QUERY_PARAM);
                String currentCursor = ctx.request().parameters().getFirst(CURSOR_QUERY_PARAM);
                String nextCursor = ctx.getInternalAttribute(ATTR_INTERNAL_LAST_MESSAGE_ID);

                if (contentType.equals(MediaType.APPLICATION_JSON)) {
                    List<String> paginationString = new ArrayList<>();
                    if (currentCursor != null && !currentCursor.isEmpty()) {
                        paginationString.add("\"cursor\":\"" + currentCursor + "\"");
                    }
                    if (nextCursor != null && !nextCursor.isEmpty()) {
                        paginationString.add("\"nextCursor\":\"" + nextCursor + "\"");
                    }
                    if (limit != null && !limit.isEmpty()) {
                        paginationString.add("\"limit\":\"" + limit + "\"");
                    }
                    return Flowable.just(Buffer.buffer(",\"pagination\":{" + String.join(",", paginationString) + "}"));
                } else if (contentType.equals(MediaType.APPLICATION_XML)) {
                    StringBuilder paginationString = new StringBuilder();
                    if (currentCursor != null && !currentCursor.isEmpty()) {
                        paginationString.append("<cursor>" + currentCursor + "</cursor>");
                    }
                    if (nextCursor != null && !nextCursor.isEmpty()) {
                        paginationString.append("<nextCursor>" + nextCursor + "</nextCursor>");
                    }
                    if (limit != null && !limit.isEmpty()) {
                        paginationString.append("<limit>" + limit + "</limit>");
                    }
                    return Flowable.just(Buffer.buffer("<pagination>" + String.join("", paginationString) + "</pagination>"));
                } else {
                    List<String> paginationString = new ArrayList<>();
                    if (currentCursor != null && !currentCursor.isEmpty()) {
                        paginationString.add("cursor=" + currentCursor);
                    }
                    if (nextCursor != null && !nextCursor.isEmpty()) {
                        paginationString.add("nextCursor=" + nextCursor);
                    }
                    if (limit != null && !limit.isEmpty()) {
                        paginationString.add("limit=" + limit);
                    }
                    return Flowable.just(Buffer.buffer("\npagination=(" + String.join(", ", paginationString) + ")"));
                }
            }
        );
    }
}
