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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import inmemory.ResourcePluginCrudServiceInMemory;
import inmemory.ResourcePluginQueryServiceInMemory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.ResourcePlugin;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PluginNotFoundException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ResourcesResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "/plugins/resources";
    }

    @Autowired
    protected ResourcePluginQueryServiceInMemory resourcePluginQueryServiceInMemory;

    @Autowired
    protected ResourcePluginCrudServiceInMemory resourcePluginCrudServiceInMemory;

    @Autowired
    protected LicenseManager licenseManager;

    protected License license;

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(license);
        resourcePluginQueryServiceInMemory.reset();
        resourcePluginCrudServiceInMemory.reset();
    }

    @BeforeEach
    public void setUp() {
        super.setUp();
        GraviteeContext.setCurrentOrganization("fake-org");
        license = mock(License.class);
        var fakeResourcePlugins = List.of(
            io.gravitee.apim.core.plugin.model.ResourcePlugin
                .builder()
                .id("resource-1")
                .name("resource-1")
                .feature("feature-1")
                .deployed(false)
                .build(),
            io.gravitee.apim.core.plugin.model.ResourcePlugin
                .builder()
                .id("resource-2")
                .name("resource-2")
                .feature("feature-2")
                .description("description")
                .category("category")
                .icon("icon")
                .version("1.0")
                .deployed(true)
                .build(),
            io.gravitee.apim.core.plugin.model.ResourcePlugin
                .builder()
                .id("resource-3")
                .name("resource-3")
                .feature("feature-3")
                .deployed(true)
                .build()
        );

        resourcePluginQueryServiceInMemory.initWith(fakeResourcePlugins);
        resourcePluginCrudServiceInMemory.initWith(fakeResourcePlugins);
    }

    @Test
    public void should_get_resource() {
        when(licenseManager.getOrganizationLicenseOrPlatform("fake-org")).thenReturn(license);
        when(license.isFeatureEnabled("feature-2")).thenReturn(true);

        final Response response = rootTarget("resource-2").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final ResourcePlugin resource = response.readEntity(ResourcePlugin.class);
        assertNotNull(resource);
        assertEquals("resource-2", resource.getId());
        assertEquals("resource-2", resource.getName());
        assertEquals(true, resource.getDeployed());
        assertEquals("description", resource.getDescription());
        assertEquals("category", resource.getCategory());
        assertEquals("icon", resource.getIcon());
        assertEquals("1.0", resource.getVersion());
    }

    @Test
    public void should_return_sorted_policies() {
        when(licenseManager.getOrganizationLicenseOrPlatform("fake-org")).thenReturn(license);
        when(license.isFeatureEnabled("feature-1")).thenReturn(true);
        when(license.isFeatureEnabled("feature-2")).thenReturn(false);
        when(license.isFeatureEnabled("feature-3")).thenReturn(true);

        final Response response = rootTarget().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        // Check response content
        final List<ResourcePlugin> resourcePlugins = response.readEntity(new GenericType<>() {});

        assertThat(resourcePlugins)
            .containsExactly(
                ResourcePlugin.builder().id("resource-1").name("resource-1").deployed(false).build(),
                ResourcePlugin
                    .builder()
                    .id("resource-2")
                    .name("resource-2")
                    .description("description")
                    .category("category")
                    .icon("icon")
                    .version("1.0")
                    .deployed(false)
                    .build(),
                ResourcePlugin.builder().id("resource-3").name("resource-3").deployed(true).build()
            );
    }

    @Test
    public void should_get_resource_schema() {
        var license = mock(License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("fake-org")).thenReturn(license);
        when(license.isFeatureEnabled("feature-2")).thenReturn(true);

        final Response response = rootTarget("resource-2").path("schema").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final String resource = response.readEntity(String.class);
        assertNotNull(resource);
    }
}
