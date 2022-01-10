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
package io.gravitee.gateway.policy.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.el.TemplateEngine;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.policy.DummyPolicy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyConfiguration;
import java.lang.reflect.Method;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ConditionalExecutablePolicyTest extends TestCase {

    private ConditionalExecutablePolicy cut;

    @Mock
    private PolicyChain policyChain;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private TemplateEngine templateEngine;

    private Method method;

    @Before
    public void setUp() throws NoSuchMethodException {
        when(executionContext.request()).thenReturn(mock(Request.class));
        when(executionContext.response()).thenReturn(mock(Response.class));
        method = DummyPolicy.class.getMethod("onRequest", PolicyChain.class, Request.class, Response.class);
    }

    @Test
    public void shouldExecuteConditionalPolicyConditionOk() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(true);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy("dummy", fakePolicy(), method, method, "condition");
        policy.execute(policyChain, executionContext);
        verify(policyChain, never()).doNext(any(), any());
    }

    @Test
    public void shouldNotExecuteConditionalPolicyConditionEvaluatedToFalse() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(false);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy("dummy", fakePolicy(), method, method, "condition");
        policy.execute(policyChain, executionContext);
        verify(policyChain, times(1)).doNext(any(), any());
    }

    @Test(expected = PolicyException.class)
    public void shouldNotExecuteConditionalPolicyExpressionEvaluationException() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenThrow(ExpressionEvaluationException.class);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy("dummy", fakePolicy(), method, method, "condition");
        policy.execute(policyChain, executionContext);
    }

    @Test
    public void shouldStreamConditionalPolicyConditionOk() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(true);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy("dummy", fakePolicy(), method, method, "condition");
        policy.stream(policyChain, executionContext);
        verify(policyChain, never()).doNext(any(), any());
    }

    @Test(expected = PolicyException.class)
    public void shouldNotStreamConditionalPolicyExpressionEvaluationException() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenThrow(ExpressionEvaluationException.class);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy("dummy", fakePolicy(), method, method, "condition");
        policy.stream(policyChain, executionContext);
    }

    private Object fakePolicy() {
        return new PolicyPluginFactoryImpl().create(DummyPolicy.class, mock(PolicyConfiguration.class));
    }
}
