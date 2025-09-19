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
package io.gravitee.rest.api.management.v2.rest.exceptionMapper;

import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.ErrorDetailsInner;
import io.gravitee.rest.api.service.exceptions.AbstractValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ValidationExceptionMapper extends AbstractExceptionMapper<AbstractValidationException> {

    @Override
    public Response toResponse(AbstractValidationException ve) {
        return Response.status(Response.Status.fromStatusCode(ve.getHttpStatusCode()))
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(validationError(ve))
            .build();
    }

    private Error validationError(AbstractValidationException ve) {
        return Error.builder().httpStatus(ve.getHttpStatusCode()).message(ve.getMessage()).details(buildDetails(ve)).build();
    }

    private List<ErrorDetailsInner> buildDetails(AbstractValidationException exception) {
        return exception
            .getConstraints()
            .entrySet()
            .stream()
            .map(entry ->
                ErrorDetailsInner.builder()
                    .message(exception.getDetailMessage())
                    .invalidValue(JsonNullable.of(entry.getKey()))
                    .location(entry.getValue())
                    .build()
            )
            .collect(Collectors.toList());
    }
}
