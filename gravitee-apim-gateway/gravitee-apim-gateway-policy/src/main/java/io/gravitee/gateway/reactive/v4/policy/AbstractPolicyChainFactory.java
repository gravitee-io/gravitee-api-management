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
package io.gravitee.gateway.reactive.v4.policy;

import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactive.policy.AbstractPolicyChain;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.plugin.cache.common.InMemoryCache;
import java.util.List;
import java.util.Objects;

/**
 * {@link AbstractPolicyChainFactory} that can be instantiated per-api or per-organization, and optimized to maximize the reuse of created {@link AbstractPolicyChain} thanks to a cache.
 *
 * <b>WARNING</b>: this factory must absolutely be created per api to ensure proper cache destruction when deploying / un-deploying the api.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractPolicyChainFactory<T extends BasePolicy, F extends AbstractFlow, PC extends AbstractPolicyChain<T>>
    implements PolicyChainFactory<PC, F> {

    public static final long CACHE_MAX_SIZE = 15;
    public static final long CACHE_TIME_TO_IDLE_IN_MS = 3_600_000;
    protected static final String ID_SEPARATOR = "-";

    protected final PolicyManager policyManager;
    protected final Cache<String, PC> policyChains;

    public AbstractPolicyChainFactory(final String id, final PolicyManager policyManager) {
        this.policyManager = policyManager;

        final CacheConfiguration cacheConfiguration = CacheConfiguration
            .builder()
            .maxSize(CACHE_MAX_SIZE)
            .timeToIdleInMs(CACHE_TIME_TO_IDLE_IN_MS)
            .build();

        this.policyChains = new InMemoryCache<>(id + getIdSuffix(), cacheConfiguration);
    }

    protected abstract String getIdSuffix();

    protected abstract List<Step> getSteps(F flow, ExecutionPhase phase);

    protected abstract PC buildPolicyChain(String flowChainId, F flow, ExecutionPhase phase, List<T> policies);

    protected abstract PolicyMetadata buildPolicyMetadata(Step step);

    /**
     * Creates a policy chain from the provided flow, for the given execution phase.
     * The policies composing the policy chain depends on the specified execution phase and the API type (Native vs HTTP):
     *
     * @param flowChainId the flow chain id in which one the policy chain will be executed
     * @param flow the flow where to extract the policies to create the policy chain.
     * @param phase the execution phase used to select the relevant steps list in the flow.
     *
     * @return the created {@link AbstractPolicyChain}.
     */
    @Override
    public PC create(final String flowChainId, F flow, ExecutionPhase phase) {
        final String key = getFlowKey(flow, phase);
        PC policyChain = policyChains.get(key);

        if (policyChain == null) {
            final List<Step> steps = getSteps(flow, phase);

            final List<T> policies = steps
                .stream()
                .filter(Step::isEnabled)
                .map(this::buildPolicyMetadata)
                .map(policyMetadata -> (T) policyManager.create(phase, policyMetadata))
                .filter(Objects::nonNull)
                .toList();

            policyChain = buildPolicyChain(flowChainId, flow, phase, policies);
            policyChains.put(key, policyChain);
        }

        return policyChain;
    }

    private String getFlowKey(F flow, ExecutionPhase phase) {
        return flow.hashCode() + "-" + phase.name();
    }
}
