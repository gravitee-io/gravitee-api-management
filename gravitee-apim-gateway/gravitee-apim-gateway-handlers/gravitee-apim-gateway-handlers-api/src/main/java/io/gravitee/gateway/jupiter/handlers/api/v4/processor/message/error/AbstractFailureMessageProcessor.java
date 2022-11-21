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
package io.gravitee.gateway.jupiter.handlers.api.v4.processor.message.error;

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.context.interruption.InterruptionHelper;
import io.gravitee.gateway.jupiter.core.processor.MessageProcessor;
import io.gravitee.gateway.jupiter.handlers.api.processor.error.ExecutionFailureMessage;
import io.gravitee.gateway.jupiter.handlers.api.processor.error.ExecutionFailureMessageHelper;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.UnicastProcessor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractFailureMessageProcessor implements MessageProcessor {

    public static final String ID = "message-processor-simple-failure";
    private static final ObjectMapper OBJECT_MAPPER = new GraviteeMapper();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final MutableExecutionContext ctx) {
        return Completable.defer(
            () -> {
                UnicastProcessor<Message> errorEmitter = UnicastProcessor.create();
                return ctx
                    .request()
                    .onMessages(
                        upstream ->
                            upstream.onErrorResumeNext(
                                throwable -> {
                                    return toMessageFlowable(ctx, throwable)
                                        .doOnNext(
                                            message -> {
                                                errorEmitter.onNext(message);
                                                errorEmitter.onComplete();
                                            }
                                        )
                                        .ignoreElements()
                                        .andThen(Flowable.empty());
                                }
                            )
                    )
                    .andThen(
                        ctx
                            .response()
                            .onMessages(
                                upstream ->
                                    errorEmitter
                                        .materialize()
                                        .mergeWith(
                                            upstream
                                                .onErrorResumeNext(
                                                    throwable -> {
                                                        return toMessageFlowable(ctx, throwable);
                                                    }
                                                )
                                                .materialize()
                                        )
                                        .dematerialize(bufferNotification -> bufferNotification)
                            )
                    );
            }
        );
    }

    private Flowable<Message> toMessageFlowable(final MutableExecutionContext ctx, final Throwable throwable) {
        if (InterruptionHelper.isInterruption(throwable)) {
            return Flowable.empty();
        } else {
            ExecutionFailure executionFailure = InterruptionHelper.extractExecutionFailure(throwable);

            if (executionFailure == null) {
                executionFailure =
                    new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .message(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
            }
            ctx.setInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);

            return buildMessage(ctx, executionFailure).toFlowable();
        }
    }

    protected Single<Message> buildMessage(final MessageExecutionContext ctx, final ExecutionFailure executionFailure) {
        DefaultMessage.DefaultMessageBuilder messageBuilder = DefaultMessage.builder().id(UUID.randomUUID().toString()).error(true);
        messageBuilder.attributes(Map.of(ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure));
        Map<String, Object> metadata = new HashMap<>();
        if (executionFailure.key() != null) {
            metadata.put("key", executionFailure.key());
        }
        metadata.put("statusCode", executionFailure.statusCode());
        metadata.put("reason", HttpResponseStatus.valueOf(executionFailure.statusCode()).reasonPhrase());
        messageBuilder.metadata(metadata);
        HttpHeaders headers = HttpHeaders.create();
        if (executionFailure.message() != null) {
            ExecutionFailureMessage failureMessage = ExecutionFailureMessageHelper.createFailureMessage(
                ctx.request(),
                executionFailure,
                OBJECT_MAPPER
            );

            String contentType = failureMessage.getContentType();
            Buffer payload = failureMessage.getPayload();

            messageBuilder.content(payload);
            messageBuilder.headers(
                headers
                    .set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(payload.length()))
                    .set(HttpHeaderNames.CONTENT_TYPE, contentType)
            );
        }
        return Single.just(messageBuilder.headers(headers).build());
    }
}
