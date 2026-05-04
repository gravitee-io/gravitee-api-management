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
package io.gravitee.gateway.handlers.sharedpolicygroup.policy;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.hook.HttpHook;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactive.policy.tracing.TracingPolicyHook;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.plugin.cache.common.InMemoryCache;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.CustomLog;

@CustomLog
public class DefaultSharedPolicyGroupPolicyChainFactory implements SharedPolicyGroupPolicyChainFactory {

    public static final long CACHE_MAX_SIZE = 15;
    public static final long CACHE_TIME_TO_IDLE_IN_MS = 3_600_000;
    protected final List<HttpHook> policyHooks = new ArrayList<>();
    protected final PolicyManager policyManager;
    protected final Cache<String, HttpPolicyChain> policyChains;

    public DefaultSharedPolicyGroupPolicyChainFactory(final String id, final PolicyManager policyManager, final boolean tracing) {
        this.policyManager = policyManager;

        final CacheConfiguration cacheConfiguration = CacheConfiguration.builder()
            .maxSize(CACHE_MAX_SIZE)
            .timeToIdleInMs(CACHE_TIME_TO_IDLE_IN_MS)
            .build();

        this.policyChains = new InMemoryCache<>(id + "-policyChainFactory", cacheConfiguration);
        initPolicyHooks(tracing);
    }

    protected void initPolicyHooks(final boolean tracing) {
        if (tracing) {
            policyHooks.add(new TracingPolicyHook());
        }
    }

    @Override
    public HttpPolicyChain create(final String sharedPolicyGroupPolicyId, String environmentId, List<Step> steps, ExecutionPhase phase) {
        final String key = getSharedPolicyGroupKey(sharedPolicyGroupPolicyId, environmentId, steps, phase);
        HttpPolicyChain policyChain = policyChains.get(key);
        if (policyChain == null) {
            policyChain = buildPolicyChain(sharedPolicyGroupPolicyId, steps, phase);
            policyChains.put(key, policyChain);
        }
        return policyChain;
    }

    private HttpPolicyChain buildPolicyChain(final String id, final List<Step> steps, final ExecutionPhase phase) {
        final List<HttpPolicy> policies = new ArrayList<>(steps.size());
        final Map<HttpPolicy, String> descriptions = new IdentityHashMap<>();
        for (Step step : steps) {
            if (!step.isEnabled()) continue;
            if (step.getPolicy().equals(SharedPolicyGroupPolicy.POLICY_ID)) {
                log.warn("Nested Shared Policy Group is not supported. The Shared Policy Group {} will be ignored", step.getName());
            } else {
                HttpPolicy policy = (HttpPolicy) policyManager.create(phase, buildPolicyMetadata(step));
                if (policy != null) {
                    policies.add(policy);
                    String desc = step.getDescription();
                    if (desc != null && !desc.isBlank()) descriptions.put(policy, desc);
                }
            }
        }
        HttpPolicyChain policyChain = new HttpPolicyChain(id, policies, phase);
        policyChain.addHooks(policyHooks);
        if (!descriptions.isEmpty()) {
            policyChain.setPolicyDescriptions(descriptions);
        }
        return policyChain;
    }

    @Override
    public HttpPolicyChain create(
        final String sharedPolicyGroupPolicyId,
        String environmentId,
        List<Step> steps,
        ExecutionPhase phase,
        List<HttpHook> additionalHooks
    ) {
        if (additionalHooks == null || additionalHooks.isEmpty()) {
            return create(sharedPolicyGroupPolicyId, environmentId, steps, phase);
        }
        // Build a fresh chain (not cached) — only used for debug requests
        return buildChain(sharedPolicyGroupPolicyId, steps, phase, additionalHooks);
    }

    private HttpPolicyChain buildChain(String id, List<Step> steps, ExecutionPhase phase, List<HttpHook> additionalHooks) {
        final List<HttpPolicy> policies = steps
            .stream()
            .filter(Step::isEnabled)
            .filter(step -> {
                final boolean hasNestedSharedPolicyGroup = step.getPolicy().equals(SharedPolicyGroupPolicy.POLICY_ID);
                if (hasNestedSharedPolicyGroup) {
                    log.warn("Nested Shared Policy Group is not supported. The Shared Policy Group {} will be ignored", step.getName());
                }
                return !hasNestedSharedPolicyGroup;
            })
            .map(this::buildPolicyMetadata)
            .map(policyMetadata -> (HttpPolicy) policyManager.create(phase, policyMetadata))
            .filter(Objects::nonNull)
            .toList();

        final HttpPolicyChain policyChain = new HttpPolicyChain(id, policies, phase);
        policyChain.addHooks(policyHooks);
        policyChain.addHooks(additionalHooks);
        return policyChain;
    }

    protected PolicyMetadata buildPolicyMetadata(Step step) {
        final PolicyMetadata policyMetadata = new PolicyMetadata(step.getPolicy(), step.getConfiguration(), step.getCondition());
        policyMetadata.metadata().put(PolicyMetadata.MetadataKeys.EXECUTION_MODE, ExecutionMode.V4_EMULATION_ENGINE);

        return policyMetadata;
    }

    @VisibleForTesting
    protected String getSharedPolicyGroupKey(
        String sharedPolicyGroupPolicyId,
        String environmentId,
        List<Step> steps,
        ExecutionPhase phase
    ) {
        return "shared-policy-group-" + sharedPolicyGroupPolicyId + "-" + environmentId + "-" + phase + "-" + steps.hashCode();
    }
}
