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
import io.gravitee.definition.model.v4.listener.ListenerType;
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
public class EntrypointsResourceTest extends AbstractResourceTest {

    public static final String FAKE_ENTRYPOINT_ID = "fake-entrypoint";

    @Override
    protected String contextPath() {
        return "/plugins/entrypoints";
    }

    @Before
    public void init() {
        reset(entrypointConnectorPluginService);
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
    public void shouldReturnEntrypointsFirstPage() {
        when(entrypointConnectorPluginService.findAll())
            .thenReturn(
                List
                    .of(getConnectorPluginEntity("id-1"), getConnectorPluginEntity("id-2"), getConnectorPluginEntity("id-3"))
                    .stream()
                    .collect(Collectors.toCollection(LinkedHashSet::new))
            );

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check response content
        final EntrypointsResponse entrypointsResponse = response.readEntity(EntrypointsResponse.class);
        assertNotNull(entrypointsResponse.getData());
        assertNotNull(entrypointsResponse.getPagination());
        assertNotNull(entrypointsResponse.getLinks());

        // Check data
        List<ConnectorPlugin> data = entrypointsResponse.getData();
        assertEquals(3, data.size());
        ConnectorPlugin pluginEntity = data.get(0);
        assertEquals("id-1", pluginEntity.getId());
        assertEquals("name", pluginEntity.getName());
        assertEquals("1.0", pluginEntity.getVersion());
        assertEquals(ApiType.MESSAGE.toString(), pluginEntity.getSupportedApiType().getValue());
        assertEquals(ListenerType.HTTP.toString(), pluginEntity.getSupportedListenerType().getValue());
        assertEquals(1, pluginEntity.getSupportedModes().size());
        assertEquals(ConnectorMode.SUBSCRIBE.getLabel(), pluginEntity.getSupportedModes().iterator().next().getValue());

        // Check pagination
        Pagination pagination = entrypointsResponse.getPagination();
        assertEquals(1, pagination.getPage());
        assertEquals(10, pagination.getPerPage());
        assertEquals(3, pagination.getPageItemsCount());
        assertEquals(3, pagination.getTotalCount());
        assertEquals(1, pagination.getPageCount());

        // Check links
        Links links = entrypointsResponse.getLinks();
        assertTrue(links.getSelf().endsWith("/plugins/entrypoints/"));
        assertNull(links.getFirst());
        assertNull(links.getPrevious());
        assertNull(links.getNext());
        assertNull(links.getLast());
    }

    @Test
    public void shouldReturnEntrypointsSmallPage() {
        when(entrypointConnectorPluginService.findAll())
            .thenReturn(
                List
                    .of(getConnectorPluginEntity("id-1"), getConnectorPluginEntity("id-2"), getConnectorPluginEntity("id-3"))
                    .stream()
                    .collect(Collectors.toCollection(LinkedHashSet::new))
            );

        final Response response = rootTarget()
            .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 2)
            .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, 2)
            .request()
            .get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check response content
        final EntrypointsResponse entrypointsResponse = response.readEntity(EntrypointsResponse.class);
        assertNotNull(entrypointsResponse.getData());
        assertNotNull(entrypointsResponse.getPagination());
        assertNotNull(entrypointsResponse.getLinks());

        // Check data
        List<ConnectorPlugin> data = entrypointsResponse.getData();
        assertEquals(1, data.size());
        ConnectorPlugin pluginEntity = data.get(0);
        assertEquals("id-3", pluginEntity.getId());
        assertEquals("name", pluginEntity.getName());
        assertEquals("1.0", pluginEntity.getVersion());
        assertEquals(ApiType.MESSAGE.toString(), pluginEntity.getSupportedApiType().getValue());
        assertEquals(ListenerType.HTTP.toString(), pluginEntity.getSupportedListenerType().getValue());
        LinkedHashSet<String> supportedModes = new LinkedHashSet<>();
        supportedModes.add(ConnectorMode.SUBSCRIBE.getLabel());
        assertEquals(1, pluginEntity.getSupportedModes().size());
        assertEquals(ConnectorMode.SUBSCRIBE.getLabel(), pluginEntity.getSupportedModes().iterator().next().getValue());

        // Check pagination
        Pagination pagination = entrypointsResponse.getPagination();
        assertEquals(2, pagination.getPage());
        assertEquals(2, pagination.getPerPage());
        assertEquals(1, pagination.getPageItemsCount());
        assertEquals(2, pagination.getPageCount());
        assertEquals(3, pagination.getTotalCount());

        // Check links
        Links links = entrypointsResponse.getLinks();
        assertTrue(links.getSelf().endsWith("/plugins/entrypoints/?page=2&perPage=2"));
        assertTrue(links.getFirst().endsWith("/plugins/entrypoints/?page=1&perPage=2"));
        assertTrue(links.getPrevious().endsWith("/plugins/entrypoints/?page=1&perPage=2"));
        assertNull(links.getNext());
        assertTrue(links.getLast().endsWith("/plugins/entrypoints/?page=2&perPage=2"));
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
    public void shouldNotGetEntrypointSubscriptionSchemaWhenPluginNotFound() {
        when(entrypointConnectorPluginService.findById(FAKE_ENTRYPOINT_ID)).thenThrow(new PluginNotFoundException(FAKE_ENTRYPOINT_ID));

        final Response response = rootTarget(FAKE_ENTRYPOINT_ID).path("subscription-schema").request().get();
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
        assertEquals(io.gravitee.rest.api.management.v4.rest.model.ApiType.MESSAGE, entrypoint.getSupportedApiType());
        assertEquals(Set.of(io.gravitee.rest.api.management.v4.rest.model.ConnectorMode.SUBSCRIBE), entrypoint.getSupportedModes());
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
        connectorPlugin.setSupportedListenerType(ListenerType.HTTP);
        return connectorPlugin;
    }
}
