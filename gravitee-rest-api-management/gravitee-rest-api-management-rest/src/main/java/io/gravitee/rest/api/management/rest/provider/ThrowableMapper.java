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
package io.gravitee.rest.api.management.rest.provider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@Provider
public class ThrowableMapper extends AbstractExceptionMapper<Throwable> {

    private static Logger LOGGER = LoggerFactory.getLogger(ThrowableMapper.class);

    @Override
    public Response toResponse(Throwable e) {
        LOGGER.error("Internal error", e);
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(convert(e, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()))
                .build();
    }
}
