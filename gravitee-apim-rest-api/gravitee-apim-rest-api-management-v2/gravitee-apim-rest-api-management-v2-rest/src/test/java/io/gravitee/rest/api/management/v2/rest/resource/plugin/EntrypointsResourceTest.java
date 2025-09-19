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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import inmemory.EntrypointPluginQueryServiceInMemory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.management.v2.rest.model.ConnectorPlugin;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointsResourceTest extends AbstractResourceTest {

    @Autowired
    private EntrypointPluginQueryServiceInMemory entrypointPluginQueryServiceInMemory;

    @Autowired
    private LicenseManager licenseManager;

    public static final String FAKE_ENTRYPOINT_ID = "fake-entrypoint";

    @Override
    protected String contextPath() {
        return "/plugins/entrypoints";
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        reset(entrypointConnectorPluginService);
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldReturnEntrypoints() {
        var license = mock(License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(license);
        when(license.isFeatureEnabled("feature-id-1")).thenReturn(true);
        when(license.isFeatureEnabled("feature-id-2")).thenReturn(true);
        when(license.isFeatureEnabled("feature-id-3")).thenReturn(false);

        var plugins = List.of(getConnectorPlugin("id-1"), getConnectorPlugin("id-2"), getConnectorPlugin("id-3"));
        entrypointPluginQueryServiceInMemory.initWith(plugins);

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check response content
        final Set<ConnectorPlugin> connectorPlugins = response.readEntity(new GenericType<>() {});

        // Check data
        ConnectorPlugin pluginEntity1 = new ConnectorPlugin()
            .id("id-1")
            .name("name")
            .version("1.0")
            .icon("my-icon")
            .supportedApiType(io.gravitee.rest.api.management.v2.rest.model.ApiType.MESSAGE)
            .supportedListenerType(io.gravitee.rest.api.management.v2.rest.model.ListenerType.HTTP)
            .supportedModes(Set.of(io.gravitee.rest.api.management.v2.rest.model.ConnectorMode.SUBSCRIBE))
            .deployed(true);
        ConnectorPlugin pluginEntity2 = new ConnectorPlugin()
            .id("id-2")
            .name("name")
            .version("1.0")
            .icon("my-icon")
            .supportedApiType(io.gravitee.rest.api.management.v2.rest.model.ApiType.MESSAGE)
            .supportedListenerType(io.gravitee.rest.api.management.v2.rest.model.ListenerType.HTTP)
            .supportedModes(Set.of(io.gravitee.rest.api.management.v2.rest.model.ConnectorMode.SUBSCRIBE))
            .deployed(true);
        ConnectorPlugin pluginEntity3 = new ConnectorPlugin()
            .id("id-3")
            .name("name")
            .version("1.0")
            .icon("my-icon")
            .supportedApiType(io.gravitee.rest.api.management.v2.rest.model.ApiType.MESSAGE)
            .supportedListenerType(io.gravitee.rest.api.management.v2.rest.model.ListenerType.HTTP)
            .supportedModes(Set.of(io.gravitee.rest.api.management.v2.rest.model.ConnectorMode.SUBSCRIBE))
            .deployed(false);

        assertEquals(Set.of(pluginEntity1, pluginEntity2, pluginEntity3), connectorPlugins);
    }

    @Test
    public void shouldGetEntrypointSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
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
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenThrow(new PluginNotFoundException(FAKE_ENTRYPOINT_ID));

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("schema").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        final Error error = response.readEntity(Error.class);

        final Error expectedError = new Error();
        expectedError.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedError.setMessage("Plugin [" + FAKE_ENTRYPOINT_ID + "] cannot be found.");
        expectedError.setTechnicalCode("plugin.notFound");
        expectedError.setParameters(Map.of("plugin", FAKE_ENTRYPOINT_ID));

        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @Test
    public void shouldGetEntrypointDocumentation() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
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
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenThrow(new PluginNotFoundException(FAKE_ENTRYPOINT_ID));

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("documentation").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());

        final Error error = response.readEntity(Error.class);

        final Error expectedError = new Error();
        expectedError.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedError.setMessage("Plugin [" + FAKE_ENTRYPOINT_ID + "] cannot be found.");
        expectedError.setTechnicalCode("plugin.notFound");
        expectedError.setParameters(Map.of("plugin", FAKE_ENTRYPOINT_ID));

        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @Test
    public void shouldGetEntrypointSubscriptionSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenReturn(connectorPlugin);
        when(entrypointConnectorPluginService.getSubscriptionSchema(FAKE_ENTRYPOINT_ID)).thenReturn("subscriptionSchemaResponse");

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("subscription-schema").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("subscriptionSchemaResponse");
    }

    @Test
    public void shouldGetEntrypointSubscriptionSchemaWithDisplay() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenReturn(connectorPlugin);
        when(entrypointConnectorPluginService.getSubscriptionSchema(FAKE_ENTRYPOINT_ID, SchemaDisplayFormat.GV_SCHEMA_FORM)).thenReturn(
            "subscriptionSchemaResponse"
        );

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID)
            .path("subscription-schema")
            .queryParam("display", "gv-schema-form")
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("subscriptionSchemaResponse");
    }

    @Test
    public void shouldNotGetEntrypointSubscriptionSchemaWhenPluginNotFound() {
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenThrow(new PluginNotFoundException(FAKE_ENTRYPOINT_ID));

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("subscription-schema").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        final Error error = response.readEntity(Error.class);

        final Error expectedError = new Error();
        expectedError.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedError.setMessage("Plugin [" + FAKE_ENTRYPOINT_ID + "] cannot be found.");
        expectedError.setTechnicalCode("plugin.notFound");
        expectedError.setParameters(Map.of("plugin", FAKE_ENTRYPOINT_ID));

        Assertions.assertThat(error).isEqualTo(expectedError);
    }

    @Test
    public void shouldGetEndpointById() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT_ID);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setIcon("Fake Icon");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenReturn(connectorPlugin);

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final ConnectorPlugin entrypoint = response.readEntity(ConnectorPlugin.class);
        assertNotNull(entrypoint);
        assertEquals(FAKE_ENTRYPOINT_ID, entrypoint.getId());
        assertEquals("Fake Entrypoint", entrypoint.getName());
        assertEquals("1.0", entrypoint.getVersion());
        assertEquals("Fake Icon", entrypoint.getIcon());
        assertEquals(io.gravitee.rest.api.management.v2.rest.model.ApiType.MESSAGE, entrypoint.getSupportedApiType());
        assertEquals(Set.of(io.gravitee.rest.api.management.v2.rest.model.ConnectorMode.SUBSCRIBE), entrypoint.getSupportedModes());
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
        connectorPlugin.setSupportedListenerType(ListenerType.HTTP);
        connectorPlugin.setDeployed(true);
        return connectorPlugin;
    }

    @NotNull
    private io.gravitee.apim.core.plugin.model.ConnectorPlugin getConnectorPlugin(String id) {
        io.gravitee.apim.core.plugin.model.ConnectorPlugin connectorPlugin = new io.gravitee.apim.core.plugin.model.ConnectorPlugin();
        connectorPlugin.setId(id);
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setIcon("my-icon");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        connectorPlugin.setSupportedListenerType(ListenerType.HTTP);
        connectorPlugin.setDeployed(true);
        connectorPlugin.setFeature("feature-" + id);
        return connectorPlugin;
    }
}
