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
package io.gravitee.gateway.jupiter.policy;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.policy.*;
import io.gravitee.gateway.policy.dummy.*;
import io.gravitee.gateway.policy.impl.PolicyManifestBuilder;
import io.gravitee.gateway.policy.impl.PolicyPluginFactoryImpl;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.gateway.jupiter.policy.adapter.policy.PolicyAdapter;
import io.gravitee.plugin.policy.internal.PolicyMethodResolver;
import io.gravitee.policy.api.PolicyConfiguration;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class DefaultPolicyFactoryTest {

    private DefaultPolicyFactory policyFactory;
    private PolicyManifestBuilder policyManifestBuilder;
    private PolicyMetadata policyMetadata;
    private PolicyConfiguration policyConfiguration;

    @BeforeEach
    public void init() {
        policyFactory = new DefaultPolicyFactory(new PolicyPluginFactoryImpl(), new ExpressionLanguageStringConditionEvaluator());
        policyConfiguration = new DummyPolicyConfiguration();
        ((DummyPolicyConfiguration) policyConfiguration).setValue(1);
        policyMetadata = new PolicyMetadata("dummy-reactive", "{\"value\": 1}");
        policyManifestBuilder =
            new PolicyManifestBuilder().setId("dummy-reactive").setClassLoader(new DefaultClassLoader(this.getClass().getClassLoader()));
    }

    @ParameterizedTest
    @EnumSource(ExecutionPhase.class)
    public void shouldCreateReactivePolicyWithoutConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder.setPolicy(DummyReactivePolicy.class).build();
        Policy policy = policyFactory.create(phase, policyManifest, null, policyMetadata);
        assertInstanceOf(DummyReactivePolicy.class, policy);
    }

    @ParameterizedTest
    @EnumSource(ExecutionPhase.class)
    public void shouldCreateReactivePolicyWithConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyReactivePolicyWithConfig.class)
            .setConfiguration(DummyPolicyConfiguration.class)
            .build();
        Policy policy = policyFactory.create(phase, policyManifest, policyConfiguration, policyMetadata);
        assertInstanceOf(DummyReactivePolicyWithConfig.class, policy);
        DummyReactivePolicyWithConfig dummyReactivePolicy = (DummyReactivePolicyWithConfig) policy;
        assertThat(dummyReactivePolicy.getDummyPolicyConfiguration()).isEqualTo(policyConfiguration);
    }

    @Test
    public void shouldCreateOnceReactivePolicy() {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyReactivePolicyWithConfig.class)
            .setConfiguration(DummyPolicyConfiguration.class)
            .build();
        Policy policy = policyFactory.create(ExecutionPhase.REQUEST, policyManifest, policyConfiguration, policyMetadata);
        assertInstanceOf(DummyReactivePolicyWithConfig.class, policy);

        Policy policy2 = policyFactory.create(ExecutionPhase.REQUEST, policyManifest, policyConfiguration, policyMetadata);
        assertInstanceOf(DummyReactivePolicyWithConfig.class, policy);

        assertSame(policy, policy2);
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionPhase.class, names = { "REQUEST", "RESPONSE" })
    public void shouldCreatePolicyAdapterWithoutConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyPolicy.class)
            .setMethods(new PolicyMethodResolver().resolve(DummyPolicy.class))
            .build();
        Policy policy = policyFactory.create(phase, policyManifest, null, policyMetadata);
        assertInstanceOf(PolicyAdapter.class, policy);
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionPhase.class, names = { "REQUEST", "RESPONSE" }, mode = EnumSource.Mode.EXCLUDE)
    public void shouldFailCreatePolicyAdapterWithoutConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyPolicy.class)
            .setMethods(new PolicyMethodResolver().resolve(DummyPolicy.class))
            .build();
        assertThrows(IllegalArgumentException.class, () -> policyFactory.create(phase, policyManifest, null, policyMetadata));
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionPhase.class, names = { "ASYNC_REQUEST", "ASYNC_RESPONSE" })
    public void shouldCreatePolicyAdapterWithConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyPolicyWithConfig.class)
            .setConfiguration(DummyPolicyConfiguration.class)
            .setMethods(new PolicyMethodResolver().resolve(DummyPolicyWithConfig.class))
            .build();
        assertThrows(
            IllegalArgumentException.class,
            () -> policyFactory.create(phase, policyManifest, policyConfiguration, policyMetadata)
        );
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionPhase.class, names = { "REQUEST", "RESPONSE" }, mode = EnumSource.Mode.EXCLUDE)
    public void shouldFailCreatePolicyAdapterWithConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyPolicyWithConfig.class)
            .setConfiguration(DummyPolicyConfiguration.class)
            .setMethods(new PolicyMethodResolver().resolve(DummyPolicyWithConfig.class))
            .build();
        assertThrows(
            IllegalArgumentException.class,
            () -> policyFactory.create(phase, policyManifest, policyConfiguration, policyMetadata)
        );
    }

    @Test
    public void shouldCreateOncePolicyAdapter() {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyPolicyWithConfig.class)
            .setConfiguration(DummyPolicyConfiguration.class)
            .setMethods(new PolicyMethodResolver().resolve(DummyPolicyWithConfig.class))
            .build();
        Policy policy = policyFactory.create(ExecutionPhase.REQUEST, policyManifest, policyConfiguration, policyMetadata);
        assertInstanceOf(PolicyAdapter.class, policy);

        Policy policy2 = policyFactory.create(ExecutionPhase.REQUEST, policyManifest, policyConfiguration, policyMetadata);
        assertInstanceOf(PolicyAdapter.class, policy);

        assertSame(policy, policy2);
    }

    @ParameterizedTest
    @MethodSource("wrongPhase")
    public void shouldReturnNullWhileCreatingPolicyAdapterOnWrongPhase(ExecutionPhase executionPhase, final Class<?> policyClass) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(policyClass)
            .setMethods(new PolicyMethodResolver().resolve(policyClass))
            .build();
        assertNull(policyFactory.create(executionPhase, policyManifest, policyConfiguration, policyMetadata));
    }

    private static Stream<Arguments> wrongPhase() {
        return Stream.of(
            Arguments.of(ExecutionPhase.REQUEST, DummyPolicyResponse.class),
            Arguments.of(ExecutionPhase.RESPONSE, DummyPolicyRequest.class)
        );
    }
}
