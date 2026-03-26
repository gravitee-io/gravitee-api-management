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

import java.time.Instant;
import java.util.Optional;

/**
 * Query for GROUP_BY analytics: terms aggregation on a specified field.
 *
 * @param apiId the API identifier
 * @param field the field to group by (e.g., "status", "application", "plan")
 * @param size  max number of buckets to return
 * @param from  start of time range
 * @param to    end of time range
 */
public record GroupByQuery(String apiId, String field, int size, Instant from, Instant to) {
    public Optional<String> optionalApiId() {
        return Optional.ofNullable(apiId);
    }

    public Optional<Instant> optionalFrom() {
        return Optional.ofNullable(from);
    }

    public Optional<Instant> optionalTo() {
        return Optional.ofNullable(to);
    }
}
