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
package io.gravitee.gamma.module.platform.rest.resource.am;

import io.gravitee.gamma.module.platform.core.am.use_case.GetDomainUseCase;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.DomainResponse;
import io.gravitee.gamma.module.platform.rest.resource.mapper.am.AmDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DomainResource {

    @Inject
    private GetDomainUseCase getDomainUseCase;

    @Context
    private ResourceContext resourceContext;

    @GET
    public DomainResponse get(@PathParam("orgId") String orgId, @PathParam("envId") String envId, @PathParam("domainId") String domainId) {
        return AmCalls.run(() -> AmDtoMapper.toDto(getDomainUseCase.execute(new GetDomainUseCase.Input(orgId, envId, domainId)).domain()));
    }

    @Path("/entrypoints")
    public DomainEntrypointsResource entrypoints() {
        return resourceContext.getResource(DomainEntrypointsResource.class);
    }
}
