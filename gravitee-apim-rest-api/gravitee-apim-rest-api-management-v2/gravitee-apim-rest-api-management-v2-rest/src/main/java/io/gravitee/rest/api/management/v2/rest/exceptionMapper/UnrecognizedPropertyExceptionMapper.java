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

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Provider
public class UnrecognizedPropertyExceptionMapper extends AbstractExceptionMapper<UnrecognizedPropertyException> {

    @Override
    public Response toResponse(UnrecognizedPropertyException e) {
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(
                Entity.json(
                    new Error()
                        .message(String.format("Property [%s] is not recognized as a valid property", e.getPropertyName()))
                        .httpStatus(Response.Status.BAD_REQUEST.getStatusCode())
                )
            )
            .build();
    }
}
