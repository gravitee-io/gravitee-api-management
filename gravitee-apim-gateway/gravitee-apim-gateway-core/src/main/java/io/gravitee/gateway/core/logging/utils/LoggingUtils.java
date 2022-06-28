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
package io.gravitee.gateway.core.logging.utils;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.core.logging.LoggingContext;
import io.reactivex.Completable;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class LoggingUtils {

    private static final String DEFAULT_EXCLUDED_CONTENT_TYPES =
        "video.*|audio.*|image.*|application\\/octet-stream|application\\/pdf|text\\/event-stream";

    private static Pattern EXCLUDED_CONTENT_TYPES_PATTERN;

    @Nullable
    public static LoggingContext getLoggingContext(@Nonnull final Api api) {
        final Logging logging = api.getProxy().getLogging();

        if (logging != null) {
            final LoggingMode loggingMode = logging.getMode();

            if (loggingMode != null && loggingMode != LoggingMode.NONE) {
                return new LoggingContext(logging);
            }
        }

        return null;
    }

    public static int getMaxSizeLogMessage(ExecutionContext executionContext) {
        try {
            final LoggingContext loggingContext = getLoggingContext(executionContext);

            if (loggingContext == null) {
                return -1;
            }

            return loggingContext.getMaxSizeLogMessage();
        } catch (Exception ex) {
            return -1;
        }
    }

    public static boolean isContentTypeLoggable(final String contentType, final ExecutionContext executionContext) {
        return isContentTypeLoggable(contentType, getLoggingContext(executionContext));
    }

    public static boolean isContentTypeLoggable(final String contentType, final LoggingContext loggingContext) {
        // init pattern
        if (EXCLUDED_CONTENT_TYPES_PATTERN == null) {
            try {
                final String responseTypes = loggingContext.getExcludedResponseTypes();
                EXCLUDED_CONTENT_TYPES_PATTERN = Pattern.compile(responseTypes);
            } catch (Exception e) {
                EXCLUDED_CONTENT_TYPES_PATTERN = Pattern.compile(DEFAULT_EXCLUDED_CONTENT_TYPES);
            }
        }

        return contentType == null || !EXCLUDED_CONTENT_TYPES_PATTERN.matcher(contentType).find();
    }

    public static boolean isRequestHeadersLoggable(final ExecutionContext executionContext) {
        final LoggingContext context = getLoggingContext(executionContext);
        return context != null && context.requestHeaders();
    }

    public static boolean isRequestPayloadsLoggable(final ExecutionContext executionContext) {
        final LoggingContext context = getLoggingContext(executionContext);
        return context != null && context.requestPayload();
    }

    public static boolean isResponseHeadersLoggable(final ExecutionContext executionContext) {
        final LoggingContext context = getLoggingContext(executionContext);
        return context != null && context.responseHeaders();
    }

    public static boolean isResponsePayloadsLoggable(final ExecutionContext executionContext) {
        final LoggingContext context = getLoggingContext(executionContext);
        return context != null && context.responsePayload();
    }

    public static boolean isProxyRequestHeadersLoggable(final ExecutionContext executionContext) {
        final LoggingContext context = getLoggingContext(executionContext);
        return context != null && context.proxyRequestHeaders();
    }

    public static boolean isProxyRequestPayloadsLoggable(final ExecutionContext executionContext) {
        final LoggingContext context = getLoggingContext(executionContext);
        return context != null && context.proxyRequestPayload();
    }

    public static boolean isProxyResponseHeadersLoggable(final ExecutionContext executionContext) {
        final LoggingContext context = getLoggingContext(executionContext);
        return context != null && context.proxyResponseHeaders();
    }

    public static boolean isProxyResponsePayloadsLoggable(final ExecutionContext executionContext) {
        final LoggingContext context = getLoggingContext(executionContext);
        return context != null && context.proxyResponsePayload();
    }

    private static LoggingContext getLoggingContext(final ExecutionContext executionContext) {
        return ((LoggingContext) executionContext.getAttribute(LoggingContext.LOGGING_ATTRIBUTE));
    }

    public static void appendBuffer(Buffer buffer, Buffer chunk, int maxLength) {
        if (maxLength != -1 && (buffer.length() + chunk.length()) > maxLength) {
            final int remainingSpace = maxLength - buffer.length();
            if (remainingSpace > 0) {
                buffer.appendBuffer(chunk, remainingSpace);
            }
        } else {
            buffer.appendBuffer(chunk);
        }
    }

    public static boolean isProxyLoggable(final ExecutionContext executionContext) {
        final LoggingContext context = getLoggingContext(executionContext);
        return context != null && context.proxyMode();
    }
}
