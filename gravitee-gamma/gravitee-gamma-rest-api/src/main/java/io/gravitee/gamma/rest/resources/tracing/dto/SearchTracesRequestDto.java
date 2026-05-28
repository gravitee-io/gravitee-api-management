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
package io.gravitee.gamma.rest.resources.tracing.dto;

import java.time.Instant;
import java.util.List;

/**
 * POST body for {@code /observability/traces/search}. Carries the API scope, the time window, and a
 * (possibly empty) list of filter conditions. Pagination travels in the URL query string
 * ({@code ?page=&perPage=}) — same convention as the apim management v2 {@code /logs/search}
 * endpoint.
 *
 * <p>{@code apiId} is required: per-API auth scope is enforced by the caller picking from APIs they
 * can see, and pinning to one apiId keeps each query bounded to a single resource-attribute term
 * regardless of how many APIs the caller has access to. Module isn't carried on the wire — every
 * apiId belongs to exactly one module, so the API id alone is sufficient to scope the query.
 */
public record SearchTracesRequestDto(String apiId, TimeRangeDto timeRange, List<FilterConditionDto> filters) {
    /**
     * ISO-8601 time window, matching apim's management v2 {@code TimeRange} schema. {@code null} on
     * either bound (or {@code null} TimeRange entirely) defers to the use case's "last 24h ending at
     * now" default.
     */
    public record TimeRangeDto(Instant from, Instant to) {}
}
