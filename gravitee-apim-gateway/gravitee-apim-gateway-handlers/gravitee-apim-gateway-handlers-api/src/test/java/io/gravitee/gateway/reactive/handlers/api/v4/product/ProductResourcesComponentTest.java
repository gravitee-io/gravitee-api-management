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
package io.gravitee.gateway.reactive.handlers.api.v4.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiProductResourceManagerFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.LicenseManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProductResourcesComponentTest {

    @Mock
    private ApiProductResourceManagerFactory resourceManagerFactory;

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private ResourceLifecycleManager resourceManager;

    @Test
    void should_fail_start_when_resource_plugins_are_not_licensed() throws Exception {
        Resource resource = Resource.builder().name("cache").type("cache-redis").configuration("{}").enabled(true).build();
        ReactableApiProduct product = productWithResources(List.of(resource));
        when(resourceManagerFactory.create(product)).thenReturn(resourceManager);
        doThrow(new ForbiddenFeatureException(List.of())).when(licenseManager).validatePluginFeatures(eq("org-1"), any());

        ProductResourcesComponent cut = new ProductResourcesComponent(product, resourceManagerFactory, licenseManager);

        assertThatThrownBy(cut::start).isInstanceOf(ForbiddenFeatureException.class);
        verify(resourceManager, never()).start();
    }

    @Test
    void should_start_the_manager_once_and_stop_when_the_last_reference_is_released() throws Exception {
        Resource resource = Resource.builder().name("cache").type("cache").configuration("{}").enabled(true).build();
        ReactableApiProduct product = productWithResources(List.of(resource));
        when(resourceManagerFactory.create(product)).thenReturn(resourceManager);

        ProductResourcesComponent first = new ProductResourcesComponent(product, resourceManagerFactory, licenseManager);
        ProductResourcesComponent second = new ProductResourcesComponent(first.sharedResources());

        first.start();
        second.start();
        first.stop();

        verify(resourceManager, times(1)).start();
        verify(resourceManager, never()).stop();

        second.stop();

        verify(resourceManager, times(1)).stop();
    }

    @Test
    void should_treat_null_and_empty_resources_as_the_same_list() {
        ReactableApiProduct withNull = productWithResources(null);
        ReactableApiProduct withEmpty = productWithResources(List.of());

        assertThat(ProductResourcesComponent.sameResources(withNull, withEmpty)).isTrue();
        assertThat(ProductResourcesComponent.hasConfiguredResources(withNull)).isFalse();
        assertThat(ProductResourcesComponent.hasConfiguredResources(withEmpty)).isFalse();
    }

    private static ReactableApiProduct productWithResources(List<Resource> resources) {
        return ReactableApiProduct.builder()
            .id("product-1")
            .organizationId("org-1")
            .environmentId("env-1")
            .apiIds(Set.of("api-1"))
            .resources(resources)
            .build();
    }
}
