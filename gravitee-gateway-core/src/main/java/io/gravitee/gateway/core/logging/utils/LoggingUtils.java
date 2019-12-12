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

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class LoggingUtils {

    public static int getMaxSizeLogMessage(ExecutionContext executionContext) {
        try {
            return (int) executionContext.getAttribute(ExecutionContext.ATTR_PREFIX + "logging.max.size.log.message");
        } catch (Exception ex) {
            return -1;
        }
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
