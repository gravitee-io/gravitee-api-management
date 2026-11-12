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
package io.gravitee.gateway.reactive.handlers.api.v4;

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.CompositeComponentProvider;
import io.gravitee.gateway.core.component.CustomComponentProvider;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.resource.api.ResourceManager;
import io.reactivex.rxjava3.core.Completable;
import java.util.function.Function;

final class ApiProductPlanResourceScope {

    private ApiProductPlanResourceScope() {}

    static Completable withProductResourceManager(
        final MutableExecutionContext ctx,
        final ComponentProvider apiComponentProvider,
        final ResourceLifecycleManager productResourceManager,
        final Function<MutableExecutionContext, Completable> scopedWork
    ) {
        final CustomComponentProvider productComponents = new CustomComponentProvider();
        productComponents.add(ResourceManager.class, productResourceManager);
        final ComponentProvider productScopedProvider = new CompositeComponentProvider(productComponents, apiComponentProvider);
        final MutableExecutionContext scopedCtx = new ProductPlanResourceScopedExecutionContext(
            ctx,
            productScopedProvider,
            apiComponentProvider
        );

        return scopedWork.apply(scopedCtx);
    }
}
