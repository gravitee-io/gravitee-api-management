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
            ctx.response().headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(message.length()));
            ctx
                .response()
                .headers()
                .set(HttpHeaderNames.CONTENT_TYPE, environment.getProperty("http.errors[400].contentType", MediaType.TEXT_PLAIN));
            ctx.response().body(Buffer.buffer(message));
            return ctx.response().end(ctx);
        });
    }
}
