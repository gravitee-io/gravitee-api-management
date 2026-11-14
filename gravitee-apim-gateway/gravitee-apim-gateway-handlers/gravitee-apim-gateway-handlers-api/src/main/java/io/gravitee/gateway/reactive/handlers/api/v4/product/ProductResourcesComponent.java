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

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiProductResourceManagerFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseManager;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * The enabled resources of one deployed version of an API Product, started and stopped with its runtime.
 */
class ProductResourcesComponent extends AbstractLifecycleComponent<ResourceLifecycleManager> {

    private final SharedProductResources shared;

    ProductResourcesComponent(
        ReactableApiProduct product,
        ApiProductResourceManagerFactory resourceManagerFactory,
        LicenseManager licenseManager
    ) {
        this.shared = new SharedProductResources(product, resourceManagerFactory, licenseManager);
    }

    ProductResourcesComponent(SharedProductResources shared) {
        this.shared = shared;
    }

    SharedProductResources sharedResources() {
        return shared;
    }

    ResourceLifecycleManager resourceLifecycleManager() {
        return shared.resourceLifecycleManager();
    }

    @Override
    protected void doStart() throws Exception {
        shared.acquire();
    }

    @Override
    protected void doStop() throws Exception {
        shared.release();
    }

    static boolean hasConfiguredResources(ReactableApiProduct product) {
        return !resourcesOf(product).isEmpty();
    }

    static boolean sameResources(ReactableApiProduct deployed, ReactableApiProduct incoming) {
        return resourcesOf(deployed).equals(resourcesOf(incoming));
    }

    static List<Resource> resourcesOf(ReactableApiProduct product) {
        return product == null || product.getResources() == null ? List.of() : product.getResources();
    }

    static ProductResourcesComponent findOn(ApiProductRuntime runtime) {
        if (runtime == null) {
            return null;
        }
        for (var component : runtime.lifecycleComponents()) {
            if (component instanceof ProductResourcesComponent productResources) {
                return productResources;
            }
        }
        return null;
    }

    /**
     * One started resource manager per unchanged resource list, reference-counted across consecutive runtimes.
     */
    static final class SharedProductResources {

        private final ReactableApiProduct product;
        private final ResourceLifecycleManager resourceManager;
        private final LicenseManager licenseManager;
        private final AtomicInteger references = new AtomicInteger();

        SharedProductResources(
            ReactableApiProduct product,
            ApiProductResourceManagerFactory resourceManagerFactory,
            LicenseManager licenseManager
        ) {
            this.product = product;
            this.licenseManager = licenseManager;
            this.resourceManager = resourceManagerFactory.create(product);
        }

        ResourceLifecycleManager resourceLifecycleManager() {
            return resourceManager;
        }

        void acquire() throws Exception {
            if (references.incrementAndGet() == 1) {
                validateEnabledResourcePlugins();
                resourceManager.start();
            }
        }

        void release() throws Exception {
            if (references.decrementAndGet() == 0) {
                resourceManager.stop();
            }
        }

        private void validateEnabledResourcePlugins() throws InvalidLicenseException, ForbiddenFeatureException {
            Set<LicenseManager.Plugin> plugins = resourcesOf(product)
                .stream()
                .filter(Objects::nonNull)
                .filter(Resource::isEnabled)
                .flatMap(resource -> resource.getPlugins().stream())
                .map(plugin -> new LicenseManager.Plugin(plugin.type(), plugin.id()))
                .collect(Collectors.toSet());
            if (!plugins.isEmpty()) {
                licenseManager.validatePluginFeatures(product.getOrganizationId(), plugins);
            }
        }
    }
}
