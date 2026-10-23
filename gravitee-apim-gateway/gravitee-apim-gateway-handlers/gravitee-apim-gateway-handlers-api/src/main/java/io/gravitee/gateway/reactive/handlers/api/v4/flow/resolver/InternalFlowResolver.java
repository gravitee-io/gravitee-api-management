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
package io.gravitee.gateway.reactive.handlers.api.v4.flow.resolver;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.v4.flow.FlowResolver;
import io.reactivex.rxjava3.core.Flowable;
import org.apache.commons.lang3.StringUtils;

/**
 * Allows resolving the internal {@link Flow} of an {@link Api}: the single flow managed by the platform and hidden from
 * the API publisher. While enabled, it applies to every request: its selectors and condition are not evaluated.
 */
class InternalFlowResolver implements FlowResolver<BaseExecutionContext> {

    // Without a name, the flow would show in policy chain ids and spans as "internal-".
    private static final String DEFAULT_NAME = "internal";

    private final Flowable<Flow> flows;

    public InternalFlowResolver(Api api) {
        // The internal flow can be determined once and then reused.
        this.flows = provideFlows(api);
    }

    @Override
    public Flowable<Flow> provideFlows(BaseExecutionContext ctx) {
        return this.flows;
    }

    @Override
    public Flowable<Flow> resolve(BaseExecutionContext ctx) {
        return this.flows;
    }

    private Flowable<Flow> provideFlows(Api api) {
        Flow internalFlow = api.getInternalFlow();
        if (internalFlow == null || !internalFlow.isEnabled()) {
            return Flowable.empty();
        }

        return Flowable.just(
            StringUtils.isBlank(internalFlow.getName()) ? internalFlow.toBuilder().name(DEFAULT_NAME).build() : internalFlow
        );
    }
}
