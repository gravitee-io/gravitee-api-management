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
package io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.request;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpRequestInternal;
import io.gravitee.gateway.reactive.core.v4.analytics.BufferUtils;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.node.api.opentelemetry.Span;
import java.util.Map;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class LogRequest extends io.gravitee.reporter.api.common.Request {

    protected final LoggingContext loggingContext;
    protected final HttpRequestInternal request;

    protected LogRequest(LoggingContext loggingContext, HttpRequestInternal request) {
        this.loggingContext = loggingContext;
        this.request = request;

        this.setMethod(request.method());
    }

    protected abstract boolean isLogPayload();

    protected abstract boolean isLogHeaders();

    /**
     * Adds a {@code "payload"} span event to the root OTel span for the current request.
     * No-op when OTel tracing is disabled or no root span exists in the context.
     */
    protected void emitPayloadSpanEvent(HttpExecutionContextInternal ctx, String body) {
        Span span = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_TRACING_ROOT_SPAN);
        if (span == null) {
            return;
        }
        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        span.addEvent(
            "payload",
            Map.of("payload.body", body, "payload.format", resolvePayloadFormat(contentType), "payload.phase", "REQUEST")
        );
    }

    // NOTE: mirrored in LogResponse — both will be replaced by PayloadFormat enum from gravitee-node (APIM-13580)
    private static String resolvePayloadFormat(String contentType) {
        if (contentType == null) return "UNKNOWN";
        String ct = contentType.toLowerCase();
        if (ct.contains("json")) return "JSON";
        if (ct.contains("xml")) return "XML";
        return "UNKNOWN";
    }
}
