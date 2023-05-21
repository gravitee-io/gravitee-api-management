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
package io.gravitee.rest.api.management.rest.resource.v4.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "v4/endpoints";
    }

    @Before
    public void init() {
        reset(endpointConnectorPluginService);
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnAllEndpoints() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId("id");
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findAll()).thenReturn(Set.of(connectorPlugin));

        final Response response = envTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final List<Map<String, Object>> pluginEntities = response.readEntity(List.class);
        assertThat(pluginEntities).hasSize(1);
        Map<String, Object> pluginEntity = pluginEntities.get(0);
        assertThat(pluginEntity)
            .containsEntry("id", "id")
            .containsEntry("name", "name")
            .containsEntry("version", "1.0")
            .containsEntry("supportedApiType", ApiType.MESSAGE.getLabel())
            .containsEntry("supportedModes", List.of(ConnectorMode.SUBSCRIBE.getLabel()));
    }

    @Test
    public void shouldReturnAllEndpointsWithSchemaAndIcon() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId("id");
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findAll()).thenReturn(Set.of(connectorPlugin));
        when(endpointConnectorPluginService.getSchema(connectorPlugin.getId())).thenReturn("schemaResponse");
        when(endpointConnectorPluginService.getIcon(connectorPlugin.getId())).thenReturn("iconResponse");

        final Response response = envTarget().queryParam("expand", "schema").queryParam("expand", "icon").request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final List<Map<String, Object>> pluginEntities = response.readEntity(List.class);
        assertThat(pluginEntities).hasSize(1);
        Map<String, Object> pluginEntity = pluginEntities.get(0);
        assertThat(pluginEntity)
            .containsEntry("id", "id")
            .containsEntry("name", "name")
            .containsEntry("version", "1.0")
            .containsEntry("supportedApiType", ApiType.MESSAGE.getLabel())
            .containsEntry("supportedModes", List.of(ConnectorMode.SUBSCRIBE.getLabel()))
            .containsEntry("schema", "schemaResponse")
            .containsEntry("icon", "iconResponse");
    }

    @Test
    public void shouldReturnAllEndpointsWithSchemaAndEndpointSharedConfigurationSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId("id");
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findAll()).thenReturn(Set.of(connectorPlugin));
        when(endpointConnectorPluginService.getSchema(connectorPlugin.getId())).thenReturn("schemaResponse");
        when(endpointConnectorPluginService.getIcon(connectorPlugin.getId())).thenReturn("iconResponse");
        when(endpointConnectorPluginService.getSharedConfigurationSchema(connectorPlugin.getId()))
            .thenReturn("sharedConfigurationSchemaResponse");

        final Response response = envTarget()
            .queryParam("expand", "schema")
            .queryParam("expand", "sharedConfigurationSchema")
            .queryParam("expand", "icon")
            .request()
            .get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final List<Map<String, Object>> pluginEntities = response.readEntity(List.class);
        assertThat(pluginEntities).hasSize(1);
        Map<String, Object> pluginEntity = pluginEntities.get(0);
        assertThat(pluginEntity)
            .containsEntry("id", "id")
            .containsEntry("name", "name")
            .containsEntry("version", "1.0")
            .containsEntry("supportedApiType", ApiType.MESSAGE.getLabel())
            .containsEntry("supportedModes", List.of(ConnectorMode.SUBSCRIBE.getLabel()))
            .containsEntry("schema", "schemaResponse")
            .containsEntry("icon", "iconResponse")
            .containsEntry("sharedConfigurationSchema", "sharedConfigurationSchemaResponse");
    }
}
