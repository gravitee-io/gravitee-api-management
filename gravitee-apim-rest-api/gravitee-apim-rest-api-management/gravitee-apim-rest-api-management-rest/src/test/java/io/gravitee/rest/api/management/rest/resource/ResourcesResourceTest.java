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

import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.ResourceListItem;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.service.ResourceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.GenericType;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Antoine Cordier (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourcesResourceTest extends AbstractResourceTest {

    @Inject
    ResourceService resourceService;

    @Autowired
    protected LicenseManager licenseManager;

    @Override
    protected String contextPath() {
        return "resources";
    }

    @Test
    public void shouldReturnResources() {
        when(resourceService.findAll()).thenReturn(
            Set.of(
                PlatformPluginEntity.builder().id("resource-id-1").feature("resource-feature-1").name("Resource 1").deployed(true).build(),
                PlatformPluginEntity.builder().id("resource-id-2").feature("resource-feature-2").name("Resource 2").deployed(true).build(),
                PlatformPluginEntity.builder().id("resource-id-3").feature("resource-feature-3").name("Resource 3").deployed(false).build()
            )
        );
        var license = mock(License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("DEFAULT")).thenReturn(license);
        when(license.isFeatureEnabled("resource-feature-1")).thenReturn(false);
        when(license.isFeatureEnabled("resource-feature-2")).thenReturn(true);
        when(license.isFeatureEnabled("resource-feature-3")).thenReturn(true);

        var response = envTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());

        var resources = response.readEntity(new GenericType<Set<ResourceListItem>>() {});
        var expected1 = new ResourceListItem();
        expected1.setId("resource-id-1");
        expected1.setName("Resource 1");
        expected1.setDeployed(false);
        expected1.setFeature("resource-feature-1");
        var expected2 = new ResourceListItem();
        expected2.setId("resource-id-2");
        expected2.setName("Resource 2");
        expected2.setDeployed(true);
        expected2.setFeature("resource-feature-2");
        var expected3 = new ResourceListItem();
        expected3.setId("resource-id-3");
        expected3.setName("Resource 3");
        expected3.setDeployed(false);
        expected3.setFeature("resource-feature-3");

        assertThat(resources).hasSize(3).containsExactly(expected1, expected3, expected2);
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
