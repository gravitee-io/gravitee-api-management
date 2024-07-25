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
package io.gravitee.apim.infra.domain_service.policy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.plugin.model.PolicyPlugin;
import io.gravitee.apim.core.policy.exception.UnexpectedPoliciesException;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.v4.policy.ExecutionPhase;
import io.gravitee.rest.api.model.v4.policy.PolicyPluginEntity;
import io.gravitee.rest.api.service.v4.PolicyPluginService;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PolicyValidationDomainServiceTest {

    PolicyPluginService policyPluginService = mock(PolicyPluginService.class);
    PolicyValidationDomainServiceLegacyWrapper service = new PolicyValidationDomainServiceLegacyWrapper(policyPluginService);

    @Nested
    class validatePoliciesExecutionPhase {

        @Test
        void should_validate() {
            // Given
            when(policyPluginService.findAll())
                .thenReturn(
                    Set.of(
                        PolicyPluginEntity.builder().id("policy-1").proxy(Set.of(ExecutionPhase.REQUEST, ExecutionPhase.RESPONSE)).build(),
                        PolicyPluginEntity.builder().id("policy-2").proxy(Set.of(ExecutionPhase.REQUEST)).build()
                    )
                );
            // When
            service.validatePoliciesExecutionPhase(List.of("policy-1", "policy-2"), ApiType.PROXY, PolicyPlugin.ExecutionPhase.REQUEST);
        }

        @Test
        void should_throw_exception_when_a_policy_not_match_api_type() {
            // Given
            when(policyPluginService.findAll())
                .thenReturn(
                    Set.of(
                        PolicyPluginEntity
                            .builder()
                            .id("policy-1")
                            .name("Policy 1")
                            .proxy(Set.of(ExecutionPhase.REQUEST, ExecutionPhase.RESPONSE))
                            .build(),
                        PolicyPluginEntity.builder().id("policy-2").name("Policy 2").message(Set.of(ExecutionPhase.MESSAGE_REQUEST)).build()
                    )
                );

            // When
            var throwable = Assertions.catchThrowable(() ->
                service.validatePoliciesExecutionPhase(List.of("policy-1", "policy-2"), ApiType.PROXY, PolicyPlugin.ExecutionPhase.RESPONSE)
            );

            // Then
            Assertions
                .assertThat(throwable)
                .isInstanceOf(UnexpectedPoliciesException.class)
                .hasMessage("Unexpected policies [Policy 2] for API type PROXY and phase RESPONSE");
        }

        @Test
        void should_throw_exception_when_a_policies_not_match_execution_phase() {
            // Given
            when(policyPluginService.findAll())
                .thenReturn(
                    Set.of(
                        PolicyPluginEntity
                            .builder()
                            .id("policy-1")
                            .name("Policy 1")
                            .message(Set.of(ExecutionPhase.MESSAGE_REQUEST, ExecutionPhase.MESSAGE_RESPONSE))
                            .build(),
                        PolicyPluginEntity
                            .builder()
                            .id("policy-2")
                            .name("Policy 2")
                            .message(Set.of(ExecutionPhase.MESSAGE_REQUEST))
                            .build(),
                        PolicyPluginEntity.builder().id("policy-3").name("Policy 3").build()
                    )
                );
            // When

            var throwable = Assertions.catchThrowable(() ->
                service.validatePoliciesExecutionPhase(
                    List.of("policy-1", "policy-2", "policy-3"),
                    ApiType.MESSAGE,
                    PolicyPlugin.ExecutionPhase.MESSAGE_RESPONSE
                )
            );

            // Then
            Assertions
                .assertThat(throwable)
                .isInstanceOf(UnexpectedPoliciesException.class)
                .hasMessage("Unexpected policies [Policy 2, Policy 3] for API type MESSAGE and phase MESSAGE_RESPONSE");
        }
    }
}
