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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.ConnectorMode;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.rest.api.management.v4.rest.model.*;
import io.gravitee.rest.api.management.v4.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.management.v4.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
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
    public void shouldThrowBadRequestExceptionIfPerPageParamIsNotValid() {
        final Response response = rootTarget()
            .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 1)
            .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, -2)
            .request()
            .get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message.contains("Pagination perPage param must be >= 1"));
    }

    @Test
    public void shouldThrowBadRequestExceptionIfPageParamIsNotValid() {
        final Response response = rootTarget()
            .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 0)
            .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, 1)
            .request()
            .get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        final String message = response.readEntity(String.class);
        assertTrue(message.contains("Pagination page param must be >= 1"));
    }

    @Test
    public void shouldReturnEndpointsFirstPage() {
        ConnectorPluginEntity connectorPlugin = getConnectorPluginEntity();
        when(endpointConnectorPluginService.findAll()).thenReturn(Set.of(connectorPlugin));

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check Response content
        final EndpointsResponse endpointsResponse = response.readEntity(EndpointsResponse.class);
        assertNotNull(endpointsResponse.getData());
        assertNotNull(endpointsResponse.getPagination());
        assertNotNull(endpointsResponse.getLinks());

        // Check connectorPlugins
        List<ConnectorPlugin> connectorPlugins = endpointsResponse.getData();
        assertEquals(1, connectorPlugins.size());
        ConnectorPlugin pluginEntity = connectorPlugins.get(0);
        assertEquals("id", pluginEntity.getId());
        assertEquals("name", pluginEntity.getName());
        assertEquals("1.0", pluginEntity.getVersion());
        assertEquals(ApiType.MESSAGE.toString(), pluginEntity.getSupportedApiType().getValue());
        assertEquals(1, pluginEntity.getSupportedModes().size());
        assertEquals(ConnectorMode.SUBSCRIBE.getLabel(), pluginEntity.getSupportedModes().iterator().next().getValue());
        assertEquals(1, pluginEntity.getSupportedQos().size());
        assertEquals(Qos.AUTO.name(), pluginEntity.getSupportedQos().iterator().next().name());

        // Check pagination
        Pagination pagination = endpointsResponse.getPagination();
        assertEquals(1, pagination.getPage());
        assertEquals(10, pagination.getPerPage());
        assertEquals(1, pagination.getPageItemsCount());
        assertEquals(1, pagination.getTotalCount());
        assertEquals(1, pagination.getPageCount());

        // Check links
        Links links = endpointsResponse.getLinks();
        assertTrue(links.getSelf().endsWith("/plugins/endpoints/"));
        assertNull(links.getFirst());
        assertNull(links.getPrevious());
        assertNull(links.getNext());
        assertNull(links.getLast());
    }

    @Test
    public void shouldReturnEndpointsSmallPage() {
        ConnectorPluginEntity connectorPlugin = getConnectorPluginEntity("id-1");
        when(endpointConnectorPluginService.findAll())
            .thenReturn(
                List
                    .of(connectorPlugin, getConnectorPluginEntity("id-2"), getConnectorPluginEntity("id-3"))
                    .stream()
                    .collect(Collectors.toCollection(LinkedHashSet::new))
            );

        final Response response = rootTarget()
            .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 1)
            .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, 2)
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check response content
        final EndpointsResponse endpointsResponse = response.readEntity(EndpointsResponse.class);
        assertNotNull(endpointsResponse.getData());
        assertNotNull(endpointsResponse.getPagination());
        assertNotNull(endpointsResponse.getLinks());

        // Check data
        List<ConnectorPlugin> data = endpointsResponse.getData();
        assertEquals(2, data.size());
        ConnectorPlugin pluginEntity = data.get(0);
        assertEquals("id-1", pluginEntity.getId());
        assertEquals("name", pluginEntity.getName());
        assertEquals("1.0", pluginEntity.getVersion());
        assertEquals(ApiType.MESSAGE.toString(), pluginEntity.getSupportedApiType().getValue());
        assertEquals(1, pluginEntity.getSupportedModes().size());
        assertEquals(ConnectorMode.SUBSCRIBE.getLabel(), pluginEntity.getSupportedModes().iterator().next().getValue());
        assertEquals(1, pluginEntity.getSupportedQos().size());
        assertEquals(Qos.AUTO.name(), pluginEntity.getSupportedQos().iterator().next().name());

        // Check pagination
        Pagination pagination = endpointsResponse.getPagination();
        assertEquals(1, pagination.getPage());
        assertEquals(2, pagination.getPerPage());
        assertEquals(2, pagination.getPageItemsCount());
        assertEquals(3, pagination.getTotalCount());
        assertEquals(2, pagination.getPageCount());

        // Check links
        Links links = endpointsResponse.getLinks();
        assertTrue(links.getSelf().endsWith("/plugins/endpoints/?page=1&perPage=2"));
        assertTrue(links.getFirst().endsWith("/plugins/endpoints/?page=1&perPage=2"));
        assertNull(links.getPrevious());
        assertTrue(links.getNext().endsWith("/plugins/endpoints/?page=2&perPage=2"));
        assertTrue(links.getLast().endsWith("/plugins/endpoints/?page=2&perPage=2"));
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
        final ErrorEntity errorEntity = response.readEntity(ErrorEntity.class);

        final ErrorEntity expectedErrorEntity = new ErrorEntity();
        expectedErrorEntity.setHttpStatus(HttpStatusCode.NOT_FOUND_404);
        expectedErrorEntity.setMessage("Plugin [" + FAKE_ENDPOINT_ID + "] can not be found.");
        expectedErrorEntity.setTechnicalCode("plugin.notFound");
        expectedErrorEntity.setParameters(Map.of("plugin", FAKE_ENDPOINT_ID));

        Assertions.assertThat(errorEntity).isEqualTo(expectedErrorEntity);
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
        connectorPlugin.setSupportedApiType(ApiType.MESSAGE);
        connectorPlugin.setSupportedModes(Set.of(ConnectorMode.SUBSCRIBE));
        connectorPlugin.setSupportedQos(Set.of(Qos.AUTO));
        return connectorPlugin;
    }
}
