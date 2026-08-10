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
package io.gravitee.gamma.rest.resources.observability.dashboards.exception;

import io.gravitee.gamma.rest.core.observability.dashboard.exception.DashboardVersionConflictException;
import io.gravitee.gamma.rest.resources.observability.dashboards.DashboardEntityTag;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.DashboardConflictDto;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps a stale-version save to {@code 412 Precondition Failed}.
 *
 * <p>412 rather than 409 because the caller stated a precondition in {@code If-Match} and it did not
 * hold — which is what 412 means. 409 would describe the same event in a vocabulary the HTTP
 * mechanism we chose does not use, leaving a client that already understands conditional requests to
 * special-case this API.
 *
 * <p>The response carries the current revision's {@code ETag} — via {@link DashboardEntityTag}, like every other
 * response that names one — so a client whose user chooses to reload has the validator for its next save without a
 * second request.
 *
 * <p>Domain-specific rather than a generic mapper for the exception's base type, for two reasons.
 * The platform registers no {@code ConflictDomainException} mapper at all in
 * {@code GammaModuleApplication} — management-rest ships none — so without this a refused save would
 * surface through {@code ThrowableMapper} as a 500, telling the author their work failed for an
 * unknown reason. And the body has to carry the current dashboard, which the shared error shape
 * cannot express.
 *
 * @author GraviteeSource Team
 */
@Provider
public class DashboardVersionConflictExceptionMapper implements ExceptionMapper<DashboardVersionConflictException> {

    @Override
    public Response toResponse(DashboardVersionConflictException exception) {
        var current = exception.getCurrent();
        return DashboardEntityTag.withETagOnly(
            Response.status(Response.Status.PRECONDITION_FAILED)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(DashboardConflictDto.from(exception.getMessage(), current)),
            current
        ).build();
    }
}
