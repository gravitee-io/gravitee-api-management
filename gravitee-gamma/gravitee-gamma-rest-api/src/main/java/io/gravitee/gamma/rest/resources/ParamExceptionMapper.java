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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.management.rest.model.ErrorEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ParamException;

/**
 * Answers a parameter Jersey could not convert with a 400 naming it, rather than letting it fall through to
 * the catch-all.
 *
 * <p>Jersey raises {@link ParamException} when a declared parameter cannot be built from what the caller
 * sent — {@code ?page=abc} on an {@code int}, say. It carries a 404 status of its own, which reads as
 * "Not Found" and says nothing about the parameter; without a mapper it reached {@code ThrowableMapper},
 * which answered 500 with the message {@code HTTP 404 Not Found} and logged an internal error. A caller's
 * typo was reported as a fault of the server, and the logs filled with it.
 *
 * <p>The same answer the management API v2 gives, in this API's error shape. Registered once for the whole
 * Gamma REST API — module plugins included, since {@code GammaModulesResource} dispatches them as
 * sub-resources of this application — so every resource benefits rather than each validating its own
 * parameters.
 *
 * <p>A malformed path parameter is answered 400 as well, not the 404 a path that matches no resource gets.
 * An id of the wrong shape is a bad request, and the two are already distinguishable: a well-formed id that
 * names nothing still answers 404. The management API v2 draws the line in the same place.
 *
 * @author GraviteeSource Team
 */
@Provider
public class ParamExceptionMapper implements ExceptionMapper<ParamException> {

    @Override
    public Response toResponse(ParamException e) {
        var error = new ErrorEntity(message(e), HttpStatusCode.BAD_REQUEST_400);
        return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(error).build();
    }

    /** Names what was refused and where it came from, which is what the caller needs to fix it. */
    private static String message(ParamException e) {
        return String.format("%s %s does not have a valid format", kindOf(e), e.getParameterName());
    }

    /**
     * Only query and path parameters are declared across the Gamma API and its modules; anything else falls
     * back to the generic wording rather than being named here before it exists.
     */
    private static String kindOf(ParamException e) {
        return switch (e) {
            case ParamException.QueryParamException ignored -> "Query parameter";
            case ParamException.PathParamException ignored -> "Path parameter";
            default -> "Parameter";
        };
    }
}
