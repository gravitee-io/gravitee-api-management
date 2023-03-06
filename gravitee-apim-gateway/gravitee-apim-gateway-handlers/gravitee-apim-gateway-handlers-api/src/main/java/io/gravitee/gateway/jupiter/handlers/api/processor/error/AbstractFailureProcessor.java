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
package io.gravitee.gateway.jupiter.handlers.api.processor.error;

import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.GenericRequest;
import io.gravitee.gateway.jupiter.api.context.GenericResponse;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractFailureProcessor implements Processor {

    public static final String ID = "processor-simple-failure";
    /**
     * Code for an unknown caller / application     */
    private static final String APPLICATION_NAME_ANONYMOUS = "1";
    private static final ObjectMapper mapper = new ObjectMapper();

    protected AbstractFailureProcessor() {}

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final MutableExecutionContext ctx) {
        ExecutionFailure executionFailure = ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE);

        if (executionFailure == null) {
            executionFailure =
                new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                .message(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
            ctx.setInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        }

        // If no application has been associated to the request (for example in case security chain can not be processed
        // correctly) set the default application to track it.
        if (ctx.metrics().getApplicationId() == null) {
            ctx.metrics().setApplicationId(APPLICATION_NAME_ANONYMOUS);
        }

        return processFailure(ctx, executionFailure);
    }

    protected Completable processFailure(final HttpExecutionContext ctx, final ExecutionFailure executionFailure) {
        final GenericRequest request = ctx.request();
        final GenericResponse response = ctx.response();

        ctx.metrics().setErrorKey(executionFailure.key());
        response.status(executionFailure.statusCode());
        response.reason(HttpResponseStatus.valueOf(executionFailure.statusCode()).reasonPhrase());
        // In case of client error we don't want to force close the connection
        if (response.status() / 100 != 4) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        }

        if (executionFailure.message() != null) {
            ExecutionFailureMessage failureMessage = ExecutionFailureMessageHelper.createFailureMessage(request, executionFailure, mapper);

            String contentType = failureMessage.getContentType();
            Buffer payload = failureMessage.getPayload();

            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(payload.length()));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            ctx.response().chunks(Flowable.just(payload));
        }
        return Completable.complete();
    }
}
