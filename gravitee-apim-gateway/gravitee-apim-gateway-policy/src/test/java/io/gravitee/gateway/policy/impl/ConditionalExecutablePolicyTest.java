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
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.gateway.policy.dummy.DummyPolicy;
import io.gravitee.gateway.policy.dummy.DummyStreamablePolicy;
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

    @Mock
    private PolicyChain policyChain;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private TemplateEngine templateEngine;

    private Method method;
    private ConditionEvaluator<String> conditionEvaluator;

    @Before
    public void setUp() throws NoSuchMethodException {
        when(executionContext.request()).thenReturn(mock(Request.class));
        when(executionContext.response()).thenReturn(mock(Response.class));
        method = DummyPolicy.class.getMethod("onRequest", PolicyChain.class, Request.class, Response.class);
        conditionEvaluator = new ExpressionLanguageStringConditionEvaluator();
    }

    @Test
    public void shouldExecuteConditionalPolicyConditionOk() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(true);

        Policy delegatePolicy = new ExecutablePolicy("dummy", fakeExecutePolicy(), method, method);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy(delegatePolicy, "condition", conditionEvaluator);
        policy.execute(policyChain, executionContext);
        verify(policyChain, never()).doNext(any(), any());
    }

    @Test
    public void shouldNotExecuteConditionalPolicyConditionEvaluatedToFalse() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(false);
        Policy delegatePolicy = new ExecutablePolicy("dummy", fakeExecutePolicy(), method, method);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy(delegatePolicy, "condition", conditionEvaluator);
        policy.execute(policyChain, executionContext);
        verify(policyChain, times(1)).doNext(any(), any());
    }

    @Test(expected = PolicyException.class)
    public void shouldNotExecuteConditionalPolicyExpressionEvaluationException() throws PolicyException {
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenThrow(ExpressionEvaluationException.class);
        Policy delegatePolicy = new ExecutablePolicy("dummy", fakeExecutePolicy(), method, method);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy(delegatePolicy, "condition", conditionEvaluator);
        policy.execute(policyChain, executionContext);
    }

    @Test
    public void shouldStreamConditionalPolicyConditionOk() throws PolicyException, NoSuchMethodException {
        method = DummyStreamablePolicy.class.getMethod("onResponseContent", Response.class, ExecutionContext.class, PolicyChain.class);
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(true);
        Policy delegatePolicy = new ExecutablePolicy("dummy", fakeStreamPolicy(), method, method);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy(delegatePolicy, "condition", conditionEvaluator);
        final ReadWriteStream<Buffer> conditionedStream = policy.stream(policyChain, executionContext);

        // needed to avoid NPE in TransformableResponseStream
        final HttpHeaders headers = HttpHeaders.create();
        headers.add("Transfer-Encoding", "value");
        when(executionContext.response().headers()).thenReturn(headers);

        conditionedStream.write(Buffer.buffer("Test")).end();
        verify(policyChain, never()).doNext(any(), any());
        verify(executionContext, times(1)).setAttribute("stream", "On Response Content Dummy Streamable Policy");
    }

    @Test
    public void shouldNotStreamConditionalPolicyConditionEvaluatedToFalse() throws PolicyException, NoSuchMethodException {
        method = DummyStreamablePolicy.class.getMethod("onResponseContent", Response.class, ExecutionContext.class, PolicyChain.class);
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenReturn(false);
        Policy delegatePolicy = new ExecutablePolicy("dummy", fakeStreamPolicy(), method, method);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy(delegatePolicy, "condition", conditionEvaluator);
        final ReadWriteStream<Buffer> conditionedStream = policy.stream(policyChain, executionContext);
        conditionedStream.write(Buffer.buffer("Test")).end();
        verify(executionContext, never()).setAttribute("stream", "On Response Content Dummy Streamable Policy");
    }

    @Test
    public void shouldNotStreamConditionalPolicyExpressionEvaluationException() throws PolicyException, NoSuchMethodException {
        method = DummyStreamablePolicy.class.getMethod("onResponseContent", Response.class, ExecutionContext.class, PolicyChain.class);
        when(executionContext.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.getValue("condition", Boolean.class)).thenThrow(ExpressionEvaluationException.class);
        Policy delegatePolicy = new ExecutablePolicy("dummy", fakeStreamPolicy(), method, method);

        final ConditionalExecutablePolicy policy = new ConditionalExecutablePolicy(delegatePolicy, "condition", conditionEvaluator);
        final ReadWriteStream<Buffer> conditionedStream = policy.stream(policyChain, executionContext);
        conditionedStream.write(Buffer.buffer("Test")).end();
        verify(policyChain, times(1)).streamFailWith(any());
    }

    private Object fakeExecutePolicy() {
        return new PolicyPluginFactoryImpl().create(DummyPolicy.class, mock(PolicyConfiguration.class));
    }

    private Object fakeStreamPolicy() {
        return new PolicyPluginFactoryImpl().create(DummyStreamablePolicy.class, mock(PolicyConfiguration.class));
    }
}
