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
package io.gravitee.rest.api.management.rest.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConstraintValidationExceptionMapper extends AbstractExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException cve) {
        final Response.Status error = Response.Status.BAD_REQUEST;
        return Response.status(error).type(MediaType.APPLICATION_JSON_TYPE).entity(new ConstraintValidationError(cve)).build();
    }

    @Getter
    static class ConstraintValidationError {

        private final String message;

        private final String path;

        @JsonProperty("invalid_value")
        private final Object invalidValue;

        private final List<ConstraintViolationDetail> details;

        ConstraintValidationError(ConstraintViolationException cve) {
            ConstraintViolation<?> violation = cve.getConstraintViolations().iterator().next();
            this.message = violation.getMessage();
            this.path = violation.getPropertyPath().toString();
            this.invalidValue = violation.getInvalidValue();
            this.details = cve
                .getConstraintViolations()
                .stream()
                .map(constraintViolation ->
                    ConstraintViolationDetail.builder()
                        .message(constraintViolation.getMessage())
                        .location(extractLocation(constraintViolation))
                        .invalidValue(constraintViolation.getInvalidValue())
                        .build()
                )
                .toList();
        }

        private String extractLocation(ConstraintViolation<?> constraintViolation) {
            final String errorLocation = constraintViolation.getPropertyPath().toString();
            // getPropertyPath returns a location in the form of "methodName.methodParameter.fieldNameOfTheParameter.[...]". We are not interested by the method name and parameter, so we remove them.
            return errorLocation.substring(StringUtils.ordinalIndexOf(errorLocation, ".", 2) + 1);
        }
    }

    @Builder
    record ConstraintViolationDetail(String message, String location, Object invalidValue) {}
}
