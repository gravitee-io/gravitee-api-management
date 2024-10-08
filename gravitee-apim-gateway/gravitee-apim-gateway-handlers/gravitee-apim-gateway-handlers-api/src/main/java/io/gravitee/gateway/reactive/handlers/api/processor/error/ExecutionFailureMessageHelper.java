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
package io.gravitee.gateway.reactive.handlers.api.processor.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.GenericRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionFailureMessageHelper {

    public static ExecutionFailureMessage createFailureMessage(
        final HttpBaseRequest request,
        final ExecutionFailure executionFailure,
        final ObjectMapper objectMapper
    ) {
        List<String> accepts = request.headers().getAll(HttpHeaderNames.ACCEPT);

        Buffer payload;
        String contentType;

        if (accepts != null && accepts.stream().anyMatch(s -> s.contains(MediaType.APPLICATION_JSON) || s.contains(MediaType.WILDCARD))) {
            // Write error as json when accepted by the client.
            contentType = MediaType.APPLICATION_JSON;

            if (executionFailure.contentType() != null && executionFailure.contentType().equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
                // Message is already json string.
                payload = Buffer.buffer(executionFailure.message());
            } else {
                try {
                    String contentAsJson = objectMapper.writeValueAsString(new ExecutionFailureAsJson(executionFailure));
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
        return ExecutionFailureMessage.builder().payload(payload).contentType(contentType).build();
    }
}
