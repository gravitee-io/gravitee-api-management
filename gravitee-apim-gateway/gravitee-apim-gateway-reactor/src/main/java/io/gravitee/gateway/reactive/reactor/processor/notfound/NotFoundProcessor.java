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
package io.gravitee.gateway.reactive.reactor.processor.notfound;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

public class NotFoundProcessor implements Processor {

    public static final String ID = "processor-not-found";
    private final Logger LOGGER = LoggerFactory.getLogger(NotFoundProcessor.class);

    private final Environment environment;

    public NotFoundProcessor(final Environment environment) {
        this.environment = environment;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final RequestExecutionContext ctx) {
        return Completable.defer(
            () -> {
                LOGGER.warn("No handler can be found for request {}, returning NOT_FOUND (404)", ctx.request().path());
                // Send a NOT_FOUND HTTP status code (404)
                ctx.response().status(HttpStatusCode.NOT_FOUND_404);
                String message = environment.getProperty("http.errors[404].message", "No context-path matches the request URI.");
                ctx.response().headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(message.length()));
                ctx
                    .response()
                    .headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, environment.getProperty("http.errors[404].contentType", MediaType.TEXT_PLAIN));
                ctx.response().body(Buffer.buffer(message));
                return ctx.response().end();
            }
        );
    }
}
