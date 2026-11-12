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

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChain;
import io.gravitee.gateway.reactive.handlers.api.v4.processor.pathparameters.PathParametersExtractor;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * What one deployed version of an API Product runs on this gateway, whatever the number of its member APIs: the
 * policies of its plans and the chain of its plan flows, built once in the product's flow mode.
 *
 * <p>A member API pins the runtime for the whole request, so the response phase replays the runtime the request phase
 * used. A replaced runtime is retired: it is handed out no more, and stops once its pinned requests have ended.</p>
 */
public class ApiProductRuntime {

    public static final String ATTR_INTERNAL_API_PRODUCT_RUNTIME = "apiProduct.runtime";

    private final ReactableApiProduct product;
    private final FlowChain productPlanFlowChain;
    private final FlowChain tracedProductPlanFlowChain;
    private final PathParametersExtractor pathParametersExtractor;
    private final List<LifecycleComponent<?>> components;
    private final ResourceLifecycleManager resourceManager;
    private final AtomicInteger inFlight = new AtomicInteger();
    private volatile boolean retired;

    /**
     * @param pathParametersExtractor extracts the path parameters declared by the plan flows, null when none declares one
     * @param components started in order and stopped in reverse order, the policy manager first
     */
    public ApiProductRuntime(
        ReactableApiProduct product,
        FlowChain productPlanFlowChain,
        FlowChain tracedProductPlanFlowChain,
        PathParametersExtractor pathParametersExtractor,
        List<LifecycleComponent<?>> components,
        ResourceLifecycleManager resourceManager
    ) {
        this.product = product;
        this.productPlanFlowChain = productPlanFlowChain;
        this.tracedProductPlanFlowChain = tracedProductPlanFlowChain;
        this.pathParametersExtractor = pathParametersExtractor;
        this.components = List.copyOf(components);
        this.resourceManager = resourceManager;
    }

    /**
     * The resource manager this runtime exposes during its product plan flow: the product's started
     * manager, or {@link EmptyProductResourceLifecycleManager} when the product has no resources.
     */
    public ResourceLifecycleManager resourceManager() {
        return resourceManager;
    }

    public ReactableApiProduct product() {
        return product;
    }

    List<LifecycleComponent<?>> lifecycleComponents() {
        return components;
    }

    public boolean servesApi(String apiId) {
        return product.getApiIds() != null && product.getApiIds().contains(apiId);
    }

    public FlowChain productPlanFlowChain(boolean tracing) {
        return tracing ? tracedProductPlanFlowChain : productPlanFlowChain;
    }

    /**
     * Sets the path parameters the product plan flows declare and the member API does not: the API's own value wins.
     */
    public void extractPathParameters(HttpExecutionContext ctx) {
        if (pathParametersExtractor == null) {
            return;
        }
        var pathParameters = ctx.request().pathParameters();
        pathParametersExtractor
            .extract(ctx.request().method().name(), ctx.request().pathInfo())
            .forEach((name, value) -> {
                if (!pathParameters.containsKey(name)) {
                    pathParameters.add(name, value);
                }
            });
    }

    /**
     * Counts a request in, unless the runtime is retired. The count is raised before the flag is read, and the flag set
     * before the count is read, so a request either sees the runtime retired or is waited for.
     */
    boolean tryAcquire() {
        inFlight.incrementAndGet();
        if (retired) {
            inFlight.decrementAndGet();
            return false;
        }
        return true;
    }

    public void release() {
        inFlight.decrementAndGet();
    }

    int inFlight() {
        return inFlight.get();
    }

    void retire() {
        retired = true;
    }

    void start() throws Exception {
        for (LifecycleComponent<?> component : components) {
            component.start();
        }
    }

    /** Stops every component, even after one fails to stop, then throws the first failure. */
    void stop() throws Exception {
        Exception failure = null;
        for (int i = components.size() - 1; i >= 0; i--) {
            try {
                components.get(i).stop();
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
