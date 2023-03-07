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
package io.gravitee.rest.api.management.rest.resource.v4.entrypoint;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
public class EntrypointsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "v4/entrypoints";
    }

    @Before
    public void init() {
        reset(entrypointConnectorPluginService);
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnAllEntrypoints() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId("id");
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        connectorPlugin.setSupportedListenerType(ListenerType.HTTP);
        when(entrypointConnectorPluginService.findAll()).thenReturn(Set.of(connectorPlugin));

        final Response response = envTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final List<Map<String, String>> pluginEntities = response.readEntity(List.class);
        assertEquals(1, pluginEntities.size());
        Map<String, String> pluginEntity = pluginEntities.get(0);
        assertEquals("id", pluginEntity.get("id"));
        assertEquals("name", pluginEntity.get("name"));
        assertEquals("1.0", pluginEntity.get("version"));
        assertEquals(ApiType.MESSAGE.getLabel(), pluginEntity.get("supportedApiType"));
        assertEquals(ListenerType.HTTP.getLabel(), pluginEntity.get("supportedListenerType"));
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(ConnectorMode.SUBSCRIBE.getLabel());
        assertEquals(arrayList, pluginEntity.get("supportedModes"));
    }

    @Test
    public void shouldReturnAllEntrypointsWithSchemaAndIcon() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId("id");
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findAll()).thenReturn(Set.of(connectorPlugin));
        when(entrypointConnectorPluginService.getSchema(connectorPlugin.getId())).thenReturn("schema");
        when(entrypointConnectorPluginService.getIcon(connectorPlugin.getId())).thenReturn("icon");

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
    public void shouldReturnAllEntrypointsWithSchemaAndSubscriptionSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId("id");
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findAll()).thenReturn(Set.of(connectorPlugin));
        when(entrypointConnectorPluginService.getSchema(connectorPlugin.getId())).thenReturn("schema");
        when(entrypointConnectorPluginService.getIcon(connectorPlugin.getId())).thenReturn("icon");
        when(entrypointConnectorPluginService.getSubscriptionSchema(connectorPlugin.getId())).thenReturn("subscriptionSchema");

        final Response response = envTarget()
            .queryParam("expand", "schema")
            .queryParam("expand", "subscriptionSchema")
            .queryParam("expand", "icon")
            .request()
            .get();
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
        assertEquals("subscriptionSchema", pluginEntity.get("subscriptionSchema"));
    }
}
