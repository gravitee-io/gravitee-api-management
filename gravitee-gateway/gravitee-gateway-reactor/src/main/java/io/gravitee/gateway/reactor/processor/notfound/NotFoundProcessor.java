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
package io.gravitee.gateway.reactor.processor.notfound;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

public class NotFoundProcessor extends AbstractProcessor<ExecutionContext> {

    private final Logger LOGGER = LoggerFactory.getLogger(NotFoundProcessor.class);

    private final Environment environment;

    public NotFoundProcessor(final Environment environment) {
        this.environment = environment;
    }

    @Override
    public void handle(ExecutionContext context) {
        LOGGER.warn("No handler can be found for request {}, returning NOT_FOUND (404)", context.request().path());
        // Send a NOT_FOUND HTTP status code (404)
        context.response().status(HttpStatusCode.NOT_FOUND_404);

        String message = environment.getProperty("http.errors[404].message", "No context-path matches the request URI.");
        context.response().headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(message.length()));
        context
            .response()
            .headers()
            .set(HttpHeaders.CONTENT_TYPE, environment.getProperty("http.errors[404].contentType", MediaType.TEXT_PLAIN));
        context.response().headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        context.response().write(Buffer.buffer(message));

        context.response().end();
        next.handle(context);
    }
}
