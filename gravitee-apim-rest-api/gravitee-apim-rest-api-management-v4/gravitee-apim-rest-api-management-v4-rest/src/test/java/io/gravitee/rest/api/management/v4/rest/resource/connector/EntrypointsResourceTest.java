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
package io.gravitee.rest.api.management.v4.rest.resource.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.rest.api.management.v4.rest.model.ConnectorPlugin;
import io.gravitee.rest.api.management.v4.rest.model.ErrorEntity;
import io.gravitee.rest.api.management.v4.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointsResourceTest extends AbstractResourceTest {

    public static final String FAKE_ENTRYPOINT_ID = "fake-entrypoint";

    @Override
    protected String contextPath() {
        return "/entrypoints";
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
        connectorPlugin.setSupportedApiType(ApiType.ASYNC);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        connectorPlugin.setSupportedListenerType(ListenerType.HTTP);
        when(entrypointConnectorPluginService.findAll()).thenReturn(Set.of(connectorPlugin));

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final List<Map<String, String>> pluginEntities = response.readEntity(List.class);
        assertEquals(1, pluginEntities.size());
        Map<String, String> pluginEntity = pluginEntities.get(0);
        assertEquals("id", pluginEntity.get("id"));
        assertEquals("name", pluginEntity.get("name"));
        assertEquals("1.0", pluginEntity.get("version"));
        assertEquals(ApiType.ASYNC.getLabel(), pluginEntity.get("supportedApiType"));
        assertEquals(ListenerType.HTTP.getLabel(), pluginEntity.get("supportedListenerType"));
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(ConnectorMode.SUBSCRIBE.getLabel());
        assertEquals(arrayList, pluginEntity.get("supportedModes"));
    }

    @Test
    public void shouldGetEntrypointSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.ASYNC);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenReturn(connectorPlugin);
        when(entrypointConnectorPluginService.getSchema(FAKE_ENTRYPOINT_ID)).thenReturn("schemaResponse");

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("schema").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("schemaResponse");
    }

    @Test
    public void shouldNotGetEntrypointSchemaWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.ASYNC);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenThrow(new PluginNotFoundException(FAKE_ENTRYPOINT_ID));

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("schema").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        final ErrorEntity errorEntity = response.readEntity(ErrorEntity.class);

        final ErrorEntity expectedErrorEntity = new ErrorEntity();
        expectedErrorEntity.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedErrorEntity.setMessage("Plugin [" + FAKE_ENTRYPOINT_ID + "] can not be found.");
        expectedErrorEntity.setTechnicalCode("plugin.notFound");
        expectedErrorEntity.setParameters(Map.of("plugin", FAKE_ENTRYPOINT_ID));

        Assertions.assertThat(errorEntity).isEqualTo(expectedErrorEntity);
    }

    @Test
    public void shouldGetEntrypointDocumentation() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.ASYNC);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenReturn(connectorPlugin);
        when(entrypointConnectorPluginService.getDocumentation(FAKE_ENTRYPOINT_ID)).thenReturn("documentationResponse");

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("documentation").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("documentationResponse");
    }

    @Test
    public void shouldNotGetEntrypointDocumentationWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.ASYNC);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenThrow(new PluginNotFoundException(FAKE_ENTRYPOINT_ID));

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("documentation").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());

        final ErrorEntity errorEntity = response.readEntity(ErrorEntity.class);

        final ErrorEntity expectedErrorEntity = new ErrorEntity();
        expectedErrorEntity.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedErrorEntity.setMessage("Plugin [" + FAKE_ENTRYPOINT_ID + "] can not be found.");
        expectedErrorEntity.setTechnicalCode("plugin.notFound");
        expectedErrorEntity.setParameters(Map.of("plugin", FAKE_ENTRYPOINT_ID));

        Assertions.assertThat(errorEntity).isEqualTo(expectedErrorEntity);
    }

    @Test
    public void shouldGetEntrypointSubscriptionSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.ASYNC);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenReturn(connectorPlugin);
        when(entrypointConnectorPluginService.getSubscriptionSchema(FAKE_ENTRYPOINT_ID)).thenReturn("subscriptionSchemaResponse");

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("subscriptionSchema").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("subscriptionSchemaResponse");
    }

    @Test
    public void shouldNotGetEntrypointSubscriptionSchemaWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.ASYNC);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenThrow(new PluginNotFoundException(FAKE_ENTRYPOINT_ID));

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("subscriptionSchema").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        final ErrorEntity errorEntity = response.readEntity(ErrorEntity.class);

        final ErrorEntity expectedErrorEntity = new ErrorEntity();
        expectedErrorEntity.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedErrorEntity.setMessage("Plugin [" + FAKE_ENTRYPOINT_ID + "] can not be found.");
        expectedErrorEntity.setTechnicalCode("plugin.notFound");
        expectedErrorEntity.setParameters(Map.of("plugin", FAKE_ENTRYPOINT_ID));

        Assertions.assertThat(errorEntity).isEqualTo(expectedErrorEntity);
    }

    @Test
    public void shouldGetEndpointById() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.ASYNC);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenReturn(connectorPlugin);

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final ConnectorPlugin entrypoint = response.readEntity(ConnectorPlugin.class);
        assertNotNull(entrypoint);
        assertEquals(FAKE_ENTRYPOINT_ID, entrypoint.getId());
        assertEquals("Fake Entrypoint", entrypoint.getName());
        assertEquals("1.0", entrypoint.getVersion());
        assertEquals(io.gravitee.rest.api.management.v4.rest.model.ApiType.ASYNC, entrypoint.getSupportedApiType());
        assertEquals(Set.of(io.gravitee.rest.api.management.v4.rest.model.ConnectorMode.SUBSCRIBE), entrypoint.getSupportedModes());
    }
}
