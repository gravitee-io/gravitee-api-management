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
package io.gravitee.rest.api.management.v2.rest.exceptionmapper.domain;

import io.gravitee.apim.core.portal.exception.PortalLimitReachedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class PortalLimitReachedExceptionMapper extends AbstractDomainExceptionMapper<PortalLimitReachedException> {

    @Override
    public Response toResponse(PortalLimitReachedException exception) {
        return Response.status(Response.Status.FORBIDDEN)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(convert(exception, Response.Status.FORBIDDEN.getStatusCode(), exception.getTechnicalCode(), exception.getParameters()))
            .build();
    }
}
