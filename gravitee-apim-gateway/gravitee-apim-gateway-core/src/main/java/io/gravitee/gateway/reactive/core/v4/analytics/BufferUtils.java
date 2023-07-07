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
package io.gravitee.gateway.reactive.core.v4.analytics;

import io.gravitee.gateway.api.buffer.Buffer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BufferUtils {

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
}
