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
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.plugin.core.api.PluginMoreInformation;
import io.gravitee.rest.api.management.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointResourceTest extends AbstractResourceTest {

    public static final String FAKE_ENTRYPOINT = "fake-entrypoint";

    @Override
    protected String contextPath() {
        return "v4/entrypoints/fake-entrypoint";
    }

    @Before
    public void init() {
        reset(entrypointConnectorPluginService);
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldGetEntrypointSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT)).thenReturn(connectorPlugin);
        when(entrypointConnectorPluginService.getSchema(FAKE_ENTRYPOINT)).thenReturn("schemaResponse");

        final Response response = envTarget().path("schema").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("schemaResponse");
    }

    @Test
    public void shouldNotGetEntrypointSchemaWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT)).thenThrow(new PluginNotFoundException(FAKE_ENTRYPOINT));

        final Response response = envTarget().path("schema").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions
            .assertThat(result)
            .isEqualTo(
                "{\n" +
                "  \"message\" : \"Plugin [" +
                FAKE_ENTRYPOINT +
                "] can not be found.\",\n" +
                "  \"parameters\" : {\n" +
                "    \"plugin\" : \"fake-entrypoint\"\n" +
                "  },\n" +
                "  \"technicalCode\" : \"plugin.notFound\",\n" +
                "  \"http_status\" : 404\n" +
                "}"
            );
    }

    @Test
    public void shouldGetEntrypointDocumentation() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT)).thenReturn(connectorPlugin);
        when(entrypointConnectorPluginService.getDocumentation(FAKE_ENTRYPOINT)).thenReturn("documentationResponse");

        final Response response = envTarget().path("documentation").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("documentationResponse");
    }

    @Test
    public void shouldNotGetEntrypointDocumentationWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT)).thenThrow(new PluginNotFoundException(FAKE_ENTRYPOINT));

        final Response response = envTarget().path("documentation").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions
            .assertThat(result)
            .isEqualTo(
                "{\n" +
                "  \"message\" : \"Plugin [" +
                FAKE_ENTRYPOINT +
                "] can not be found.\",\n" +
                "  \"parameters\" : {\n" +
                "    \"plugin\" : \"fake-entrypoint\"\n" +
                "  },\n" +
                "  \"technicalCode\" : \"plugin.notFound\",\n" +
                "  \"http_status\" : 404\n" +
                "}"
            );
    }

    @Test
    public void shouldGetEntrypointSubscriptionSchema() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT)).thenReturn(connectorPlugin);
        when(entrypointConnectorPluginService.getSubscriptionSchema(FAKE_ENTRYPOINT)).thenReturn("subscriptionSchemaResponse");

        final Response response = envTarget().path("subscriptionSchema").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions.assertThat(result).isEqualTo("subscriptionSchemaResponse");
    }

    @Test
    public void shouldNotGetEntrypointSubscriptionSchemaWhenPluginNotFound() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT);
        connectorPlugin.setName("Fake Entrypoint");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT)).thenThrow(new PluginNotFoundException(FAKE_ENTRYPOINT));

        final Response response = envTarget().path("subscriptionSchema").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        final String result = response.readEntity(String.class);
        Assertions
            .assertThat(result)
            .isEqualTo(
                "{\n" +
                "  \"message\" : \"Plugin [" +
                FAKE_ENTRYPOINT +
                "] can not be found.\",\n" +
                "  \"parameters\" : {\n" +
                "    \"plugin\" : \"fake-entrypoint\"\n" +
                "  },\n" +
                "  \"technicalCode\" : \"plugin.notFound\",\n" +
                "  \"http_status\" : 404\n" +
                "}"
            );
    }

    @Test
    public void shouldReturnMoreInformation() {
        ConnectorPluginEntity connectorPlugin = new ConnectorPluginEntity();
        connectorPlugin.setId(FAKE_ENTRYPOINT);
        connectorPlugin.setName("name");
        connectorPlugin.setVersion("1.0");
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT)).thenReturn(connectorPlugin);

        var moreInformation = new PluginMoreInformation();
        moreInformation.setDescription("A nice description");
        moreInformation.setSchemaImg("A nice schema");
        moreInformation.setDocumentationUrl("foobar");
        when(entrypointConnectorPluginService.getMoreInformation(FAKE_ENTRYPOINT)).thenReturn(moreInformation);

        final Response response = envTarget().path("/moreInformation").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        var body = response.readEntity(PluginMoreInformation.class);
        assertEquals("A nice description", body.getDescription());
        assertEquals("A nice schema", body.getSchemaImg());
        assertEquals("foobar", body.getDocumentationUrl());
    }

    @Test
    public void shouldReturn500IfMoreInformationError() {
        doThrow(new TechnicalManagementException()).when(entrypointConnectorPluginService).getMoreInformation(FAKE_ENTRYPOINT);

        final Response response = envTarget().path("/moreInformation").request().get();
        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }
}
