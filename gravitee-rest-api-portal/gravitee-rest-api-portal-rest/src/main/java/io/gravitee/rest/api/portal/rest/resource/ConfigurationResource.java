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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.mapper.ConfigurationMapper;
import io.gravitee.rest.api.portal.rest.mapper.IdentityProviderMapper;
import io.gravitee.rest.api.portal.rest.model.IdentityProvider;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.SocialIdentityProviderService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConfigurationResource extends AbstractResource {

    @Autowired
    private ConfigService configService;

    @Autowired
    private SocialIdentityProviderService socialIdentityProviderService;

    @Autowired
    private ConfigurationMapper configMapper;

    @Autowired
    private IdentityProviderMapper identityProviderMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalConfiguration() {
        return Response.ok(configMapper.convert(configService.getPortalConfig())).build();
    }

    @GET
    @Path("identities")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortalIdentityProviders(@BeanParam PaginationParam paginationParam) {

        List<IdentityProvider> identities = socialIdentityProviderService.findAll().stream()
                .sorted((idp1, idp2) -> String.CASE_INSENSITIVE_ORDER.compare(idp1.getName(), idp2.getName()))
                .map(identityProviderMapper::convert)
                .collect(Collectors.toList());
        return createListResponse(identities, paginationParam);
    }
}
