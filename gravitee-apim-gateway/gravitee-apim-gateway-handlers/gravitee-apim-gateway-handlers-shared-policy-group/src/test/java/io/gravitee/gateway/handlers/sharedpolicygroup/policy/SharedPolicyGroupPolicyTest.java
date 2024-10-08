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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.gateway.handlers.sharedpolicygroup.ReactableSharedPolicyGroup;
import io.gravitee.gateway.handlers.sharedpolicygroup.reactor.SharedPolicyGroupReactor;
import io.gravitee.gateway.handlers.sharedpolicygroup.registry.SharedPolicyGroupRegistry;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.policy.HttpPolicyChain;
import io.reactivex.rxjava3.core.Completable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SharedPolicyGroupPolicyTest {

    public static final String SHARED_POLICY_GROUP_ID = "sharedPolicyGroupId";
    public static final String ENVIRONMENT_ID = "environmentId";

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private SharedPolicyGroupRegistry sharedPolicyGroupRegistry;

    @Mock
    private HttpPolicyChain policyChain;

    private ListAppender<ILoggingEvent> listAppender;
    private SharedPolicyGroupPolicy cut;

    @BeforeEach
    void setUp() {
        // get Logback Logger
        Logger logger = (Logger) LoggerFactory.getLogger(SharedPolicyGroupPolicy.class);

        // create and start a ListAppender
        listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        // addAppender is outdated now
        logger.addAppender(listAppender);

        when(executionContext.getComponent(SharedPolicyGroupRegistry.class)).thenReturn(sharedPolicyGroupRegistry);
        lenient().when(policyChain.execute(executionContext)).thenReturn(Completable.complete());
        when(executionContext.getAttribute(ContextAttributes.ATTR_ENVIRONMENT)).thenReturn(ENVIRONMENT_ID);
        final SharedPolicyGroupPolicyConfiguration policyConfiguration = new SharedPolicyGroupPolicyConfiguration();
        policyConfiguration.setSharedPolicyGroupId(SHARED_POLICY_GROUP_ID);
        cut = new SharedPolicyGroupPolicy("id", policyConfiguration);
    }

    @Test
    void should_execute_policy_chain_for_existing_shared_policy_group_on_request() {
        when(sharedPolicyGroupRegistry.get(eq(SHARED_POLICY_GROUP_ID), any())).thenReturn(new FakeSharedPolicyGroupReactor(policyChain));
        cut.onRequest(executionContext).test().assertComplete();
        verify(policyChain).execute(executionContext);
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    void should_complete_directly_if_no_shared_policy_group_on_request() {
        when(sharedPolicyGroupRegistry.get(eq(SHARED_POLICY_GROUP_ID), any())).thenReturn(null);
        cut.onRequest(executionContext).test().assertComplete();
        verify(policyChain, never()).execute(executionContext);
        assertThat(listAppender.list)
            .hasSize(1)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("No Shared Policy Group found for id sharedPolicyGroupId on environment environmentId");
    }

    @Test
    void should_execute_policy_chain_for_existing_shared_policy_group_on_response() {
        when(sharedPolicyGroupRegistry.get(eq(SHARED_POLICY_GROUP_ID), any())).thenReturn(new FakeSharedPolicyGroupReactor(policyChain));
        cut.onResponse(executionContext).test().assertComplete();
        verify(policyChain).execute(executionContext);
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    void should_complete_directly_if_no_shared_policy_group_on_response() {
        when(sharedPolicyGroupRegistry.get(eq(SHARED_POLICY_GROUP_ID), any())).thenReturn(null);
        cut.onResponse(executionContext).test().assertComplete();
        verify(policyChain, never()).execute(executionContext);
        assertThat(listAppender.list)
            .hasSize(1)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly("No Shared Policy Group found for id sharedPolicyGroupId on environment environmentId");
    }

    static class FakeSharedPolicyGroupReactor implements SharedPolicyGroupReactor {

        private final HttpPolicyChain chain;

        public FakeSharedPolicyGroupReactor(HttpPolicyChain policyChain) {
            chain = policyChain;
        }

        @Override
        public String id() {
            return SHARED_POLICY_GROUP_ID;
        }

        @Override
        public ReactableSharedPolicyGroup reactableSharedPolicyGroup() {
            return null;
        }

        @Override
        public HttpPolicyChain policyChain() {
            return chain;
        }

        @Override
        public Lifecycle.State lifecycleState() {
            return null;
        }

        @Override
        public SharedPolicyGroupReactor start() throws Exception {
            return null;
        }

        @Override
        public SharedPolicyGroupReactor stop() throws Exception {
            return null;
        }
    }
}
