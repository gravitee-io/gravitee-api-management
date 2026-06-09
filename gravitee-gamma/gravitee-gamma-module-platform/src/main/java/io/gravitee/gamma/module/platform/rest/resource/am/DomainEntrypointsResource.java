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

import io.gravitee.gamma.module.platform.core.am.use_case.ListDomainEntrypointsUseCase;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.GatewayEntrypointResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DomainEntrypointsResource {

    @Inject
    private ListDomainEntrypointsUseCase listDomainEntrypointsUseCase;

    @GET
    public List<GatewayEntrypointResponse> list(
        @PathParam("orgId") String orgId,
        @PathParam("envId") String envId,
        @PathParam("domainId") String domainId
    ) {
        return AmCalls.run(() ->
            listDomainEntrypointsUseCase
                .execute(new ListDomainEntrypointsUseCase.Input(orgId, envId, domainId))
                .entrypoints()
                .stream()
                .map(e -> new GatewayEntrypointResponse(e.id(), e.name(), e.url(), e.defaultEntrypoint()))
                .toList()
        );
    }
}
