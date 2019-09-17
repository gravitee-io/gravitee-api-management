/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.provider;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConstraintValidationExceptionMapper extends AbstractExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException cve) {
        final Response.Status error = Response.Status.BAD_REQUEST;
        return Response
                .status(error)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ConstraintValidationError(cve))
                .build();
    }

    static class ConstraintValidationError {
        private final String message;

        private final String path;

        @JsonProperty("invalid_value")
        private final Object invalidValue;

        ConstraintValidationError(ConstraintViolationException cve) {
            ConstraintViolation<?> violation = cve.getConstraintViolations().iterator().next();
            this.message = violation.getMessage();
            this.path = violation.getPropertyPath().toString();
            this.invalidValue = violation.getInvalidValue();
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }

        public Object getInvalidValue() {
            return invalidValue;
        }
    }
}
