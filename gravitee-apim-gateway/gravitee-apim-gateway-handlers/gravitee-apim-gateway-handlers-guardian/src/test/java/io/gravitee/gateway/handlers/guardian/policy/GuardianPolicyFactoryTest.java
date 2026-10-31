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
package io.gravitee.gateway.handlers.guardian.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.guardian.GuardianPolicyConfiguration;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.core.condition.ExpressionLanguageConditionFilter;
import io.gravitee.gateway.reactive.policy.HttpConditionalPolicy;
import io.gravitee.node.api.configuration.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianPolicyFactoryTest {

    @Mock
    private Configuration configuration;

    @Mock
    private PolicyPluginFactory policyPluginFactory;

    @Mock
    private PolicyManifest policyManifest;

    private GuardianPolicyFactory cut;

    @BeforeEach
    void setUp() {
        cut = new GuardianPolicyFactory(configuration, policyPluginFactory, new ExpressionLanguageConditionFilter<>());
    }

    @Test
    void should_only_accept_the_guardian_policy() {
        when(policyManifest.id()).thenReturn(GuardianPolicy.POLICY_ID, "transform-headers");

        assertThat(cut.accept(policyManifest)).isTrue();
        assertThat(cut.accept(policyManifest)).isFalse();
    }

    @Test
    void should_create_guardian_policy_named_after_its_step() {
        PolicyMetadata metadata = new PolicyMetadata("my-guardian-step", "{}", null);

        HttpPolicy policy = cut.create(ExecutionPhase.REQUEST, policyManifest, new GuardianPolicyConfiguration(), metadata);

        assertThat(policy).isInstanceOf(GuardianPolicy.class);
        assertThat(policy.id()).isEqualTo("my-guardian-step");
    }

    @Test
    void should_wrap_guardian_policy_in_a_conditional_policy_when_step_has_a_condition() {
        PolicyMetadata metadata = new PolicyMetadata("my-guardian-step", "{}", "{#request.headers['X-Guard'] != null}");

        HttpPolicy policy = cut.create(ExecutionPhase.REQUEST, policyManifest, new GuardianPolicyConfiguration(), metadata);

        assertThat(policy).isInstanceOf(HttpConditionalPolicy.class);
    }
}
