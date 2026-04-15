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

/**
 * Query criteria for the ES {@code terms} (GROUP_BY) aggregation (US-03).
 *
 * @param size  max number of buckets to return (default 10, max 100)
 * @param order sort order for bucket counts — "ASC" or "DESC" (default "DESC")
 */
public record GroupByQueryCriteria(String apiId, String field, Instant from, Instant to, int size, String order) {
    public GroupByQueryCriteria {
        if (size <= 0) size = 10;
        if (order == null || order.isBlank()) order = "DESC";
    }
}
