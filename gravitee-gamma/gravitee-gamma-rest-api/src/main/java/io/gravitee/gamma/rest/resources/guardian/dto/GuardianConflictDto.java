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
package io.gravitee.gamma.rest.resources.guardian.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import jakarta.ws.rs.core.Response;

/**
 * Body of the {@code 412} answered when a Guardian changed since the caller read it.
 *
 * <p>The standard error shape — {@code message} and {@code http_status} — plus what a client needs to
 * recover without a second request: the version to state next, also carried by the {@code ETag}, and the
 * Guardian as it is now.
 *
 * @author GraviteeSource Team
 */
public record GuardianConflictDto(
    String message,
    @JsonProperty("http_status") int httpStatus,
    Integer currentVersion,
    GuardianDto guardian
) {
    public static GuardianConflictDto from(String message, Guardian current) {
        return new GuardianConflictDto(
            message,
            Response.Status.PRECONDITION_FAILED.getStatusCode(),
            current.getVersion(),
            GuardianDto.from(current)
        );
    }
}
