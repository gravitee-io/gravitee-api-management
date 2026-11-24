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
package io.gravitee.gamma.rest.resources.guardrail.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import jakarta.ws.rs.core.Response;

/**
 * Body of the {@code 412} answered when a Guardrail changed since the caller read it.
 *
 * <p>The standard error shape — {@code message} and {@code http_status} — plus what a client needs to
 * recover without a second request: the version to state next, also carried by the {@code ETag}, and the
 * Guardrail as it is now.
 *
 * @author GraviteeSource Team
 */
public record GuardrailConflictDto(
    String message,
    @JsonProperty("http_status") int httpStatus,
    Integer currentVersion,
    GuardrailDto guardrail
) {
    public static GuardrailConflictDto from(String message, Guardrail current) {
        return new GuardrailConflictDto(
            message,
            Response.Status.PRECONDITION_FAILED.getStatusCode(),
            current.getVersion(),
            GuardrailDto.from(current)
        );
    }
}
