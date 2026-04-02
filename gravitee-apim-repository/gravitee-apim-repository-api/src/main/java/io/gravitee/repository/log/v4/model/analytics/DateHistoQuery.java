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
import lombok.Builder;

/**
 * Query parameters for a date-histogram analytics request.
 *
 * <p>Mirrors the Optional-wrapping pattern used by {@link StatsQuery} and {@link GroupByQuery}.</p>
 */
@Builder(toBuilder = true)
public record DateHistoQuery(Optional<String> apiId, String field, Duration interval, Optional<Instant> from, Optional<Instant> to) {
    /**
     * Convenience constructor used by service and test code — wraps nullable arguments.
     */
    public DateHistoQuery(String apiId, String field, Duration interval, Instant from, Instant to) {
        this(Optional.ofNullable(apiId), field, interval, Optional.ofNullable(from), Optional.ofNullable(to));
    }
}
