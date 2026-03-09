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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.glassfish.jersey.server.ParamException;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ParamExceptionMapper extends AbstractExceptionMapper<ParamException> {

    @Override
    public Response toResponse(ParamException e) {
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(paramError(e)).build();
    }

    private static Error paramError(ParamException e) {
        return new Error().httpStatus(Response.Status.BAD_REQUEST.getStatusCode()).details(details(e)).message(errorMessage(e));
    }

    private static List<ErrorDetailsInner> details(ParamException e) {
        if (e.getCause() == null) {
            return List.of();
        }
        return List.of(new ErrorDetailsInner().message(e.getCause().getMessage()).location(e.getParameterName()));
    }

    private static String errorMessage(ParamException e) {
        return paramType(e) + " " + e.getParameterName() + " does not have a valid format";
    }

    private static String paramType(ParamException e) {
        return switch (e) {
            case ParamException.QueryParamException q -> "Query parameter";
            case ParamException.PathParamException p -> "Path parameter";
            case ParamException.FormParamException f -> "Form value";
            case ParamException.HeaderParamException h -> "Header";
            default -> "Parameter";
        };
    }
}
