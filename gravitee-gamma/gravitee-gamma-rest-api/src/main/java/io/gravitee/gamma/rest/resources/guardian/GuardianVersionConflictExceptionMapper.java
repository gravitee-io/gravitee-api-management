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
package io.gravitee.gamma.rest.resources.guardian;

import io.gravitee.gamma.rest.core.guardian.exception.GuardianVersionConflictException;
import io.gravitee.gamma.rest.resources.guardian.dto.GuardianConflictDto;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps an update built on a stale read to {@code 412 Precondition Failed}.
 *
 * <p>412 rather than the 409 the exception's base type would get: the caller stated a precondition in
 * {@code If-Match} and it did not hold. The body carries the current Guardian and the response its
 * {@code ETag}, which the shared error shape cannot.
 *
 * @author GraviteeSource Team
 */
@Provider
public class GuardianVersionConflictExceptionMapper implements ExceptionMapper<GuardianVersionConflictException> {

    @Override
    public Response toResponse(GuardianVersionConflictException exception) {
        var current = exception.getCurrent();
        return GuardianEntityTag.withETagOnly(
            Response.status(Response.Status.PRECONDITION_FAILED)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(GuardianConflictDto.from(exception.getMessage(), current)),
            current
        ).build();
    }
}
