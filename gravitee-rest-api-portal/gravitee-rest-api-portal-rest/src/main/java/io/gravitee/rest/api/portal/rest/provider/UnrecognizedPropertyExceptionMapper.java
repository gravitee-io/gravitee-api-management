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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.portal.rest.model.Error;


/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Provider
public class UnrecognizedPropertyExceptionMapper extends AbstractExceptionMapper<UnrecognizedPropertyException> {
    
    @Override
    public Response toResponse(UnrecognizedPropertyException e) {
        Status status = Response.Status.BAD_REQUEST;
        
        return Response
                .status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new Error()
                        .code(Integer.toString(HttpStatusCode.BAD_REQUEST_400))
                        .detail(String.format("Property [%s] is not recognized as a valid property", e.getPropertyName()))
                        .title(status.getReasonPhrase())
                        )
                .build();
    }
}
