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
package io.gravitee.rest.api.management.rest.resource;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.ResourceListItem;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.service.ResourceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import java.util.Set;
import org.junit.Test;

/**
 * @author Antoine Cordier (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourcesResourceTest extends AbstractResourceTest {

    @Inject
    ResourceService resourceService;

    @Override
    protected String contextPath() {
        return "resources";
    }

    @Test
    public void shouldReturnResources() {
        when(resourceService.findAll()).thenReturn(Set.of(PlatformPluginEntity.builder().id("resource-id").build()));

        var response = envTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());

        var resources = response.readEntity(new GenericType<Set<ResourceListItem>>() {});
        assertThat(resources).extracting("id").containsExactly("resource-id");
    }

    @Test
    public void shouldExpandSchema() {
        when(resourceService.findAll()).thenReturn(Set.of(PlatformPluginEntity.builder().id("resource-id").build()));

        when(resourceService.getSchema("resource-id")).thenReturn("{}");

        var response = envTarget().queryParam("expand", "schema").request().get();
        assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());

        var resources = response.readEntity(new GenericType<Set<ResourceListItem>>() {});
        assertThat(resources).extracting("schema").containsExactly("{}");
    }

    @Test
    public void shouldExpandIcon() {
        when(resourceService.findAll()).thenReturn(Set.of(PlatformPluginEntity.builder().id("resource-id").build()));

        when(resourceService.getIcon("resource-id")).thenReturn("🍔");

        var response = envTarget().queryParam("expand", "icon").request().get();
        assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());

        var resources = response.readEntity(new GenericType<Set<ResourceListItem>>() {});
        assertThat(resources).extracting("icon").containsExactly("🍔");
    }
}
