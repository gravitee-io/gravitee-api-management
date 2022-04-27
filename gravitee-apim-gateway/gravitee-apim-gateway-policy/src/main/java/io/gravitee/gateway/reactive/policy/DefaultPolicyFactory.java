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
package io.gravitee.gateway.reactive.policy;

import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.policy.*;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.reactive.policy.adapter.policy.PolicyAdapter;
import io.gravitee.policy.api.PolicyConfiguration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Guillaume Lamirand (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultPolicyFactory implements PolicyFactory {

    private final ConcurrentMap<String, Policy> policies = new ConcurrentHashMap<>();
    private final PolicyPluginFactory policyPluginFactory;
    private final io.gravitee.gateway.policy.PolicyFactory v3PolicyFactory;

    public DefaultPolicyFactory(final PolicyPluginFactory policyPluginFactory, final ConditionEvaluator<String> conditionEvaluator) {
        this.policyPluginFactory = policyPluginFactory;
        v3PolicyFactory = new io.gravitee.gateway.policy.impl.PolicyFactoryImpl(policyPluginFactory, conditionEvaluator);
    }

    @Override
    public Policy create(
        final ExecutionPhase executionPhase,
        final PolicyManifest policyManifest,
        final PolicyConfiguration policyConfiguration,
        final PolicyMetadata policyMetadata
    ) {
        return policies.computeIfAbsent(
            generateKey(executionPhase, policyManifest, policyConfiguration, policyMetadata.getCondition()),
            k -> createPolicy(executionPhase, policyManifest, policyConfiguration, policyMetadata)
        );
    }

    private Policy createPolicy(
        final ExecutionPhase phase,
        final PolicyManifest policyManifest,
        final PolicyConfiguration policyConfiguration,
        final PolicyMetadata policyMetadata
    ) {
        if (Policy.class.isAssignableFrom(policyManifest.policy())) {
            Object policy = policyPluginFactory.create(policyManifest.policy(), policyConfiguration);
            return (Policy) policy;
        } else if (phase == ExecutionPhase.REQUEST || phase == ExecutionPhase.RESPONSE) {
            StreamType streamType = phase == ExecutionPhase.REQUEST ? StreamType.ON_REQUEST : StreamType.ON_RESPONSE;
            if (policyManifest.accept(streamType)) {
                io.gravitee.gateway.policy.Policy v3Policy = v3PolicyFactory.create(
                    streamType,
                    policyManifest,
                    policyConfiguration,
                    policyMetadata
                );
                return new PolicyAdapter(v3Policy);
            }
        } else {
            throw new IllegalArgumentException(
                String.format("Cannot create policy instance with [phase=%s, policy=%s]", phase, policyManifest.id())
            );
        }
        return null;
    }

    @Override
    public void cleanup(PolicyManifest policyManifest) {
        policyPluginFactory.cleanup(policyManifest);
    }

    private String generateKey(
        final ExecutionPhase executionPhase,
        final PolicyManifest policyManifest,
        final PolicyConfiguration policyConfiguration,
        final String condition
    ) {
        return (
            Objects.hashCode(executionPhase) +
            "-" +
            Objects.hashCode(policyManifest) +
            "-" +
            Objects.hashCode(policyConfiguration) +
            "-" +
            Objects.hashCode(condition)
        );
    }
}
