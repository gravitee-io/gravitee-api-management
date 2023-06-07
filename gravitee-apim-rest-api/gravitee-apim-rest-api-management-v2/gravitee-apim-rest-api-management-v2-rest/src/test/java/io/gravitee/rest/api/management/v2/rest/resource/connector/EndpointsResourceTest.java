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
package io.gravitee.rest.api.management.v2.rest.resource.connector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "/plugins/endpoints";
    }

    private static final String FAKE_ENDPOINT_ID = "my_endpoint";

    @Before
    public void init() {
        reset(endpointConnectorPluginService);
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnEndpoints() {
        ConnectorPluginEntity connectorPlugin = getConnectorPluginEntity();
        when(endpointConnectorPluginService.findAll()).thenReturn(Set.of(connectorPlugin));

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check response content
        final Set<ConnectorPlugin> connectorPlugins = response.readEntity(new GenericType<>() {});

        // Check data
        ConnectorPlugin pluginEntity1 = new ConnectorPlugin()
            .id("id")
            .name("name")
            .version("1.0")
            .icon("my-icon")
            .supportedApiType(io.gravitee.rest.api.management.v2.rest.model.ApiType.MESSAGE)
            .supportedModes(Set.of(io.gravitee.rest.api.management.v2.rest.model.ConnectorMode.SUBSCRIBE))
            .supportedQos(Set.of(io.gravitee.rest.api.management.v2.rest.model.Qos.AUTO));

        assertEquals(Set.of(pluginEntity1), connectorPlugins);
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
        final Error error = response.readEntity(Error.class);

        final Error expectedError = new Error();
        expectedError.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedError.setMessage("Plugin [" + FAKE_ENDPOINT_ID + "] cannot be found.");
        expectedError.setTechnicalCode("plugin.notFound");
        expectedError.setParameters(Map.of("plugin", FAKE_ENDPOINT_ID));

        Assertions.assertThat(error).isEqualTo(expectedError);
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

        final Error error = response.readEntity(Error.class);

        final Error expectedError = new Error();
        expectedError.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedError.setMessage("Plugin [" + FAKE_ENDPOINT_ID + "] cannot be found.");
        expectedError.setTechnicalCode("plugin.notFound");
        expectedError.setParameters(Map.of("plugin", FAKE_ENDPOINT_ID));

        Assertions.assertThat(error).isEqualTo(expectedError);
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
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.ApiType.MESSAGE, endpoint.getSupportedApiType());
        assertEquals(Set.of(io.gravitee.rest.api.management.v2.rest.model.ConnectorMode.SUBSCRIBE), endpoint.getSupportedModes());
    }

    @Test
    public void shouldGetEndpointSharedConfigurationSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT_ID);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT_ID)).thenReturn(connectorPlugin);
        when(endpointConnectorPluginService.getSharedConfigurationSchema(FAKE_ENDPOINT_ID)).thenReturn("sharedConfigurationSchemaResponse");

        final Response response = rootTarget(FAKE_ENDPOINT_ID).path("shared-configuration-schema").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("sharedConfigurationSchemaResponse");
    }

    @Test
    public void shouldNotGetSharedConfigurationSchemaWhenPluginNotFound() {
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT_ID)).thenThrow(new PluginNotFoundException(FAKE_ENDPOINT_ID));

        final Response response = rootTarget(FAKE_ENDPOINT_ID).path("shared-configuration-schema").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        final Error error = response.readEntity(Error.class);

        final Error expectedError = new Error();
        expectedError.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedError.setMessage("Plugin [" + FAKE_ENDPOINT_ID + "] cannot be found.");
        expectedError.setTechnicalCode("plugin.notFound");
        expectedError.setParameters(Map.of("plugin", FAKE_ENDPOINT_ID));

        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @NotNull
    private ConnectorPluginEntity getConnectorPluginEntity() {
        return getConnectorPluginEntity("id");
    }

    @NotNull
    private ConnectorPluginEntity getConnectorPluginEntity(String id) {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(id);
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setIcon("my-icon");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        connectorPlugin.setSupportedQos(Set.of(Qos.AUTO));
        return connectorPlugin;
    }
}
