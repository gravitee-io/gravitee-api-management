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
package io.gravitee.gamma.rest.resources;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.model.ErrorEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;

/**
 * Answers an unknown key of a {@link RejectUnknownProperties} body with the management API's 400, plus the technical
 * code the refused type declares, when it declares one.
 *
 * @author GraviteeSource Team
 */
@Provider
public class UnknownPropertyExceptionMapper implements ExceptionMapper<UnrecognizedPropertyException> {

    @Override
    public Response toResponse(UnrecognizedPropertyException e) {
        var error = new ErrorEntity(
            String.format("Property [%s] is not recognized as a valid property", e.getPropertyName()),
            HttpStatusCode.BAD_REQUEST_400
        );
        technicalCodeOf(e).ifPresent(error::setTechnicalCode);
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(error).build();
    }

    private static Optional<String> technicalCodeOf(UnrecognizedPropertyException e) {
        return Optional.ofNullable(e.getReferringClass())
            .map(type -> type.getAnnotation(RejectUnknownProperties.class))
            .map(RejectUnknownProperties::technicalCode)
            .filter(code -> !code.isEmpty());
    }
}
