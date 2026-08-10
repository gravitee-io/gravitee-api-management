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
package io.gravitee.gamma.rest.resources.observability.dashboards.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.gamma.rest.core.observability.dashboard.model.Dashboard;
import jakarta.ws.rs.core.Response;

/**
 * Body of the {@code 412} returned when a dashboard was modified since the caller read it.
 *
 * <p>Extends the standard error shape ({@code message} + {@code http_status}, as produced by the
 * platform's {@code ErrorEntity}) rather than replacing it, so a client with generic error handling
 * still gets a usable message, while one that handles the refusal properly finds everything it needs
 * to offer overwrite / reload / save-as-a-copy without a second round trip.
 *
 * <p>{@code currentVersion} duplicates what the response's {@code ETag} already carries. That is
 * deliberate: the ETag is what a client feeds back into {@code If-Match}, while this stays readable
 * to anything that only ever parses bodies — and to a human reading the response.
 */
public record DashboardConflictDto(
    String message,
    @JsonProperty("http_status") int httpStatus,
    Integer currentVersion,
    DashboardDto dashboard
) {
    public static DashboardConflictDto from(String message, Dashboard current) {
        return new DashboardConflictDto(
            message,
            Response.Status.PRECONDITION_FAILED.getStatusCode(),
            current.version(),
            DashboardDto.from(current)
        );
    }
}
