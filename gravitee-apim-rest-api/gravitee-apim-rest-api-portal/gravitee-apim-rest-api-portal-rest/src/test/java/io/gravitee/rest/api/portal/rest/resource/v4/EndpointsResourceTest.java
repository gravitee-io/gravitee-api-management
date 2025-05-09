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
package io.gravitee.rest.api.portal.rest.resource.v4;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.v4.connector.ConnectorPluginEntity;
import io.gravitee.rest.api.portal.rest.model.ConnectorsResponse;
import io.gravitee.rest.api.portal.rest.resource.AbstractResourceTest;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class EndpointsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "endpoints";
    }

    @BeforeEach
    void init() {
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    void should_empty_list_of_endpoints() {
        when(endpointConnectorPluginService.findAll()).thenReturn(Set.of());
        final Response response = target().request().get();
        assertEquals(OK_200, response.getStatus());

        var resp = response.readEntity(ConnectorsResponse.class);
        assertNotNull(resp.getData());
        assertEquals(0, resp.getData().size());
    }

    @Test
    void should_list_of_endpoints() {
        when(endpointConnectorPluginService.findAll()).thenReturn(Set.of(ConnectorPluginEntity.builder().id("mock").name("Mock").build()));
        final Response response = target().request().get();
        assertEquals(OK_200, response.getStatus());

        var resp = response.readEntity(ConnectorsResponse.class);
        assertNotNull(resp.getData());
        assertEquals(1, resp.getData().size());
        assertEquals("Mock", resp.getData().getFirst().getName());
        assertEquals("mock", resp.getData().getFirst().getId());
    }
}
