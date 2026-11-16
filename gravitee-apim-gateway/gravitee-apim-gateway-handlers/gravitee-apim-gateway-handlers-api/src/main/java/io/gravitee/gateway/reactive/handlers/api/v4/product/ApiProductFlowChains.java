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

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.v4.flow.FlowChain;
import io.reactivex.rxjava3.core.Completable;

/**
 * The chains a member API runs for a product subscription, in the product's flow mode: the flows of the subscribed
 * product plan, then the flows of the product.
 */
public record ApiProductFlowChains(FlowChain productPlanFlowChain, FlowChain productFlowChain) {
    public Completable execute(HttpExecutionContext ctx, ExecutionPhase phase) {
        return productPlanFlowChain.execute(ctx, phase).andThen(productFlowChain.execute(ctx, phase));
    }
}
