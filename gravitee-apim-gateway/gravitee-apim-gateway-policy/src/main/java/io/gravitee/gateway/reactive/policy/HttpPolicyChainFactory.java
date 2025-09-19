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
package io.gravitee.gateway.reactive.policy;

import static io.gravitee.gateway.reactive.api.ExecutionPhase.REQUEST;
import static io.gravitee.gateway.reactive.api.ExecutionPhase.RESPONSE;

import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.hook.HttpHook;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.policy.tracing.TracingPolicyHook;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.plugin.cache.common.InMemoryCache;
import io.netty.util.internal.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link PolicyChainFactory} that can be instantiated per-api or per-organization, and optimized to maximize the reuse of created {@link HttpPolicyChain} thanks to a cache.
 *
 * <b>WARNING</b>: this factory must absolutely be created per api to ensure proper cache destruction when deploying / un-deploying the api.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpPolicyChainFactory implements PolicyChainFactory<HttpPolicyChain> {

    public static final long CACHE_MAX_SIZE = 15;
    public static final long CACHE_TIME_TO_IDLE_MS = 3_600_000;
    private static final String ID_SEPARATOR = "-";
    protected final List<HttpHook> policyHooks = new ArrayList<>();
    private final PolicyManager policyManager;
    private final Cache<String, HttpPolicyChain> policyChains;

    public HttpPolicyChainFactory(final String id, final PolicyManager policyManager, final boolean tracing) {
        this.policyManager = policyManager;

        final CacheConfiguration cacheConfiguration = CacheConfiguration.builder()
            .maxSize(CACHE_MAX_SIZE)
            .timeToIdleInMs(CACHE_TIME_TO_IDLE_MS)
            .build();

        this.policyChains = new InMemoryCache<>(id + "-policyChainFactory", cacheConfiguration);
        initPolicyHooks(tracing);
    }

    private void initPolicyHooks(final boolean tracing) {
        if (tracing) {
            policyHooks.add(new TracingPolicyHook());
        }
    }

    /**
     * {@inheritDoc}
     *
     * Once created, the policy chain is put in cache to avoid useless re-instantiations.
     */
    @Override
    public HttpPolicyChain create(final String flowChainId, Flow flow, ExecutionPhase phase) {
        final String key = getFlowKey(flow, phase);
        HttpPolicyChain policyChain = policyChains.get(key);

        if (policyChain == null) {
            final List<Step> steps = getSteps(flow, phase);

            final List<HttpPolicy> policies = steps
                .stream()
                .filter(Step::isEnabled)
                .map(this::buildPolicyMetadata)
                .map(policyMetadata -> (HttpPolicy) policyManager.create(phase, policyMetadata))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            String policyChainId = getFlowId(flowChainId, flow);
            policyChain = new HttpPolicyChain(policyChainId, policies, phase);
            policyChain.addHooks(policyHooks);
            policyChains.put(key, policyChain);
        }

        return policyChain;
    }

    private PolicyMetadata buildPolicyMetadata(Step step) {
        final PolicyMetadata policyMetadata = new PolicyMetadata(step.getPolicy(), step.getConfiguration(), step.getCondition());
        policyMetadata.metadata().put(PolicyMetadata.MetadataKeys.EXECUTION_MODE, ExecutionMode.V4_EMULATION_ENGINE);

        return policyMetadata;
    }

    private List<Step> getSteps(Flow flow, ExecutionPhase phase) {
        List<Step> steps = null;

        if (phase == REQUEST) {
            steps = flow.getPre();
        } else if (phase == RESPONSE) {
            steps = flow.getPost();
        }

        return steps != null ? steps : Collections.emptyList();
    }

    private String getFlowKey(Flow flow, ExecutionPhase phase) {
        return flow.hashCode() + "-" + phase.name();
    }

    private String getFlowId(final String flowChainId, final Flow flow) {
        StringBuilder flowNameBuilder = new StringBuilder(flowChainId).append(ID_SEPARATOR);
        if (StringUtil.isNullOrEmpty(flow.getName())) {
            if (flow.getMethods().isEmpty()) {
                flowNameBuilder.append("ALL").append(ID_SEPARATOR);
            } else {
                flow.getMethods().forEach(httpMethod -> flowNameBuilder.append(httpMethod).append("-"));
            }
            flowNameBuilder.append(flow.getPath());
        } else {
            flowNameBuilder.append(flow.getName());
        }
        return flowNameBuilder.toString().toLowerCase(Locale.ROOT);
    }
}
