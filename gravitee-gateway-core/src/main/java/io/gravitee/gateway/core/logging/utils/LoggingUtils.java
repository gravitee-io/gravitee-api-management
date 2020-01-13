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

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;

import java.util.regex.Pattern;

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

    public static int getMaxSizeLogMessage(ExecutionContext executionContext) {
        try {
            return (int) executionContext.getAttribute(ExecutionContext.ATTR_PREFIX + "logging.max.size.log.message");
        } catch (Exception ex) {
            return -1;
        }
    }

    public static boolean isContentTypeLoggable(final String contentType, final ExecutionContext executionContext) {
        // init pattern
        if (EXCLUDED_CONTENT_TYPES_PATTERN == null) {
            try {
                final String responseTypes =
                        (String) executionContext.getAttribute(ExecutionContext.ATTR_PREFIX + "logging.response.excluded.types");
                EXCLUDED_CONTENT_TYPES_PATTERN = Pattern.compile(responseTypes);
            } catch (Exception e) {
                EXCLUDED_CONTENT_TYPES_PATTERN = Pattern.compile(DEFAULT_EXCLUDED_CONTENT_TYPES);
            }
        }

        return contentType == null || !EXCLUDED_CONTENT_TYPES_PATTERN.matcher(contentType).find();
    }

    public static void appendBuffer(Buffer buffer, Buffer chunk, int maxLength) {
        if ((buffer.length() + chunk.length()) > maxLength) {
            final int remainingSpace = maxLength - buffer.length();
            if (remainingSpace > 0) {
                buffer.appendBuffer(chunk, remainingSpace);
            }
        } else {
            buffer.appendBuffer(chunk);
        }
    }
}
