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

import io.gravitee.gamma.module.platform.core.am.use_case.connection.GetAmConnectionUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.SaveAmConnectionUseCase;
import io.gravitee.gamma.module.platform.core.am.use_case.connection.TestAmConnectionUseCase;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.AmConnectionRequest;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.AmConnectionResponse;
import io.gravitee.gamma.module.platform.rest.resource.dto.am.AmDtos.AmConnectionTestResultResponse;
import io.gravitee.gamma.module.platform.rest.resource.mapper.am.AmDtoMapper;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AmConnectionResource {

    @Inject
    private GetAmConnectionUseCase getAmConnectionUseCase;

    @Inject
    private SaveAmConnectionUseCase saveAmConnectionUseCase;

    @Inject
    private TestAmConnectionUseCase testAmConnectionUseCase;

    @GET
    public AmConnectionResponse get(@PathParam("orgId") String orgId) {
        var output = getAmConnectionUseCase.execute(new GetAmConnectionUseCase.Input(orgId));
        return new AmConnectionResponse(
            output.baseUrl(),
            output.hasAccessToken(),
            output.environmentId(),
            output.defaultDomainId(),
            output.defaultDomainHrid(),
            output.gatewayUrl()
        );
    }

    @PUT
    public AmConnectionResponse save(@PathParam("orgId") String orgId, @Valid @NotNull AmConnectionRequest req) {
        var output = saveAmConnectionUseCase.execute(
            new SaveAmConnectionUseCase.Input(
                orgId,
                req.baseUrl(),
                req.serviceAccountAccessToken(),
                req.environmentId(),
                req.defaultDomainId(),
                req.defaultDomainHrid(),
                req.gatewayUrl()
            )
        );
        return new AmConnectionResponse(
            output.baseUrl(),
            output.hasAccessToken(),
            output.environmentId(),
            output.defaultDomainId(),
            output.defaultDomainHrid(),
            output.gatewayUrl()
        );
    }

    @POST
    @Path("/_test")
    public AmConnectionTestResultResponse test(@PathParam("orgId") String orgId, AmConnectionRequest req) {
        String inboundBaseUrl = req == null ? null : req.baseUrl();
        String inboundToken = req == null ? null : req.serviceAccountAccessToken();
        return AmDtoMapper.toDto(
            testAmConnectionUseCase.execute(new TestAmConnectionUseCase.Input(orgId, inboundBaseUrl, inboundToken)).result()
        );
    }
}
