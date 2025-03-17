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
package io.gravitee.rest.api.portal.rest.resource.v4.endpoint;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.model.v4.connector.ConnectorExpandPluginEntity;
import io.gravitee.rest.api.portal.rest.mapper.ConnectorMapper;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.resource.v4.connector.AbstractConnectorsResource;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

/**
 * Defines the REST resources to manage endpoints.
 *
 * @author GraviteeSource Team
 */
public class EndpointsResource extends AbstractConnectorsResource {

    @Inject
    private EndpointConnectorPluginService endpointService;

    private final ConnectorMapper connectorMapper = ConnectorMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public Response getEndpoints(@QueryParam("expand") List<String> expands) {
        final Collection<ConnectorExpandPluginEntity> connectors = super.expand(endpointService.findAll(), expands);

        return createListResponse(
            GraviteeContext.getExecutionContext(),
            connectorMapper.convert(connectors),
            PaginationParam.builder().page(1).size(-1).build(),
            null,
            false
        );
    }

    @Override
    protected String getSchema(final String connectorId) {
        return endpointService.getSchema(connectorId);
    }

    @Override
    protected String getIcon(final String connectorId) {
        return endpointService.getIcon(connectorId);
    }
}
