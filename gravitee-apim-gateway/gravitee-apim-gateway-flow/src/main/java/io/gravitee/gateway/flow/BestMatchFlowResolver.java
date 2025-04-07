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
package io.gravitee.gateway.flow;

import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.reactive.v4.flow.AbstractBestMatchFlowSelector;
import java.util.Collections;
import java.util.List;

/**
 * This flow provider is resolving only the {@link FlowV2Impl} which best match according to the incoming request.
 * This flow provider relies on the result of the {@link io.gravitee.gateway.flow.condition.ConditionalFlowResolver} use to build this instance.
 * This means that the flows list patterns to filter for Best Match mode already matches the request.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BestMatchFlowResolver implements FlowResolver {

    private final FlowResolver flowResolver;
    private AbstractBestMatchFlowSelector<FlowV2Impl> bestMatchFlowSelector;

    public BestMatchFlowResolver(final FlowResolver flowResolver, AbstractBestMatchFlowSelector<FlowV2Impl> bestMatchFlowSelector) {
        this.flowResolver = flowResolver;
        this.bestMatchFlowSelector = bestMatchFlowSelector;
    }

    @Override
    public List<FlowV2Impl> resolve(ExecutionContext context) {
        final FlowV2Impl bestMatch = bestMatchFlowSelector.forPath(flowResolver.resolve(context), context.request().pathInfo());
        return bestMatch == null ? Collections.emptyList() : List.of(bestMatch);
    }
}
