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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.impl.PolicyManifestBuilder;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.policy.api.PolicyConfiguration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class PolicyFactoryManagerTest {

    private static final DefaultPolicyFactory DEFAULT_POLICY_FACTORY = new DefaultPolicyFactory(null, null);
    private PolicyFactoryManager cut;

    @Test
    void should_throw_if_no_default_policy_factory() {
        assertThatThrownBy(() -> new PolicyFactoryManager(new HashSet<>()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to find an instance of DefaultPolicyFactory");
    }

    @Test
    void should_select_default_policy_factory() {
        cut = new PolicyFactoryManager(new HashSet<>(Set.of(DEFAULT_POLICY_FACTORY)));
        assertThat(cut.get(fakePolicyManifest("unknown"))).isEqualTo(DEFAULT_POLICY_FACTORY);
    }

    @Test
    void should_select_custom_policy_factory() {
        final MyDefaultPolicyFactory customDefaultPolicyFactory = new MyDefaultPolicyFactory();
        cut =
            new PolicyFactoryManager(
                MyDefaultPolicyFactory.class,
                new HashSet<>(Set.of(fakePolicyFactory(manifest -> false), customDefaultPolicyFactory))
            );
        assertThat(cut.get(fakePolicyManifest("unknown"))).isEqualTo(customDefaultPolicyFactory);
    }

    @Test
    void should_select_default_policy_factory_if_no_appropriate_custom_factory() {
        cut =
            new PolicyFactoryManager(
                new HashSet<>(
                    Set.of(
                        DEFAULT_POLICY_FACTORY,
                        fakePolicyFactory(manifest -> manifest.id().equals("shared-policy-group-policy")),
                        fakePolicyFactory(manifest -> manifest.id().equals("other-internal-policy"))
                    )
                )
            );
        assertThat(cut.get(fakePolicyManifest("unknown"))).isEqualTo(DEFAULT_POLICY_FACTORY);
    }

    @Test
    void should_select_appropriate_policy_factory() {
        final PolicyFactory expectedPolicyFactory = fakePolicyFactory(manifest -> manifest.id().equals("other-internal-policy"));
        cut =
            new PolicyFactoryManager(
                new HashSet<>(
                    Set.of(
                        DEFAULT_POLICY_FACTORY,
                        fakePolicyFactory(manifest -> manifest.id().equals("shared-policy-group-policy")),
                        expectedPolicyFactory
                    )
                )
            );
        assertThat(cut.get(fakePolicyManifest("other-internal-policy"))).isEqualTo(expectedPolicyFactory);
    }

    @Test
    void should_cleanup_a_manifest() {
        final DefaultPolicyFactory policyFactoryMock = mock(DefaultPolicyFactory.class);
        cut = new PolicyFactoryManager(new HashSet<>(Set.of(policyFactoryMock)));
        final PolicyManifest policyManifest = fakePolicyManifest("unknown");
        cut.cleanup(policyManifest);
        verify(policyFactoryMock).cleanup(policyManifest);
    }

    private PolicyFactory fakePolicyFactory(Predicate<PolicyManifest> policyManifestPredicate) {
        return new PolicyFactory() {
            public boolean hasBeenCleaned;

            @Override
            public boolean accept(PolicyManifest policyManifest) {
                return policyManifestPredicate.test(policyManifest);
            }

            @Override
            public Policy create(
                ExecutionPhase phase,
                PolicyManifest policyManifest,
                PolicyConfiguration policyConfiguration,
                PolicyMetadata policyMetadata
            ) {
                return null;
            }

            @Override
            public void cleanup(PolicyManifest policyManifest) {
                hasBeenCleaned = true;
            }
        };
    }

    private PolicyManifest fakePolicyManifest(String id) {
        return new PolicyManifestBuilder().setId(id).build();
    }

    class MyDefaultPolicyFactory implements PolicyFactory {

        @Override
        public boolean accept(PolicyManifest policyManifest) {
            return false;
        }

        @Override
        public Policy create(
            ExecutionPhase phase,
            PolicyManifest policyManifest,
            PolicyConfiguration policyConfiguration,
            PolicyMetadata policyMetadata
        ) {
            return null;
        }

        @Override
        public void cleanup(PolicyManifest policyManifest) {}
    }
}
