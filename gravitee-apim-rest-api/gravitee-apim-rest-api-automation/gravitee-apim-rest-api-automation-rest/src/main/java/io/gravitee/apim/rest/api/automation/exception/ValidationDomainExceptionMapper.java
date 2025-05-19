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
package io.gravitee.apim.rest.api.automation.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.rest.api.management.v2.rest.exceptionMapper.domain.AbstractDomainExceptionMapper;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class ValidationDomainExceptionMapper extends AbstractDomainExceptionMapper<ValidationDomainException> {

    @Override
    public Response toResponse(ValidationDomainException ve) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(validationDomainError(ve)).build();
    }

    private Error validationDomainError(ValidationDomainException vde) {
        return new Error()
            .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
            .technicalCode("invalid_domain")
            .message(vde.getMessage());
    }
}
