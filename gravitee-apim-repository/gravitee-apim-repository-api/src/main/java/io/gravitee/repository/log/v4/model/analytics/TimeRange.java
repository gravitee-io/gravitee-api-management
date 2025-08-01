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
package io.gravitee.repository.log.v4.model.analytics;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public record TimeRange(Instant from, Instant to, Optional<Duration> interval) {
    public TimeRange(java.time.Instant from, java.time.Instant to) {
        this(from, to, java.util.Optional.empty());
    }

    public TimeRange(java.time.Instant from, java.time.Instant to, java.time.Duration interval) {
        this(from, to, java.util.Optional.of(interval));
    }

    public long seconds() {
        return Math.max(1, (to.toEpochMilli() - from.toEpochMilli()) / 1000);
    }
}
