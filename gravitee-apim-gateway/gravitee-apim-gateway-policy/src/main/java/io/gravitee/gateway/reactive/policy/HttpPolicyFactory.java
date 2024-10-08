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

import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.adapter.policy.PolicyAdapter;
import io.gravitee.policy.api.PolicyConfiguration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Guillaume Lamirand (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpPolicyFactory implements PolicyFactory<HttpPolicy> {

    private final ConcurrentMap<String, HttpPolicy> policies = new ConcurrentHashMap<>();
    private final PolicyPluginFactory policyPluginFactory;
    private final io.gravitee.gateway.policy.PolicyFactory v3PolicyFactory;
    protected final ExpressionLanguageConditionFilter<HttpConditionalPolicy> filter;

    public HttpPolicyFactory(
        final PolicyPluginFactory policyPluginFactory,
        final ExpressionLanguageConditionFilter<HttpConditionalPolicy> filter
    ) {
        this.policyPluginFactory = policyPluginFactory;
        this.filter = filter;
        // V3 policy factory doesn't need condition evaluator anymore as condition is directly handled by v4 engine.
        this.v3PolicyFactory = new io.gravitee.gateway.policy.impl.PolicyFactoryImpl(policyPluginFactory);
    }

    @Override
    public boolean accept(PolicyManifest policyManifest) {
        // DefaultPolicyFactory accept any kind of policy
        return true;
    }

    @Override
    public HttpPolicy create(
        final ExecutionPhase executionPhase,
        final PolicyManifest policyManifest,
        final PolicyConfiguration policyConfiguration,
        final PolicyMetadata policyMetadata
    ) {
        return policies.computeIfAbsent(
            generateKey(
                executionPhase,
                policyManifest,
                policyConfiguration,
                policyMetadata.getCondition(),
                policyMetadata.getMessageCondition()
            ),
            k -> createPolicy(executionPhase, policyManifest, policyConfiguration, policyMetadata)
        );
    }

    protected HttpPolicy createPolicy(
        final ExecutionPhase phase,
        final PolicyManifest policyManifest,
        final PolicyConfiguration policyConfiguration,
        final PolicyMetadata policyMetadata
    ) {
        HttpPolicy policy = null;

        if (HttpPolicy.class.isAssignableFrom(policyManifest.policy())) {
            policy = (HttpPolicy) policyPluginFactory.create(policyManifest.policy(), policyConfiguration);
        } else if (phase == ExecutionPhase.REQUEST || phase == ExecutionPhase.RESPONSE) {
            StreamType streamType = phase == ExecutionPhase.REQUEST ? StreamType.ON_REQUEST : StreamType.ON_RESPONSE;
            if (policyManifest.accept(streamType)) {
                io.gravitee.gateway.policy.Policy v3Policy = v3PolicyFactory.create(
                    streamType,
                    policyManifest,
                    policyConfiguration,
                    policyMetadata
                );
                policy = new PolicyAdapter(v3Policy);
            }
        } else {
            throw new IllegalArgumentException(
                String.format("Cannot create policy instance with [phase=%s, policy=%s]", phase, policyManifest.id())
            );
        }

        policy = createConditionalPolicy(policyMetadata, policy);

        return policy;
    }

    protected HttpPolicy createConditionalPolicy(PolicyMetadata policyMetadata, HttpPolicy policy) {
        if (policy != null) {
            final String condition = policyMetadata.getCondition();

            // Avoid creating a conditional policy if no condition or message condition is defined.
            if (isNotBlank(condition)) {
                policy = new HttpConditionalPolicy(policy, condition, filter);
            }
        }
        return policy;
    }

    @Override
    public void cleanup(PolicyManifest policyManifest) {
        policyPluginFactory.cleanup(policyManifest);
    }

    private String generateKey(
        final ExecutionPhase executionPhase,
        final PolicyManifest policyManifest,
        final PolicyConfiguration policyConfiguration,
        final String condition,
        final String messageCondition
    ) {
        return (
            Objects.hashCode(executionPhase) +
            "-" +
            Objects.hashCode(policyManifest) +
            "-" +
            Objects.hashCode(policyConfiguration) +
            "-" +
            Objects.hashCode(condition) +
            "-" +
            Objects.hashCode(messageCondition)
        );
    }

    protected boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
