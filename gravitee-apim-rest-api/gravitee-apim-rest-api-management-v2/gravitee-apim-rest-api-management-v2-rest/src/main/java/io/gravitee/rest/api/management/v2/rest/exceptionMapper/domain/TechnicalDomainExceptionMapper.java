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

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class TechnicalDomainExceptionMapper extends AbstractDomainExceptionMapper<TechnicalDomainException> {

    @Override
    public Response toResponse(TechnicalDomainException ve) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(technicalDomainError(ve))
            .build();
    }

    private Error technicalDomainError(TechnicalDomainException tde) {
        return new Error().httpStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).message(tde.getMessage());
    }
}
