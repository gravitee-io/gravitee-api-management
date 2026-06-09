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

import io.gravitee.gamma.module.platform.core.am.use_case.ListDomainsUseCase;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.DomainResponse;
import io.gravitee.gamma.module.platform.rest.resource.mapper.am.AmDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DomainsResource {

    @Inject
    private ListDomainsUseCase listDomainsUseCase;

    @Context
    private ResourceContext resourceContext;

    @GET
    public List<DomainResponse> list(@PathParam("orgId") String orgId, @PathParam("envId") String envId, @QueryParam("q") String q) {
        return AmCalls.run(() ->
            listDomainsUseCase.execute(new ListDomainsUseCase.Input(orgId, envId, q)).domains().stream().map(AmDtoMapper::toDto).toList()
        );
    }

    @Path("/{domainId}")
    public DomainResource domain() {
        return resourceContext.getResource(DomainResource.class);
    }
}
