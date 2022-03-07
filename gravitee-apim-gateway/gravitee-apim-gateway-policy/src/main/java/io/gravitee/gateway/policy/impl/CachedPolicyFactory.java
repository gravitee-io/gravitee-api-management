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
package io.gravitee.gateway.policy.impl;

import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.policy.api.PolicyConfiguration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CachedPolicyFactory implements PolicyFactory {

    private final ConcurrentMap<String, Policy> policies = new ConcurrentHashMap<>();
    private final PolicyFactory delegate;

    public CachedPolicyFactory(PolicyFactory delegate) {
        Objects.requireNonNull(delegate, "PolicyFactory delegate is mandatory");

        this.delegate = delegate;
    }

    @Override
    public Policy create(StreamType streamType, PolicyMetadata policyMetadata, PolicyConfiguration policyConfiguration, String place) {
        return policies.computeIfAbsent(
            getKey(streamType, policyMetadata, policyConfiguration, null),
            k -> delegate.create(streamType, policyMetadata, policyConfiguration, place)
        );
    }

    @Override
    public Policy create(StreamType streamType, PolicyMetadata policyMetadata, PolicyConfiguration policyConfiguration, String place, String condition) {
        return policies.computeIfAbsent(
            getKey(streamType, policyMetadata, policyConfiguration, condition),
            k -> delegate.create(streamType, policyMetadata, policyConfiguration, place, condition)
        );
    }

    @Override
    public void cleanup(PolicyMetadata policyMetadata) {
        delegate.cleanup(policyMetadata);
    }

    private String getKey(StreamType streamType, PolicyMetadata policyMetadata, PolicyConfiguration policyConfiguration, String condition) {
        return (
            getHashCode(streamType) +
            "-" +
            getHashCode(policyMetadata) +
            "-" +
            getHashCode(policyConfiguration) +
            "-" +
            getHashCode(condition)
        );
    }

    private Integer getHashCode(Object o) {
        if (o == null) {
            return null;
        }

        return o.hashCode();
    }
}
