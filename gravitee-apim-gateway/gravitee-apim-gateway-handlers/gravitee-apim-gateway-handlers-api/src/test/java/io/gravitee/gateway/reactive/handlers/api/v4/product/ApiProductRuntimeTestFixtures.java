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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiProductResourceManagerFactory;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChain;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.license.LicenseManager;
import java.util.List;
import java.util.Set;

public final class ApiProductRuntimeTestFixtures {

    private ApiProductRuntimeTestFixtures() {}

    public static ApiProductRuntime runtime(
        String productId,
        String apiId,
        FlowChain productPlanFlowChain,
        ResourceLifecycleManager resourceManager
    ) throws Exception {
        ReactableApiProduct product = mock(ReactableApiProduct.class);
        when(product.getId()).thenReturn(productId);
        when(product.getOrganizationId()).thenReturn("org");
        when(product.getApiIds()).thenReturn(Set.of(apiId));

        ApiProductResourceManagerFactory factory = mock(ApiProductResourceManagerFactory.class);
        when(factory.create(product)).thenReturn(resourceManager);
        LicenseManager licenseManager = mock(LicenseManager.class);

        ProductResourcesComponent resources = new ProductResourcesComponent(product, factory, licenseManager);
        resources.start();
        return new ApiProductRuntime(product, productPlanFlowChain, productPlanFlowChain, null, List.of(resources), resourceManager);
    }

    public static ApiProductRuntime runtimeWithoutResources(String productId, String apiId, FlowChain productPlanFlowChain) {
        ReactableApiProduct product = mock(ReactableApiProduct.class);
        when(product.getId()).thenReturn(productId);
        when(product.getApiIds()).thenReturn(Set.of(apiId));
        return new ApiProductRuntime(
            product,
            productPlanFlowChain,
            productPlanFlowChain,
            null,
            List.of(),
            EmptyProductResourceLifecycleManager.INSTANCE
        );
    }
}
