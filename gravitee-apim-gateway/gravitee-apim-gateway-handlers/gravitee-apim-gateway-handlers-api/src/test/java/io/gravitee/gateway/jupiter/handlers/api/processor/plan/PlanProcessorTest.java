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
package io.gravitee.gateway.jupiter.handlers.api.processor.plan;

import static io.gravitee.gateway.api.ExecutionContext.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.handlers.api.processor.AbstractProcessorTest;
import io.gravitee.gateway.jupiter.handlers.api.security.SecurityChain;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class PlanProcessorTest extends AbstractProcessorTest {

    protected static final String PLAN_ID = "planId";
    protected static final String APPLICATION_ID = "applicationId";
    protected static final String SUBSCRIPTION_ID = "subscriptionId";
    protected static final String REMOTE_ADDRESS = "remoteAddress";

    @Mock
    private RequestExecutionContext ctx;

    private PlanProcessor cut;

    @BeforeEach
    void initProcessor() {
        when(ctx.request()).thenReturn(mockRequest);
        cut = PlanProcessor.instance();
    }

    @Test
    void shouldSetMetricsWhenSecurityChainIsNotSkipped() {
        when(ctx.getAttribute(SecurityChain.SKIP_SECURITY_CHAIN)).thenReturn(null);
        when(ctx.getAttribute(ATTR_PLAN)).thenReturn(PLAN_ID);
        when(ctx.getAttribute(ATTR_APPLICATION)).thenReturn(APPLICATION_ID);
        when(ctx.getAttribute(ATTR_SUBSCRIPTION_ID)).thenReturn(SUBSCRIPTION_ID);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertResult();

        verify(mockMetrics).setPlan(PLAN_ID);
        verify(mockMetrics).setApplication(APPLICATION_ID);
        verify(mockMetrics).setSubscription(SUBSCRIPTION_ID);
    }

    @Test
    void shouldSetMetricsUnknownApplicationWhenSecurityChainIsSkipped() {
        when(ctx.getAttribute(SecurityChain.SKIP_SECURITY_CHAIN)).thenReturn(true);
        when(mockRequest.remoteAddress()).thenReturn(REMOTE_ADDRESS);

        final TestObserver<Void> obs = cut.execute(ctx).test();
        obs.assertResult();

        verify(mockMetrics).setPlan(PlanProcessor.PLAN_NAME_ANONYMOUS);
        verify(mockMetrics).setApplication(PlanProcessor.APPLICATION_NAME_ANONYMOUS);
        verify(mockMetrics).setSubscription(REMOTE_ADDRESS);
    }
}
