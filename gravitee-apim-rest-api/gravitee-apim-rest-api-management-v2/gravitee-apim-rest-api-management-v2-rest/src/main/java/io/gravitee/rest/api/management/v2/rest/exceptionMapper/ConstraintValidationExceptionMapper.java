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
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConstraintValidationExceptionMapper extends AbstractExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException cve) {
        final Response.Status error = Response.Status.BAD_REQUEST;
        return Response.status(error).type(MediaType.APPLICATION_JSON_TYPE).entity(validationError(cve)).build();
    }

    private Object validationError(ConstraintViolationException cve) {
        return Error.builder()
            .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
            .message("Validation error")
            .details(buildDetails(cve))
            .build();
    }

    private List<ErrorDetailsInner> buildDetails(ConstraintViolationException exception) {
        return exception
            .getConstraintViolations()
            .stream()
            .map(constraintViolation -> {
                String errorLocation = constraintViolation.getPropertyPath().toString();
                return ErrorDetailsInner.builder()
                    .message(constraintViolation.getMessage())
                    // getPropertyPath returns a location in the form of "methodName.methodParameter.fieldNameOfTheParameter.[...]". We are not interested by the method name and parameter, so we remove them.
                    .location(errorLocation.substring(StringUtils.ordinalIndexOf(errorLocation, ".", 2) + 1))
                    .invalidValue(JsonNullable.of(constraintViolation.getInvalidValue()))
                    .build();
            })
            .collect(Collectors.toList());
    }
}
