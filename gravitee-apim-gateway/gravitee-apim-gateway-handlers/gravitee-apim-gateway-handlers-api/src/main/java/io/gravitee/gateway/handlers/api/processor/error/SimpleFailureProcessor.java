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
package io.gravitee.gateway.handlers.api.processor.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SimpleFailureProcessor extends AbstractProcessor<ExecutionContext> {

    /**
     * Code for an unknown caller / application
     */
    private static final String APPLICATION_NAME_ANONYMOUS = "1";

    private static final String PROCESSOR_FAILURE_ATTRIBUTE = ExecutionContext.ATTR_PREFIX + "failure";

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(ExecutionContext context) {
        final ProcessorFailure failure = (ProcessorFailure) context.getAttribute(PROCESSOR_FAILURE_ATTRIBUTE);

        // If no application has been associated to the request (for example in case security chain can not be processed
        // correctly) set the default application to track it.
        if (context.request().metrics().getApplication() == null) {
            context.request().metrics().setApplication(APPLICATION_NAME_ANONYMOUS);
        }

        handleFailure(context, failure);
        next.handle(context);
    }

    protected void handleFailure(final ExecutionContext context, final ProcessorFailure failure) {
        final Response response = context.response();

        context.request().metrics().setErrorKey(failure.key());

        response.status(failure.statusCode());
        response.reason(HttpResponseStatus.valueOf(response.status()).reasonPhrase());
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);

        if (failure.message() != null) {
            List<String> accepts = context.request().headers().getAll(HttpHeaderNames.ACCEPT);

            Buffer payload;
            String contentType;

            if (accepts != null && (accepts.contains(MediaType.APPLICATION_JSON) || accepts.contains(MediaType.WILDCARD))) {
                // Write error as json when accepted by the client.
                contentType = MediaType.APPLICATION_JSON;

                if (failure.contentType() != null && failure.contentType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
                    // Message is already json string.
                    payload = Buffer.buffer(failure.message());
                } else {
                    try {
                        String contentAsJson = mapper.writeValueAsString(new ProcessorFailureAsJson(failure));
                        payload = Buffer.buffer(contentAsJson);
                    } catch (JsonProcessingException jpe) {
                        // There is a problem with json. Just return the content in text/plain.
                        contentType = MediaType.TEXT_PLAIN;
                        payload = Buffer.buffer(failure.message());
                    }
                }
            } else {
                // Fallback to text/plain error.
                contentType = MediaType.TEXT_PLAIN;
                payload = Buffer.buffer(failure.message());
            }

            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(payload.length()));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            response.write(payload);
        }
    }

    private class ProcessorFailureAsJson {

        @JsonProperty
        private final String message;

        @JsonProperty("http_status_code")
        private final int httpStatusCode;

        private ProcessorFailureAsJson(ProcessorFailure processorFailure) {
            this.message = processorFailure.message();
            this.httpStatusCode = processorFailure.statusCode();
        }

        private String getMessage() {
            return message;
        }

        private int httpStatusCode() {
            return httpStatusCode;
        }
    }
}
