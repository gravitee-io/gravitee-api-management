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
package io.gravitee.gateway.jupiter.handlers.api.v4.analytics;

import io.gravitee.gateway.jupiter.api.message.Message;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
public class MessageCounters {

    private AtomicInteger atomicMessageCount = new AtomicInteger(0);
    private AtomicInteger atomicErrorCount = new AtomicInteger(0);

    public Counters increment(final Message message) {
        int count = atomicMessageCount.incrementAndGet();
        int errorCount = -1;
        if (message.error()) {
            errorCount = atomicErrorCount.incrementAndGet();
        }
        return new Counters(count, errorCount);
    }

    @RequiredArgsConstructor
    @Getter
    @Accessors(fluent = true)
    public static class Counters {

        private final int messageCount;
        private final int errorCount;
    }
}
