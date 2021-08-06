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

import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConstraintValidationExceptionMapper extends AbstractExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException cve) {
        final Response.Status error = Response.Status.BAD_REQUEST;
        return Response.status(error).type(MediaType.APPLICATION_JSON_TYPE).entity(buildErrorList(cve)).build();
    }

    private ErrorResponse buildErrorList(ConstraintViolationException cve) {
        ErrorResponse response = new ErrorResponse();
        for (ConstraintViolation<?> violation : cve.getConstraintViolations()) {
            String detail = violation.getMessage();
            Object invalidValue = violation.getInvalidValue();
            if (invalidValue != null) {
                detail += "\n" + invalidValue;
            }
            Error error = new Error().code(violation.getPropertyPath().toString()).message(detail);
            response.addErrorsItem(error);
        }
        return response;
    }
}
