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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.definition.model.guardian.GuardianPolicyConfiguration;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.reactor.GuardianReactor;
import io.gravitee.gateway.handlers.guardian.registry.GuardianRegistry;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.reactivex.rxjava3.core.Completable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianPolicyTest {

    private static final String GUARDIAN_ID = "guardian-id";
    private static final String ENVIRONMENT_ID = "env-id";

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private GuardianRegistry guardianRegistry;

    @Mock
    private HttpPolicyChain policyChain;

    private final Logger logger = (Logger) LoggerFactory.getLogger(GuardianPolicy.class);
    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    private GuardianPolicy cut;

    @BeforeEach
    void setUp() {
        listAppender.start();
        logger.addAppender(listAppender);
        lenient().when(executionContext.withLogger(any())).thenReturn(logger);
        lenient().when(policyChain.execute(executionContext)).thenReturn(Completable.complete());
        lenient().when(executionContext.getComponent(GuardianRegistry.class)).thenReturn(guardianRegistry);
        lenient().when(executionContext.getAttribute(ContextAttributes.ATTR_ENVIRONMENT)).thenReturn(ENVIRONMENT_ID);

        GuardianPolicyConfiguration configuration = new GuardianPolicyConfiguration();
        configuration.setGuardianId(GUARDIAN_ID);
        cut = new GuardianPolicy("my-guardian-step", configuration);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void should_expose_the_step_name_as_id() {
        assertThat(cut.id()).isEqualTo("my-guardian-step");
    }

    @Test
    void should_execute_guardian_policy_chain_on_request() {
        when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(new FakeGuardianReactor(policyChain));

        cut.onRequest(executionContext).test().assertComplete();

        verify(policyChain).execute(executionContext);
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    void should_execute_guardian_policy_chain_on_response() {
        when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(new FakeGuardianReactor(policyChain));

        cut.onResponse(executionContext).test().assertComplete();

        verify(policyChain).execute(executionContext);
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    void should_complete_and_warn_when_guardian_is_not_deployed_on_request() {
        when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(null);

        cut.onRequest(executionContext).test().assertComplete();

        verify(policyChain, never()).execute(any());
        assertThat(listAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("No Guardian found for id guardian-id on environment env-id");
    }

    @Test
    void should_complete_and_warn_when_guardian_is_not_deployed_on_response() {
        when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(null);

        cut.onResponse(executionContext).test().assertComplete();

        verify(policyChain, never()).execute(any());
        assertThat(listAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("No Guardian found for id guardian-id on environment env-id");
    }

    private record FakeGuardianReactor(HttpPolicyChain policyChain) implements GuardianReactor {
        @Override
        public String id() {
            return GUARDIAN_ID;
        }

        @Override
        public ReactableGuardian reactableGuardian() {
            return ReactableGuardian.builder().id(GUARDIAN_ID).environmentId(ENVIRONMENT_ID).build();
        }
    }
}
