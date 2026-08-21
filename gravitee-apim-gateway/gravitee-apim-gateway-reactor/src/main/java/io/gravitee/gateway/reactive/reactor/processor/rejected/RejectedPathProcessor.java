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
package io.gravitee.gateway.reactive.reactor.processor.rejected;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.grpc.Status;
import io.reactivex.rxjava3.core.Completable;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;

/**
 * Answers 400 for a request whose path the gateway refused to route on, and makes sure it is
 * reported.
 *
 * <p>A rejection happens before any API is selected, so nothing downstream would ever account for
 * it. Left to answer on the raw response, the request would leave no trace at all — which is the
 * wrong outcome for a control whose value is telling an operator that someone is probing the
 * platform. Running through a processor chain is what gives it a metric and a report, the way an
 * unmatched context path already gets one.
 *
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class RejectedPathProcessor implements Processor {

    private static final String ID = "processor-rejected-path";
    private static final String UNKNOWN_SERVICE = "1";
    private static final String DEFAULT_MESSAGE = "The request path is not in its normalized form.";

    private final Environment environment;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.defer(() -> {
            ctx.withLogger(log).warn("Rejecting request {}, returning BAD_REQUEST (400)", ctx.request().path());

            final Metrics metrics = ctx.metrics();
            metrics.setApiId(UNKNOWN_SERVICE);
            metrics.setApplicationId(UNKNOWN_SERVICE);

            ctx.response().status(HttpStatusCode.BAD_REQUEST_400);

            final String message = environment.getProperty("http.errors[400].message", DEFAULT_MESSAGE);

            // A gRPC caller reads the status from the trailers, not from the HTTP code: without
            // these it sees UNKNOWN and has no idea why it was refused. Same treatment as an
            // unmatched context path, which is the closest thing this rejection resembles.
            final MediaType mediaType = MediaType.parseMediaType(ctx.request().headers().get(HttpHeaderNames.CONTENT_TYPE));
            if (MediaType.MEDIA_APPLICATION_GRPC.equals(mediaType)) {
                ctx.response().headers().set("grpc-status", String.valueOf(Status.INVALID_ARGUMENT.getCode().value()));
                ctx.response().headers().set("grpc-message", message);
            }

            final Buffer body = Buffer.buffer(message);
            // Counted on the encoded body, not on the string: a localised http.errors[400].message
            // holding any non-ASCII character has more bytes than it has characters, and a short
            // Content-Length truncates the response.
            ctx.response().headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(body.length()));
            ctx
                .response()
                .headers()
                .set(HttpHeaderNames.CONTENT_TYPE, environment.getProperty("http.errors[400].contentType", MediaType.TEXT_PLAIN));
            ctx.response().body(body);
            return ctx.response().end(ctx);
        });
    }
}
