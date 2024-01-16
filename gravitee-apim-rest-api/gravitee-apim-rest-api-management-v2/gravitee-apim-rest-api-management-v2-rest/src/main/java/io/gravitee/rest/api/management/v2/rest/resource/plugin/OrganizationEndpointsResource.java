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
package io.gravitee.rest.api.management.v2.rest.resource.plugin;

import io.gravitee.apim.core.plugin.use_case.GetEndpointPluginUseCase;
import io.gravitee.apim.core.plugin.use_case.GetEntrypointPluginUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ConnectorPluginMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.MoreInformationMapper;
import io.gravitee.rest.api.management.v2.rest.model.ConnectorPlugin;
import io.gravitee.rest.api.management.v2.rest.model.MoreInformation;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.Set;

public class OrganizationEndpointsResource extends AbstractResource {

    @Inject
    private GetEndpointPluginUseCase getEndpointPluginUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<ConnectorPlugin> getEndpoints() {
        String organizationId = GraviteeContext.getCurrentOrganization();
        return ConnectorPluginMapper.INSTANCE.mapCorePlugin(
            getEndpointPluginUseCase.getEndpointPluginsByOrganization(new GetEndpointPluginUseCase.Input(organizationId)).plugins()
        );
    }
}
