/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.handlers.api.adapter.resolver;

import io.gravitee.definition.model.flow.Flow;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.handlers.api.flow.resolver.FlowResolver;
import io.gravitee.gateway.reactive.policy.adapter.context.ExecutionContextAdapter;
import io.reactivex.Flowable;
import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowResolverAdapter implements FlowResolver {

    private final io.gravitee.gateway.flow.FlowResolver legacyResolver;

    public FlowResolverAdapter(io.gravitee.gateway.flow.FlowResolver legacyResolver) {
        this.legacyResolver = legacyResolver;
    }

    @Override
    public Flowable<Flow> resolve(ExecutionContext<?, ?> ctx) {
        final List<Flow> flows = legacyResolver.resolve(ExecutionContextAdapter.create(ctx));

        if (flows == null || flows.isEmpty()) {
            return Flowable.empty();
        }

        return Flowable.fromIterable(flows);
    }

    @Override
    public Flowable<Flow> provideFlows(ExecutionContext<?, ?> ctx) {
        return Flowable.empty();
    }
}
