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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.RestrictedDomainEntity;
import io.gravitee.rest.api.service.AccessPointService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import java.util.Collection;

/**
 * Defines the REST resources to manage RestrictedDomain.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Restricted Domains")
public class RestrictedDomainsResource {

    @Inject
    private AccessPointService accessPointService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the restricted domain")
    @ApiResponse(
        responseCode = "200",
        description = "Restricted domains for the environment",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestrictedDomainEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Collection<RestrictedDomainEntity> getRestrictedDomains() {
        return accessPointService.getGatewayRestrictedDomains(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment()
        );
    }
}
