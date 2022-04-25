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
package io.gravitee.gateway.policy;

import static org.mockito.Mockito.*;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.policy.dummy.DummyPolicy;
import io.gravitee.gateway.policy.impl.PolicyFactoryImpl;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import io.gravitee.policy.api.annotations.OnResponseContent;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.reflections.ReflectionUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyTest {

    @Mock
    private PolicyPluginFactory policyPluginFactory;

    private PolicyFactory policyFactory;

    @Mock
    private PolicyChain policyChain;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private ExecutionContext context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        policyFactory = spy(new PolicyFactoryImpl(policyPluginFactory, new ExpressionLanguageStringConditionEvaluator()));

        when(context.request()).thenReturn(request);
        when(context.response()).thenReturn(response);
    }

    @Test
    public void onRequest() throws Exception {
        PolicyManifest policyManifest = mock(PolicyManifest.class);
        when(policyManifest.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        Method onRequestMethod = resolvePolicyMethod(DummyPolicy.class, OnRequest.class);
        when(policyManifest.method(OnRequest.class)).thenReturn(onRequestMethod);

        DummyPolicy dummyPolicy = mock(DummyPolicy.class);
        when(policyPluginFactory.create(DummyPolicy.class, null)).thenReturn(dummyPolicy);

        io.gravitee.gateway.policy.Policy policy = Mockito.spy(
            policyFactory.create(StreamType.ON_REQUEST, policyManifest, null, new PolicyMetadata("", ""))
        );

        policy.execute(policyChain, context);

        Assert.assertTrue(policy.isRunnable());
        Assert.assertFalse(policy.isStreamable());
        verify(dummyPolicy, atLeastOnce()).onRequest(policyChain, request, response);
        verify(dummyPolicy, never()).onRequestContent(policyChain, request, response);
        verify(dummyPolicy, never()).onResponse(request, response, policyChain);
        verify(dummyPolicy, never()).onResponseContent(request, response, policyChain);
        verify(policy, atLeastOnce()).execute(policyChain, context);
    }

    @Test
    public void onResponse() throws Exception {
        PolicyManifest policyManifest = mock(PolicyManifest.class);
        when(policyManifest.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        Method onResponseMethod = resolvePolicyMethod(DummyPolicy.class, OnResponse.class);
        when(policyManifest.method(OnResponse.class)).thenReturn(onResponseMethod);

        DummyPolicy dummyPolicy = mock(DummyPolicy.class);
        when(policyPluginFactory.create(DummyPolicy.class, null)).thenReturn(dummyPolicy);

        io.gravitee.gateway.policy.Policy policy = Mockito.spy(
            policyFactory.create(StreamType.ON_RESPONSE, policyManifest, null, new PolicyMetadata("", ""))
        );

        policy.execute(policyChain, context);

        Assert.assertTrue(policy.isRunnable());
        Assert.assertFalse(policy.isStreamable());
        verify(dummyPolicy, never()).onRequest(policyChain, request, response);
        verify(dummyPolicy, never()).onRequestContent(policyChain, request, response);
        verify(dummyPolicy, atLeastOnce()).onResponse(request, response, policyChain);
        verify(dummyPolicy, never()).onResponseContent(request, response, policyChain);
        verify(policy, atLeastOnce()).execute(policyChain, context);
    }

    @Test
    public void onRequestContent() throws Exception {
        PolicyManifest policyManifest = mock(PolicyManifest.class);
        when(policyManifest.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        Method onRequestContentMethod = resolvePolicyMethod(DummyPolicy.class, OnRequestContent.class);
        when(policyManifest.method(OnRequestContent.class)).thenReturn(onRequestContentMethod);

        DummyPolicy dummyPolicy = mock(DummyPolicy.class);
        when(policyPluginFactory.create(DummyPolicy.class, null)).thenReturn(dummyPolicy);

        io.gravitee.gateway.policy.Policy policy = Mockito.spy(
            policyFactory.create(StreamType.ON_REQUEST, policyManifest, null, new PolicyMetadata("", ""))
        );

        policy.stream(policyChain, context);

        Assert.assertFalse(policy.isRunnable());
        Assert.assertTrue(policy.isStreamable());
        verify(dummyPolicy, never()).onRequest(policyChain, request, response);
        verify(dummyPolicy, atLeastOnce()).onRequestContent(policyChain, request, response);
        verify(dummyPolicy, never()).onResponse(request, response, policyChain);
        verify(dummyPolicy, never()).onResponseContent(request, response, policyChain);
        verify(policy, atLeastOnce()).stream(policyChain, context);
    }

    @Test
    public void onResponseContent() throws Exception {
        PolicyManifest policyManifest = mock(PolicyManifest.class);
        when(policyManifest.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        Method onResponseContentMethod = resolvePolicyMethod(DummyPolicy.class, OnResponseContent.class);
        when(policyManifest.method(OnResponseContent.class)).thenReturn(onResponseContentMethod);

        DummyPolicy dummyPolicy = mock(DummyPolicy.class);
        when(policyPluginFactory.create(DummyPolicy.class, null)).thenReturn(dummyPolicy);

        io.gravitee.gateway.policy.Policy policy = Mockito.spy(
            policyFactory.create(StreamType.ON_RESPONSE, policyManifest, null, new PolicyMetadata("", ""))
        );

        policy.stream(policyChain, context);

        Assert.assertFalse(policy.isRunnable());
        Assert.assertTrue(policy.isStreamable());
        verify(dummyPolicy, never()).onRequest(policyChain, request, response);
        verify(dummyPolicy, never()).onRequestContent(policyChain, request, response);
        verify(dummyPolicy, never()).onResponse(request, response, policyChain);
        verify(dummyPolicy, atLeastOnce()).onResponseContent(request, response, policyChain);
        verify(policy, atLeastOnce()).stream(policyChain, context);
    }

    private Method resolvePolicyMethod(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Set<Method> methods = ReflectionUtils.getMethods(clazz, withModifier(Modifier.PUBLIC), withAnnotation(annotationClass));

        if (methods.isEmpty()) {
            return null;
        }

        return methods.iterator().next();
    }
}
