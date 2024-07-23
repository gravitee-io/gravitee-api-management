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
package io.gravitee.gateway.handlers.sharedpolicygroup.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.reactive.policy.PolicyChain;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.gravitee.gateway.reactive.policy.tracing.TracingPolicyHook;
import io.gravitee.node.api.configuration.Configuration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultSharedPolicyGroupPolicyChainFactoryTest {

    public static final Step STEP_FOR_ALREADY_REGISTERED_CHAIN = Step
        .builder()
        .name("in-a-registered-chain")
        .policy("policy")
        .enabled(true)
        .build();
    public static final String SHARED_GROUP_POLICY_ID = "shared-group-policy-id";
    public static final String ENV_ID = "envId";

    @Mock
    private PolicyManager policyManager;

    @Mock
    private Configuration configuration;

    private DefaultSharedPolicyGroupPolicyChainFactory cut;

    @BeforeEach
    void setUp() {
        when(configuration.getProperty("services.tracing.enabled", Boolean.class, false)).thenReturn(false);
        cut = new DefaultSharedPolicyGroupPolicyChainFactory("id", policyManager, configuration);
    }

    @Test
    void should_add_tracing_hook() {
        when(configuration.getProperty("services.tracing.enabled", Boolean.class, false)).thenReturn(true);

        cut = new DefaultSharedPolicyGroupPolicyChainFactory("id", policyManager, configuration);
        assertThat(cut.policyHooks).hasSize(1).first().isInstanceOf(TracingPolicyHook.class);
    }

    @Test
    void should_not_create_policy_chain_from_a_flow() {
        assertThatThrownBy(() -> cut.create("id", Flow.builder().build(), ExecutionPhase.REQUEST))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot build a policy chain from a Flow for Shared Policy Group");
    }

    @Test
    void should_not_create_policy_chain_if_already_registered_and_return_it() {
        final List<Step> steps = List.of(STEP_FOR_ALREADY_REGISTERED_CHAIN);
        final PolicyChain expectedPolicyChain = new PolicyChain("result", List.of(), ExecutionPhase.REQUEST);
        cut.policyChains.put(
            cut.getSharedPolicyGroupKey(SHARED_GROUP_POLICY_ID, ENV_ID, steps, ExecutionPhase.REQUEST),
            expectedPolicyChain
        );
        final PolicyChain result = cut.create(SHARED_GROUP_POLICY_ID, ENV_ID, steps, ExecutionPhase.REQUEST);

        assertThat(result).isEqualTo(expectedPolicyChain);

        verify(policyManager, never()).create(any(), any());
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void should_create_policy_chain(List<Step> steps, ExecutionPhase phase) {
        Logger logger = (Logger) LoggerFactory.getLogger(DefaultSharedPolicyGroupPolicyChainFactory.class);

        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        lenient()
            .when(policyManager.create(any(), any()))
            .thenAnswer(answer -> fakePolicy(answer.<PolicyMetadata>getArgument(1).getName()));
        final PolicyChain result = cut.create(SHARED_GROUP_POLICY_ID, ENV_ID, steps, phase);
        int nestedSharedPolicyGroupLog = 0;
        for (Step step : steps) {
            final boolean isSharedPolicyGroup = step.getPolicy().equals(SharedPolicyGroupPolicy.POLICY_ID);
            if (isSharedPolicyGroup && step.isEnabled()) {
                nestedSharedPolicyGroupLog++;
            }
            verify(policyManager, step.isEnabled() && !isSharedPolicyGroup ? times(1) : never())
                .create(eq(phase), argThat(policyMetadata -> policyMetadata.getName().equals(step.getPolicy())));
        }

        assertThat(listAppender.list)
            .hasSize(nestedSharedPolicyGroupLog)
            .allMatch(log ->
                log.getLevel().equals(Level.WARN) &&
                log.getMessage().equals("Nested Shared Policy Group is not supported. The Shared Policy Group {} will be ignored")
            );
    }

    public static Stream<Arguments> provideTestData() {
        return Stream.of(
            Arguments.of(List.of(Step.builder().policy("policy").enabled(true).build()), ExecutionPhase.REQUEST),
            Arguments.of(List.of(Step.builder().policy("policy").enabled(false).build()), ExecutionPhase.REQUEST),
            Arguments.of(
                List.of(
                    Step.builder().policy("policy-1").enabled(false).build(),
                    Step.builder().policy(SharedPolicyGroupPolicy.POLICY_ID).enabled(false).build(),
                    Step.builder().policy(SharedPolicyGroupPolicy.POLICY_ID).enabled(true).build()
                ),
                ExecutionPhase.REQUEST
            )
        );
    }

    Policy fakePolicy(String id) {
        return new Policy() {
            @Override
            public String id() {
                return id;
            }
        };
    }
}
