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
package io.gravitee.repository.log.v4.model.connection;

import java.util.Collection;
import java.util.Objects;

/**
 * Query model for searching distinct error keys from the connection logs of the given APIs.
 *
 * <p>{@code apiIds} is required: an empty collection matches no log, it never widens the search to every API.
 * {@code maxBuckets} controls how many distinct error
 * keys Elasticsearch will return from the terms aggregation — callers should use a
 * value appropriate to their use case. The default is {@link #DEFAULT_MAX_BUCKETS}.
 *
 * <p>Error key types are categorical (e.g. {@code ASSIGN_CONTENT_ERROR}, {@code TIMEOUT})
 * so the number of distinct values is expected to be small in practice.
 */
public record SearchConnectionLogErrorKeysQuery(Collection<String> apiIds, Long from, Long to, int maxBuckets) {
    public static final int DEFAULT_MAX_BUCKETS = 100;

    /**
     * Convenience factory using the default bucket cap.
     */
    public static SearchConnectionLogErrorKeysQuery of(Collection<String> apiIds, Long from, Long to) {
        return new SearchConnectionLogErrorKeysQuery(apiIds, from, to, DEFAULT_MAX_BUCKETS);
    }

    public SearchConnectionLogErrorKeysQuery {
        Objects.requireNonNull(apiIds, "apiIds must not be null");
        if (maxBuckets <= 0) {
            throw new IllegalArgumentException("maxBuckets must be positive");
        }
    }
}
