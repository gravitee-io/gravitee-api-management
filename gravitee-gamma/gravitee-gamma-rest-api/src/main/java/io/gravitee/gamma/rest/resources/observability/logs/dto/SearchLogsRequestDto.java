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
package io.gravitee.gamma.rest.resources.observability.logs.dto;

import java.time.Instant;
import java.util.List;

/**
 * POST body for {@code /observability/logs/search}. Carries an ISO-8601 time window and a
 * (possibly empty) list of filter conditions. Pagination travels in the URL query string
 * ({@code ?page=&perPage=}) — same convention as the tracing and management v2 logs endpoints.
 *
 * <p>Unlike the tracing endpoint, there is no mandatory {@code apiId}: the logs search is
 * environment-wide with server-side RBAC scoping.
 */
public record SearchLogsRequestDto(TimeRangeDto timeRange, List<FilterConditionDto> filters) {
    /**
     * ISO-8601 time window. {@code null} on either bound defers to the use case's default
     * handling.
     */
    public record TimeRangeDto(Instant from, Instant to) {}
}
