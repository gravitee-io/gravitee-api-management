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

import static io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import java.util.List;

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
    public Completable execute(final RequestExecutionContext executionContext) {
        ExecutionFailure executionFailure = executionContext.getInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE);

        if (executionFailure == null) {
            executionFailure =
                new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                .message(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
        }

        // If no application has been associated to the request (for example in case security chain can not be processed
        // correctly) set the default application to track it.
        if (executionContext.request().metrics().getApplication() == null) {
            executionContext.request().metrics().setApplication(APPLICATION_NAME_ANONYMOUS);
        }

        return processFailure(executionContext, executionFailure);
    }

    protected Completable processFailure(final RequestExecutionContext context, final ExecutionFailure executionFailure) {
        final Request request = context.request();
        final Response response = context.response();

        request.metrics().setErrorKey(executionFailure.key());
        response.status(executionFailure.statusCode());
        response.reason(HttpResponseStatus.valueOf(executionFailure.statusCode()).reasonPhrase());
        // In case of client error we don't want to force close the connection
        if (response.status() / 100 != 4) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        }

        if (executionFailure.message() != null) {
            List<String> accepts = request.headers().getAll(HttpHeaderNames.ACCEPT);

            Buffer payload;
            String contentType;

            if (accepts != null && (accepts.contains(MediaType.APPLICATION_JSON) || accepts.contains(MediaType.WILDCARD))) {
                // Write error as json when accepted by the client.
                contentType = MediaType.APPLICATION_JSON;

                if (executionFailure.contentType() != null && executionFailure.contentType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
                    // Message is already json string.
                    payload = Buffer.buffer(executionFailure.message());
                } else {
                    try {
                        String contentAsJson = mapper.writeValueAsString(new ExecutionFailureAsJson(executionFailure));
                        payload = Buffer.buffer(contentAsJson);
                    } catch (JsonProcessingException jpe) {
                        // There is a problem with json. Just return the content in text/plain.
                        contentType = MediaType.TEXT_PLAIN;
                        payload = Buffer.buffer(executionFailure.message());
                    }
                }
            } else {
                // Fallback to text/plain error.
                contentType = MediaType.TEXT_PLAIN;
                payload = Buffer.buffer(executionFailure.message());
            }

            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(payload.length()));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            context.response().body(payload);
        }
        return Completable.complete();
    }
}
