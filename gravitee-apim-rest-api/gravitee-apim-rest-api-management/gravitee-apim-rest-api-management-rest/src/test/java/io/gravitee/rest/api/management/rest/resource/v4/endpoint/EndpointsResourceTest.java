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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.plugin.core.api.PluginMoreInformation;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
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
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final List<Map<String, String>> pluginEntities = response.readEntity(List.class);
        assertEquals(1, pluginEntities.size());
        Map<String, String> pluginEntity = pluginEntities.get(0);
        assertEquals("id", pluginEntity.get("id"));
        assertEquals("name", pluginEntity.get("name"));
        assertEquals("1.0", pluginEntity.get("version"));
        assertEquals(ApiType.MESSAGE.getLabel(), pluginEntity.get("supportedApiType"));
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(ConnectorMode.SUBSCRIBE.getLabel());
        assertEquals(arrayList, pluginEntity.get("supportedModes"));
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
        when(endpointConnectorPluginService.getSchema(connectorPlugin.getId())).thenReturn("schema");
        when(endpointConnectorPluginService.getIcon(connectorPlugin.getId())).thenReturn("icon");

        final Response response = envTarget().queryParam("expand", "schema").queryParam("expand", "icon").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final List<Map<String, String>> pluginEntities = response.readEntity(List.class);
        assertEquals(1, pluginEntities.size());
        Map<String, String> pluginEntity = pluginEntities.get(0);
        assertEquals("id", pluginEntity.get("id"));
        assertEquals("name", pluginEntity.get("name"));
        assertEquals("1.0", pluginEntity.get("version"));
        assertEquals(ApiType.MESSAGE.getLabel(), pluginEntity.get("supportedApiType"));
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(ConnectorMode.SUBSCRIBE.getLabel());
        assertEquals(arrayList, pluginEntity.get("supportedModes"));
        assertEquals("schema", pluginEntity.get("schema"));
        assertEquals("icon", pluginEntity.get("icon"));
    }

    @Test
    public void shouldReturnMoreInformation() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId("id");
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById("id")).thenReturn(connectorPlugin);

        var moreInformation = new PluginMoreInformation();
        moreInformation.setDescription("A nice description");
        moreInformation.setSchemaImg("A nice schema");
        moreInformation.setDocumentationUrl("foobar");
        when(endpointConnectorPluginService.getMoreInformation("id")).thenReturn(moreInformation);

        final Response response = envTarget().path("id").path("/moreInformation").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var body = response.readEntity(PluginMoreInformation.class);
        assertEquals("A nice description", body.getDescription());
        assertEquals("A nice schema", body.getSchemaImg());
        assertEquals("foobar", body.getDocumentationUrl());
    }

    @Test
    public void shouldReturn500IfMoreInformationError() {
        doThrow(new TechnicalManagementException()).when(endpointConnectorPluginService).getMoreInformation("id");

        final Response response = envTarget().path("id").path("/moreInformation").request().get();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }
}
