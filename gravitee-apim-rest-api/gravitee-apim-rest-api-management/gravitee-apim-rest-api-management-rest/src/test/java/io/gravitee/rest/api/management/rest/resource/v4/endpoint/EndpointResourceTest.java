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
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.plugin.core.api.PluginMoreInformation;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointResourceTest extends AbstractResourceTest {

    public static final String FAKE_ENDPOINT = "fake-endpoint";

    @Override
    protected String contextPath() {
        return "v4/endpoints/fake-endpoint";
    }

    @Before
    public void init() {
        reset(endpointConnectorPluginService);
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldGetEndpointSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT)).thenReturn(connectorPlugin);
        when(endpointConnectorPluginService.getSchema(FAKE_ENDPOINT)).thenReturn("schemaResponse");

        final Response response = envTarget().path("schema").request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final String result = response.readEntity(String.class);
        assertThat(result).isEqualTo("schemaResponse");
    }

    @Test
    public void shouldNotGetEndpointSchemaWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT)).thenThrow(new PluginNotFoundException(FAKE_ENDPOINT));

        final Response response = envTarget().path("schema").request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        final Map<String, Object> result = response.readEntity(Map.class);
        assertThat(result)
            .containsEntry("message", "Plugin [" + FAKE_ENDPOINT + "] cannot be found.")
            .containsEntry("parameters", Map.of("plugin", FAKE_ENDPOINT))
            .containsEntry("technicalCode", "plugin.notFound")
            .containsEntry("http_status", 404);
    }

    @Test
    public void shouldGetEndpointDocumentation() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT)).thenReturn(connectorPlugin);
        when(endpointConnectorPluginService.getDocumentation(FAKE_ENDPOINT)).thenReturn("documentationResponse");

        final Response response = envTarget().path("documentation").request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        final String result = response.readEntity(String.class);
        assertThat(result).isEqualTo("documentationResponse");
    }

    @Test
    public void shouldNotGetEndpointDocumentationWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT)).thenThrow(new PluginNotFoundException(FAKE_ENDPOINT));

        final Response response = envTarget().path("documentation").request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        final Map<String, Object> result = response.readEntity(Map.class);
        assertThat(result)
            .containsEntry("message", "Plugin [" + FAKE_ENDPOINT + "] cannot be found.")
            .containsEntry("parameters", Map.of("plugin", FAKE_ENDPOINT))
            .containsEntry("technicalCode", "plugin.notFound")
            .containsEntry("http_status", 404);
    }

    @Test
    public void shouldGetEndpointSharedConfigurationSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT)).thenReturn(connectorPlugin);
        when(endpointConnectorPluginService.getSharedConfigurationSchema(FAKE_ENDPOINT)).thenReturn("sharedSchemaResponse");

        final Response response = envTarget().path("shared-configuration-schema").request().get();
        final String result = response.readEntity(String.class);
        assertThat(result).isEqualTo("sharedSchemaResponse");
    }

    @Test
    public void shouldNotGetEndpointSharedConfigurationSchemaWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT);
        connectorPlugin.setName("Fake Endpoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT)).thenThrow(new PluginNotFoundException(FAKE_ENDPOINT));

        final Response response = envTarget().path("shared-configuration-schema").request().get();
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        final Map<String, Object> result = response.readEntity(Map.class);
        assertThat(result)
            .containsEntry("message", "Plugin [" + FAKE_ENDPOINT + "] cannot be found.")
            .containsEntry("parameters", Map.of("plugin", FAKE_ENDPOINT))
            .containsEntry("technicalCode", "plugin.notFound")
            .containsEntry("http_status", 404);
    }

    @Test
    public void shouldReturnMoreInformation() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENDPOINT);
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(endpointConnectorPluginService.findById(FAKE_ENDPOINT)).thenReturn(connectorPlugin);

        var moreInformation = new PluginMoreInformation();
        moreInformation.setDescription("A nice description");
        moreInformation.setSchemaImg("A nice schema");
        moreInformation.setDocumentationUrl("foobar");
        when(endpointConnectorPluginService.getMoreInformation(FAKE_ENDPOINT)).thenReturn(moreInformation);

        final Response response = envTarget().path("moreInformation").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var body = response.readEntity(PluginMoreInformation.class);
        assertEquals("A nice description", body.getDescription());
        assertEquals("A nice schema", body.getSchemaImg());
        assertEquals("foobar", body.getDocumentationUrl());
    }

    @Test
    public void shouldReturn500IfMoreInformationError() {
        doThrow(new TechnicalManagementException()).when(endpointConnectorPluginService).getMoreInformation(FAKE_ENDPOINT);

        final Response response = envTarget().path("moreInformation").request().get();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }
}
