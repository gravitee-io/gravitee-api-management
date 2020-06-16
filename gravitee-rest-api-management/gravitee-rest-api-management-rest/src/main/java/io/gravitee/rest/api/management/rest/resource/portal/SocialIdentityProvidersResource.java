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
package io.gravitee.rest.api.management.rest.resource.portal;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Defines the service to retrieve the social authentication providers from the portal.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Portal", "Authentication", "Identity Providers"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SocialIdentityProvidersResource extends AbstractResource {

    @Inject
    private SocialIdentityProviderService socialIdentityProviderService;

    @Inject
    private IdentityProviderActivationService identityProviderActivationService;

    @GET
    @ApiOperation(value = "Get the list of social identity providers")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List social identity providers", response = SocialIdentityProviderEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public List<SocialIdentityProviderEntity> getSocialIdentityProviders() {
        Set<String> allIdpByTarget = identityProviderActivationService.findAllByTarget(new IdentityProviderActivationService.ActivationTarget(GraviteeContext.getCurrentOrganization(), IdentityProviderActivationReferenceType.ORGANIZATION))
                .stream()
                .map(IdentityProviderActivationEntity::getIdentityProvider)
                .collect(Collectors.toSet());

        return socialIdentityProviderService.findAll(false)
                .stream()
                .filter(idp -> allIdpByTarget.contains(idp.getId()))
                .sorted((idp1, idp2) -> String.CASE_INSENSITIVE_ORDER.compare(idp1.getName(), idp2.getName()))
                .collect(Collectors.toList());
    }
}
