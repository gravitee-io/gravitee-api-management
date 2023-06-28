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
package io.gravitee.rest.api.portal.rest.resource.v4.entrypoint;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.model.v4.connector.ConnectorExpandPluginEntity;
import io.gravitee.rest.api.portal.rest.resource.v4.connector.AbstractConnectorsResource;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.Collection;
import java.util.List;

/**
 * Defines the REST resources to manage entrypoints.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointsResource extends AbstractConnectorsResource {

    @Inject
    private EntrypointConnectorPluginService entrypointService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public Collection<ConnectorExpandPluginEntity> getEntrypoints(@QueryParam("expand") List<String> expands) {
        final Collection<ConnectorExpandPluginEntity> connectors = super.expand(entrypointService.findAll(), expands);
        if (expands != null && expands.contains("subscriptionSchema")) {
            connectors.forEach(connector ->
                connector.setSubscriptionSchema(
                    entrypointService.getSubscriptionSchema(connector.getId(), SchemaDisplayFormat.GV_SCHEMA_FORM)
                )
            );
        }
        return connectors;
    }

    @Override
    protected String getSchema(final String connectorId) {
        return entrypointService.getSchema(connectorId);
    }

    @Override
    protected String getIcon(final String connectorId) {
        return entrypointService.getIcon(connectorId);
    }
}
