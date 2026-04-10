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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.ErrorDetailsInner;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.stream.Collectors;

@Provider
@Priority(1)
public class JsonMappingExceptionMapper extends AbstractExceptionMapper<JsonMappingException> {

    @Override
    public Response toResponse(JsonMappingException exception) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(buildError(exception)).build();
    }

    private Error buildError(JsonMappingException exception) {
        var error = new Error()
            .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
            .technicalCode("invalidValue")
            .message(sanitizeMessage(exception));

        var location = buildFieldLocation(exception);
        if (location != null) {
            error.details(List.of(new ErrorDetailsInner().message(sanitizeMessage(exception)).location(location)));
        }

        return error;
    }

    /**
     * When JSON deserialization fails on an invalid enum value, the exception message
     * includes the fully-qualified Java class name. We extract just the cause's message
     * to avoid exposing internal implementation details in API responses.
     */
    private String sanitizeMessage(JsonMappingException exception) {
        if (exception instanceof ValueInstantiationException && exception.getCause() != null) {
            return exception.getCause().getMessage();
        }
        return exception.getOriginalMessage();
    }

    private String buildFieldLocation(JsonMappingException exception) {
        var path = exception.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }
        return path
            .stream()
            .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
            .collect(Collectors.joining("."))
            .replace(".[", "[");
    }
}
