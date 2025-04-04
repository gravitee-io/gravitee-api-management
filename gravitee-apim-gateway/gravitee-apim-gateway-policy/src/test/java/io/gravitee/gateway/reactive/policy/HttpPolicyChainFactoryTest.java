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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.definition.model.flow.StepV2;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.Policy;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpPolicyChainFactoryTest {

    @Mock
    private PolicyManager policyManager;

    private HttpPolicyChainFactory cut;

    @BeforeEach
    public void init() {
        cut = new HttpPolicyChainFactory("unit-test", policyManager, false);
    }

    @Test
    public void shouldCreatePolicyChainForRequestPhase() {
        final Policy policy = mock(Policy.class);
        final StepV2 step1 = StepV2
            .builder()
            .enabled(true)
            .policy("policy-step1")
            .configuration("config-step1")
            .condition("condition-step1")
            .build();
        final StepV2 step2 = StepV2
            .builder()
            .enabled(true)
            .policy("policy-step2")
            .configuration("config-step2")
            .condition("condition-step2")
            .build();
        final FlowV2Impl flow = FlowV2Impl.builder().pre(List.of(step1, step2)).build();

        when(policyManager.create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class))).thenReturn(policy);

        final HttpPolicyChain policyChain = cut.create("flow-chain-test", flow, ExecutionPhase.REQUEST);
        assertNotNull(policyChain);

        verify(policyManager, times(1))
            .create(
                eq(ExecutionPhase.REQUEST),
                argThat(metadata ->
                    metadata.getName().equals("policy-step1") &&
                    metadata.getConfiguration().equals("config-step1") &&
                    metadata.getCondition().equals("condition-step1")
                )
            );

        verify(policyManager, times(1))
            .create(
                eq(ExecutionPhase.REQUEST),
                argThat(metadata ->
                    metadata.getName().equals("policy-step2") &&
                    metadata.getConfiguration().equals("config-step2") &&
                    metadata.getCondition().equals("condition-step2") &&
                    metadata.metadata().get(PolicyMetadata.MetadataKeys.EXECUTION_MODE).equals(ExecutionMode.V4_EMULATION_ENGINE)
                )
            );

        verifyNoMoreInteractions(policyManager);
    }

    @Test
    public void shouldCreatePolicyChainWithoutDisabledSteps() {
        final Policy policy = mock(Policy.class);
        final StepV2 step1 = StepV2
            .builder()
            .enabled(false)
            .policy("policy-step1")
            .configuration("config-step1")
            .condition("condition-step1")
            .build();
        final StepV2 step2 = StepV2
            .builder()
            .enabled(true)
            .policy("policy-step2")
            .configuration("config-step2")
            .condition("condition-step2")
            .build();
        final FlowV2Impl flow = FlowV2Impl.builder().pre(List.of(step1, step2)).build();

        when(policyManager.create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class))).thenReturn(policy);

        final HttpPolicyChain policyChain = cut.create("flow-chain-test", flow, ExecutionPhase.REQUEST);
        assertNotNull(policyChain);

        verify(policyManager, times(1))
            .create(
                eq(ExecutionPhase.REQUEST),
                argThat(metadata ->
                    metadata.getName().equals("policy-step2") &&
                    metadata.getConfiguration().equals("config-step2") &&
                    metadata.getCondition().equals("condition-step2") &&
                    metadata.metadata().get(PolicyMetadata.MetadataKeys.EXECUTION_MODE).equals(ExecutionMode.V4_EMULATION_ENGINE)
                )
            );

        verifyNoMoreInteractions(policyManager);
    }

    @Test
    public void shouldCreatePolicyChainOnceAndPutInCache() {
        final Policy policy = mock(Policy.class);
        final StepV2 step1 = StepV2
            .builder()
            .enabled(true)
            .policy("policy-step1")
            .configuration("config-step1")
            .condition("condition-step1")
            .build();
        final StepV2 step2 = StepV2
            .builder()
            .enabled(true)
            .policy("policy-step2")
            .configuration("config-step2")
            .condition("condition-step2")
            .build();
        final FlowV2Impl flow = FlowV2Impl.builder().pre(List.of(step1, step2)).build();

        when(policyManager.create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class))).thenReturn(policy);

        for (int i = 0; i < 10; i++) {
            cut.create("flow-chain-test", flow, ExecutionPhase.REQUEST);
        }

        verify(policyManager, times(1))
            .create(
                eq(ExecutionPhase.REQUEST),
                argThat(metadata ->
                    metadata.getName().equals("policy-step1") &&
                    metadata.getConfiguration().equals("config-step1") &&
                    metadata.getCondition().equals("condition-step1") &&
                    metadata.metadata().get(PolicyMetadata.MetadataKeys.EXECUTION_MODE).equals(ExecutionMode.V4_EMULATION_ENGINE)
                )
            );

        verify(policyManager, times(1))
            .create(
                eq(ExecutionPhase.REQUEST),
                argThat(metadata ->
                    metadata.getName().equals("policy-step2") &&
                    metadata.getConfiguration().equals("config-step2") &&
                    metadata.getCondition().equals("condition-step2") &&
                    metadata.metadata().get(PolicyMetadata.MetadataKeys.EXECUTION_MODE).equals(ExecutionMode.V4_EMULATION_ENGINE)
                )
            );

        verifyNoMoreInteractions(policyManager);
    }

    @Test
    public void shouldCreatePolicyChainForResponsePhase() {
        final Policy policy = mock(Policy.class);
        final StepV2 step1 = StepV2
            .builder()
            .enabled(true)
            .policy("policy-step1")
            .configuration("config-step1")
            .condition("condition-step1")
            .build();
        final FlowV2Impl flow = FlowV2Impl.builder().post(List.of(step1)).build();

        when(policyManager.create(eq(ExecutionPhase.RESPONSE), any(PolicyMetadata.class))).thenReturn(policy);

        final HttpPolicyChain policyChain = cut.create("flow-chain-test", flow, ExecutionPhase.RESPONSE);
        assertNotNull(policyChain);

        verify(policyManager, times(1)).create(eq(ExecutionPhase.RESPONSE), any(PolicyMetadata.class));
        verifyNoMoreInteractions(policyManager);
    }

    @Test
    public void shouldFilterNullPoliciesReturnedByPolicyManager() {
        final Policy policy = mock(Policy.class);
        final StepV2 step1 = StepV2
            .builder()
            .enabled(true)
            .policy("policy-step1")
            .configuration("config-step1")
            .condition("condition-step1")
            .build();
        final StepV2 step2 = StepV2
            .builder()
            .enabled(true)
            .policy("policy-step2")
            .configuration("config-step2")
            .condition("condition-step2")
            .build();
        final FlowV2Impl flow = FlowV2Impl.builder().pre(List.of(step1, step2)).build();

        when(policyManager.create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class))).thenReturn(null).thenReturn(policy);

        final HttpPolicyChain policyChain = cut.create("flow-chain-test", flow, ExecutionPhase.REQUEST);
        assertNotNull(policyChain);

        verify(policyManager, times(2)).create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class));

        verifyNoMoreInteractions(policyManager);
    }

    @Test
    public void shouldNoCreateAnyPolicyWhenUnsupportedPhase() {
        final FlowV2Impl flow = mock(FlowV2Impl.class);

        final HttpPolicyChain policyChain = cut.create("flow-chain-test", flow, ExecutionPhase.MESSAGE_REQUEST);
        assertNotNull(policyChain);

        verifyNoInteractions(policyManager);
    }
}
