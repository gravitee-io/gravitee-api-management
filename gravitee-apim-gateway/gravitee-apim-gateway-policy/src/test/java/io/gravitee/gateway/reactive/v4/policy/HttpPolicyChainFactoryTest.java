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
package io.gravitee.gateway.reactive.v4.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.node.container.spring.SpringEnvironmentConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;

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
        cut = new HttpPolicyChainFactory("unit-test", policyManager, new SpringEnvironmentConfiguration(new StandardEnvironment()));
    }

    @Test
    public void shouldCreatePolicyChainForRequestPhase() {
        final Policy policy = mock(Policy.class);
        final Flow flow = mock(Flow.class);
        final Step step1 = mock(Step.class);
        final Step step2 = mock(Step.class);

        when(step1.isEnabled()).thenReturn(true);
        when(step2.isEnabled()).thenReturn(true);
        when(flow.getRequest()).thenReturn(List.of(step1, step2));

        when(policyManager.create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class))).thenReturn(policy);

        when(step1.getPolicy()).thenReturn("policy-step1");
        when(step1.getConfiguration()).thenReturn("config-step1");
        when(step1.getCondition()).thenReturn("condition-step1");

        when(step2.getPolicy()).thenReturn("policy-step2");
        when(step2.getConfiguration()).thenReturn("config-step2");
        when(step2.getCondition()).thenReturn("condition-step2");

        final HttpPolicyChain policyChain = cut.create("fowchain-test", flow, ExecutionPhase.REQUEST);
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
        final Flow flow = mock(Flow.class);
        final Step step1 = mock(Step.class);
        final Step step2 = mock(Step.class);

        when(step1.isEnabled()).thenReturn(false);
        when(step2.isEnabled()).thenReturn(true);
        when(flow.getRequest()).thenReturn(List.of(step1, step2));

        when(policyManager.create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class))).thenReturn(policy);

        when(step2.getPolicy()).thenReturn("policy-step2");
        when(step2.getConfiguration()).thenReturn("config-step2");
        when(step2.getCondition()).thenReturn("condition-step2");

        final HttpPolicyChain policyChain = cut.create("fowchain-test", flow, ExecutionPhase.REQUEST);
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
        final Flow flow = mock(Flow.class);
        final Step step1 = mock(Step.class);
        final Step step2 = mock(Step.class);

        when(step1.isEnabled()).thenReturn(true);
        when(step2.isEnabled()).thenReturn(true);
        when(flow.getRequest()).thenReturn(List.of(step1, step2));

        when(policyManager.create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class))).thenReturn(policy);

        when(step1.getPolicy()).thenReturn("policy-step1");
        when(step1.getConfiguration()).thenReturn("config-step1");
        when(step1.getCondition()).thenReturn("condition-step1");

        when(step2.getPolicy()).thenReturn("policy-step2");
        when(step2.getConfiguration()).thenReturn("config-step2");
        when(step2.getCondition()).thenReturn("condition-step2");

        for (int i = 0; i < 10; i++) {
            cut.create("fowchain-test", flow, ExecutionPhase.REQUEST);
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
        final Flow flow = mock(Flow.class);
        final Step step1 = mock(Step.class);

        when(step1.isEnabled()).thenReturn(true);
        when(flow.getResponse()).thenReturn(List.of(step1));

        when(policyManager.create(eq(ExecutionPhase.RESPONSE), any(PolicyMetadata.class))).thenReturn(policy);

        final HttpPolicyChain policyChain = cut.create("fowchain-test", flow, ExecutionPhase.RESPONSE);
        assertNotNull(policyChain);

        verify(policyManager, times(1)).create(eq(ExecutionPhase.RESPONSE), any(PolicyMetadata.class));
        verifyNoMoreInteractions(policyManager);
    }

    @Test
    public void shouldFilterNullPoliciesReturnedByPolicyManager() {
        final Policy policy = mock(Policy.class);
        final Flow flow = mock(Flow.class);
        final Step step1 = mock(Step.class);
        final Step step2 = mock(Step.class);

        when(step1.isEnabled()).thenReturn(true);
        when(step2.isEnabled()).thenReturn(true);
        when(flow.getRequest()).thenReturn(List.of(step1, step2));

        when(policyManager.create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class))).thenReturn(null).thenReturn(policy);

        when(step1.getPolicy()).thenReturn("policy-step1");
        when(step1.getConfiguration()).thenReturn("config-step1");
        when(step1.getCondition()).thenReturn("condition-step1");

        when(step2.getPolicy()).thenReturn("policy-step2");
        when(step2.getConfiguration()).thenReturn("config-step2");
        when(step2.getCondition()).thenReturn("condition-step2");

        final HttpPolicyChain policyChain = cut.create("fowchain-test", flow, ExecutionPhase.REQUEST);
        assertNotNull(policyChain);

        verify(policyManager, times(2)).create(eq(ExecutionPhase.REQUEST), any(PolicyMetadata.class));

        verifyNoMoreInteractions(policyManager);
    }

    @Test
    public void shouldNotFailIfFlowMethodsIsNull() {
        final Flow flow = mock(Flow.class);
        final HttpSelector selector = mock(HttpSelector.class);

        when(flow.selectorByType(eq(SelectorType.HTTP))).thenReturn(Optional.of(selector));
        when(selector.getMethods()).thenReturn(null);
        when(selector.getPath()).thenReturn("path");

        final HttpPolicyChain policyChain = cut.create("flowchain-test", flow, ExecutionPhase.REQUEST);
        assertNotNull(policyChain);
        assertEquals("flowchain-test-all-path", policyChain.getId());
    }
}
