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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.dummy.DummyPolicy;
import io.gravitee.gateway.policy.dummy.DummyPolicyConfiguration;
import io.gravitee.gateway.policy.dummy.DummyPolicyRequest;
import io.gravitee.gateway.policy.dummy.DummyPolicyResponse;
import io.gravitee.gateway.policy.dummy.DummyPolicyWithConfig;
import io.gravitee.gateway.policy.dummy.DummyReactivePolicy;
import io.gravitee.gateway.policy.dummy.DummyReactivePolicyWithConfig;
import io.gravitee.gateway.policy.impl.PolicyManifestBuilder;
import io.gravitee.gateway.policy.impl.PolicyPluginFactoryImpl;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.adapter.policy.PolicyAdapter;
import io.gravitee.plugin.policy.internal.PolicyMethodResolver;
import io.gravitee.policy.api.PolicyConfiguration;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultPolicyFactoryTest {

    private DefaultPolicyFactory cut;

    @Spy
    private PolicyPluginFactory policyPluginFactory = new PolicyPluginFactoryImpl();

    private PolicyManifestBuilder policyManifestBuilder;
    private PolicyMetadata policyMetadata;
    private PolicyConfiguration policyConfiguration;

    private static Stream<Arguments> wrongPhase() {
        return Stream.of(
            Arguments.of(ExecutionPhase.REQUEST, DummyPolicyResponse.class),
            Arguments.of(ExecutionPhase.RESPONSE, DummyPolicyRequest.class)
        );
    }

    @BeforeEach
    void init() {
        cut = new DefaultPolicyFactory(policyPluginFactory, new ExpressionLanguageConditionFilter<>());
        policyConfiguration = new DummyPolicyConfiguration();
        ((DummyPolicyConfiguration) policyConfiguration).setValue(1);
        policyMetadata = new PolicyMetadata("dummy-reactive", "{\"value\": 1}");
        policyManifestBuilder =
            new PolicyManifestBuilder().setId("dummy-reactive").setClassLoader(new DefaultClassLoader(this.getClass().getClassLoader()));
    }

    @ParameterizedTest
    @EnumSource(ExecutionPhase.class)
    void shouldCreateReactivePolicyWithoutConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder.setPolicy(DummyReactivePolicy.class).build();
        HttpPolicy policy = cut.create(phase, policyManifest, null, policyMetadata);
        assertInstanceOf(DummyReactivePolicy.class, policy);
    }

    @ParameterizedTest
    @EnumSource(ExecutionPhase.class)
    void shouldCreateReactivePolicyWithConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyReactivePolicyWithConfig.class)
            .setConfiguration(DummyPolicyConfiguration.class)
            .build();
        HttpPolicy policy = cut.create(phase, policyManifest, policyConfiguration, policyMetadata);
        assertInstanceOf(DummyReactivePolicyWithConfig.class, policy);
        DummyReactivePolicyWithConfig dummyReactivePolicy = (DummyReactivePolicyWithConfig) policy;
        assertThat(dummyReactivePolicy.getDummyPolicyConfiguration()).isEqualTo(policyConfiguration);
    }

    @ParameterizedTest
    @EnumSource(ExecutionPhase.class)
    void shouldCreateReactiveConditionPolicy(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder.setPolicy(DummyReactivePolicy.class).build();
        policyMetadata = new PolicyMetadata("dummy-reactive", "{\"value\": 1}", "condition");
        HttpPolicy policy = cut.create(phase, policyManifest, null, policyMetadata);

        assertInstanceOf(ConditionalPolicy.class, policy);
    }

    @ParameterizedTest
    @EnumSource(ExecutionPhase.class)
    void shouldNotCreateConditionalPolicyWhenConditionIsEmpty(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder.setPolicy(DummyReactivePolicy.class).build();
        policyMetadata = new PolicyMetadata("dummy-reactive", "{\"value\": 1}", "");
        HttpPolicy policy = cut.create(phase, policyManifest, null, policyMetadata);
        assertInstanceOf(DummyReactivePolicy.class, policy);
    }

    @Test
    void shouldCreateOnceReactivePolicy() {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyReactivePolicyWithConfig.class)
            .setConfiguration(DummyPolicyConfiguration.class)
            .build();
        HttpPolicy policy = cut.create(ExecutionPhase.REQUEST, policyManifest, policyConfiguration, policyMetadata);
        assertInstanceOf(DummyReactivePolicyWithConfig.class, policy);

        HttpPolicy policy2 = cut.create(ExecutionPhase.REQUEST, policyManifest, policyConfiguration, policyMetadata);
        assertInstanceOf(DummyReactivePolicyWithConfig.class, policy);

        assertSame(policy, policy2);
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionPhase.class, names = { "REQUEST", "RESPONSE" })
    void shouldCreatePolicyAdapterWithoutConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyPolicy.class)
            .setMethods(new PolicyMethodResolver().resolve(DummyPolicy.class))
            .build();
        HttpPolicy policy = cut.create(phase, policyManifest, null, policyMetadata);
        assertInstanceOf(PolicyAdapter.class, policy);
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionPhase.class, names = { "REQUEST", "RESPONSE" }, mode = EnumSource.Mode.EXCLUDE)
    void shouldFailCreatePolicyAdapterWithoutConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyPolicy.class)
            .setMethods(new PolicyMethodResolver().resolve(DummyPolicy.class))
            .build();
        assertThrows(IllegalArgumentException.class, () -> cut.create(phase, policyManifest, null, policyMetadata));
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionPhase.class, names = { "MESSAGE_REQUEST", "MESSAGE_RESPONSE" })
    void shouldCreatePolicyAdapterWithConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyPolicyWithConfig.class)
            .setConfiguration(DummyPolicyConfiguration.class)
            .setMethods(new PolicyMethodResolver().resolve(DummyPolicyWithConfig.class))
            .build();
        assertThrows(IllegalArgumentException.class, () -> cut.create(phase, policyManifest, policyConfiguration, policyMetadata));
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionPhase.class, names = { "REQUEST", "RESPONSE" }, mode = EnumSource.Mode.EXCLUDE)
    void shouldFailCreatePolicyAdapterWithConfig(final ExecutionPhase phase) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyPolicyWithConfig.class)
            .setConfiguration(DummyPolicyConfiguration.class)
            .setMethods(new PolicyMethodResolver().resolve(DummyPolicyWithConfig.class))
            .build();
        assertThrows(IllegalArgumentException.class, () -> cut.create(phase, policyManifest, policyConfiguration, policyMetadata));
    }

    @Test
    void shouldCreateOncePolicyAdapter() {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(DummyPolicyWithConfig.class)
            .setConfiguration(DummyPolicyConfiguration.class)
            .setMethods(new PolicyMethodResolver().resolve(DummyPolicyWithConfig.class))
            .build();
        HttpPolicy policy = cut.create(ExecutionPhase.REQUEST, policyManifest, policyConfiguration, policyMetadata);
        assertInstanceOf(PolicyAdapter.class, policy);

        HttpPolicy policy2 = cut.create(ExecutionPhase.REQUEST, policyManifest, policyConfiguration, policyMetadata);
        assertInstanceOf(PolicyAdapter.class, policy);

        assertSame(policy, policy2);
    }

    @ParameterizedTest
    @MethodSource("wrongPhase")
    void shouldReturnNullWhileCreatingPolicyAdapterOnWrongPhase(ExecutionPhase executionPhase, final Class<?> policyClass) {
        PolicyManifest policyManifest = policyManifestBuilder
            .setPolicy(policyClass)
            .setMethods(new PolicyMethodResolver().resolve(policyClass))
            .build();
        assertNull(cut.create(executionPhase, policyManifest, policyConfiguration, policyMetadata));
    }

    @Test
    void shouldCleanupManifest() {
        final PolicyManifest policyManifest = new PolicyManifestBuilder().setPolicy(Policy.class).build();
        cut.cleanup(policyManifest);

        verify(policyPluginFactory).cleanup(policyManifest);
    }
}
