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
package io.gravitee.rest.api.management.v2.rest.exceptionMapper.domain;

import io.gravitee.apim.core.exception.NotFoundDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

public class NotFoundDomainExceptionMapper extends AbstractDomainExceptionMapper<NotFoundDomainException> {

    @Override
    public Response toResponse(NotFoundDomainException nfe) {
        return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(notFoundDomainError(nfe)).build();
    }

    private Error notFoundDomainError(NotFoundDomainException nfe) {
        return Error.builder()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message(nfe.getMessage())
            .parameters(Map.of("id", nfe.getId()))
            .build();
    }
}
