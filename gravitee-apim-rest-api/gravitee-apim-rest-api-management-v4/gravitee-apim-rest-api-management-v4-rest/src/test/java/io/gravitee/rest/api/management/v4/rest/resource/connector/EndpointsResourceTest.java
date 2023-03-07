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
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.rest.api.management.v4.rest.model.ConnectorPlugin;
import io.gravitee.rest.api.management.v4.rest.model.Endpoint;
import io.gravitee.rest.api.management.v4.rest.model.ErrorEntity;
import io.gravitee.rest.api.management.v4.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "/endpoints";
    }

    private static final String FAKE_ENDPOINT_ID = "my_endpoint";

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
        connectorPlugin.setSupportedQos(Set.of(Qos.AUTO));
        when(endpointConnectorPluginService.findAll()).thenReturn(Set.of(connectorPlugin));

        final Response response = rootTarget().request().get();
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
        ArrayList<String> supportedQos = new ArrayList<>(
            connectorPlugin.getSupportedQos().stream().map(qos -> qos.getLabel()).collect(Collectors.toList())
        );
        assertEquals(supportedQos, pluginEntity.get("supportedQos"));
    }

    @Test
    public void shouldGetEndpointSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT_ID);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT_ID)).thenReturn(connectorPlugin);
        when(endpointConnectorPluginService.getSchema(FAKE_ENDPOINT_ID)).thenReturn("schemaResponse");

        final Response response = rootTarget(FAKE_ENDPOINT_ID).path("schema").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("schemaResponse");
    }

    @Test
    public void shouldNotGetEndpointSchemaWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT_ID);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT_ID)).thenThrow(new PluginNotFoundException(FAKE_ENDPOINT_ID));

        final Response response = rootTarget(FAKE_ENDPOINT_ID).path("schema").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        final ErrorEntity errorEntity = response.readEntity(ErrorEntity.class);

        final ErrorEntity expectedErrorEntity = new ErrorEntity();
        expectedErrorEntity.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedErrorEntity.setMessage("Plugin [" + FAKE_ENDPOINT_ID + "] can not be found.");
        expectedErrorEntity.setTechnicalCode("plugin.notFound");
        expectedErrorEntity.setParameters(Map.of("plugin", FAKE_ENDPOINT_ID));

        Assertions.assertThat(errorEntity).isEqualTo(expectedErrorEntity);
    }

    @Test
    public void shouldGetEndpointDocumentation() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT_ID);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT_ID)).thenReturn(connectorPlugin);
        when(endpointConnectorPluginService.getDocumentation(FAKE_ENDPOINT_ID)).thenReturn("documentationResponse");

        final Response response = rootTarget(FAKE_ENDPOINT_ID).path("documentation").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("documentationResponse");
    }

    @Test
    public void shouldNotGetEndpointDocumentationWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT_ID);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT_ID)).thenThrow(new PluginNotFoundException(FAKE_ENDPOINT_ID));

        final Response response = rootTarget(FAKE_ENDPOINT_ID).path("documentation").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());

        final ErrorEntity errorEntity = response.readEntity(ErrorEntity.class);

        final ErrorEntity expectedErrorEntity = new ErrorEntity();
        expectedErrorEntity.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedErrorEntity.setMessage("Plugin [" + FAKE_ENDPOINT_ID + "] can not be found.");
        expectedErrorEntity.setTechnicalCode("plugin.notFound");
        expectedErrorEntity.setParameters(Map.of("plugin", FAKE_ENDPOINT_ID));

        Assertions.assertThat(errorEntity).isEqualTo(expectedErrorEntity);
    }

    @Test
    public void shouldGetEndpointById() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT_ID);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT_ID)).thenReturn(connectorPlugin);

        final Response response = rootTarget(FAKE_ENDPOINT_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final ConnectorPlugin endpoint = response.readEntity(ConnectorPlugin.class);
        assertNotNull(endpoint);
        assertEquals(FAKE_ENDPOINT_ID, endpoint.getId());
        assertEquals("Fake Endpoint", endpoint.getName());
        assertEquals("1.0", endpoint.getVersion());
        assertEquals(io.gravitee.rest.api.management.v4.rest.model.ApiType.MESSAGE, endpoint.getSupportedApiType());
        assertEquals(Set.of(io.gravitee.rest.api.management.v4.rest.model.ConnectorMode.SUBSCRIBE), endpoint.getSupportedModes());
    }
}
