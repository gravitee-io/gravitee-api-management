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
package io.gravitee.gateway.debug.policy.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.reflections.ReflectionUtils.withModifier;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.debug.policy.DummyPolicy;
import io.gravitee.gateway.debug.policy.NoTransformationPolicy;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContext;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugRequestStep;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugResponseStep;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import io.gravitee.gateway.policy.PolicyFactory;
import io.gravitee.gateway.policy.PolicyManifest;
import io.gravitee.gateway.policy.PolicyMetadata;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.StreamType;
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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.reflections.ReflectionUtils;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyDebugDecoratorTest {

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
    private DebugExecutionContext context;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        policyFactory =
            spy(
                new PolicyDebugDecoratorFactory(
                    new PolicyFactoryImpl(policyPluginFactory, new ExpressionLanguageStringConditionEvaluator())
                )
            );

        when(context.request()).thenReturn(request);
        when(context.response()).thenReturn(response);
    }

    @Test
    public void onRequest() throws Exception {
        PolicyManifest policyManifest = mock(PolicyManifest.class);
        when(policyManifest.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        Method onRequestMethod = resolvePolicyMethod(DummyPolicy.class, OnRequest.class);
        when(policyManifest.method(OnRequest.class)).thenReturn(onRequestMethod);

        DummyPolicy dummyPolicy = spy(DummyPolicy.class);
        when(policyPluginFactory.create(DummyPolicy.class, null)).thenReturn(dummyPolicy);

        io.gravitee.gateway.policy.Policy debugPolicy = Mockito.spy(
            policyFactory.create(StreamType.ON_REQUEST, policyManifest, null, fakePolicyMetadata())
        );

        final ArgumentCaptor<DebugStep<?>> debugStepCaptor = ArgumentCaptor.forClass(DebugStep.class);
        final ArgumentCaptor<PolicyChain> chainCaptor = ArgumentCaptor.forClass(PolicyChain.class);

        debugPolicy.execute(policyChain, context);

        Assert.assertTrue(debugPolicy.isRunnable());
        Assert.assertFalse(debugPolicy.isStreamable());
        verify(dummyPolicy, atLeastOnce()).onRequest(chainCaptor.capture(), eq(request), eq(response));
        verify(dummyPolicy, never()).onRequestContent(policyChain, request, response);
        verify(dummyPolicy, never()).onResponse(chainCaptor.capture(), eq(request), eq(response));
        verify(dummyPolicy, never()).onResponseContent(policyChain, request, response);
        verify(debugPolicy, atLeastOnce()).execute(policyChain, context);

        verify(context, times(1)).beforePolicyExecution(debugStepCaptor.capture());
        verify(context, times(1)).afterPolicyExecution(debugStepCaptor.capture());
        assertThat(debugStepCaptor.getValue()).isInstanceOf(DebugRequestStep.class);
        assertThat(chainCaptor.getValue()).isInstanceOf(DebugPolicyChain.class);
    }

    @Test
    public void onResponse() throws Exception {
        PolicyManifest policyManifest = mock(PolicyManifest.class);
        when(policyManifest.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        Method onResponseMethod = resolvePolicyMethod(DummyPolicy.class, OnResponse.class);
        when(policyManifest.method(OnResponse.class)).thenReturn(onResponseMethod);

        DummyPolicy dummyPolicy = spy(DummyPolicy.class);
        when(policyPluginFactory.create(DummyPolicy.class, null)).thenReturn(dummyPolicy);

        io.gravitee.gateway.policy.Policy debugPolicy = Mockito.spy(
            policyFactory.create(StreamType.ON_RESPONSE, policyManifest, null, fakePolicyMetadata())
        );

        final ArgumentCaptor<DebugStep<?>> debugStepCaptor = ArgumentCaptor.forClass(DebugStep.class);
        final ArgumentCaptor<PolicyChain> chainCaptor = ArgumentCaptor.forClass(PolicyChain.class);

        debugPolicy.execute(policyChain, context);

        Assert.assertTrue(debugPolicy.isRunnable());
        Assert.assertFalse(debugPolicy.isStreamable());
        verify(dummyPolicy, never()).onRequest(chainCaptor.capture(), eq(request), eq(response));
        verify(dummyPolicy, never()).onRequestContent(policyChain, request, response);
        verify(dummyPolicy, atLeastOnce()).onResponse(chainCaptor.capture(), eq(request), eq(response));
        verify(dummyPolicy, never()).onResponseContent(policyChain, request, response);
        verify(debugPolicy, atLeastOnce()).execute(policyChain, context);

        verify(context, times(1)).beforePolicyExecution(debugStepCaptor.capture());
        verify(context, times(1)).afterPolicyExecution(debugStepCaptor.capture());
        assertThat(debugStepCaptor.getValue()).isInstanceOf(DebugResponseStep.class);
        assertThat(chainCaptor.getValue()).isInstanceOf(DebugPolicyChain.class);
    }

    @Test
    public void onRequestContent() throws Exception {
        PolicyManifest policyManifest = mock(PolicyManifest.class);
        when(policyManifest.policy()).then((Answer<Class>) invocationOnMock -> DummyPolicy.class);
        Method onRequestContentMethod = resolvePolicyMethod(DummyPolicy.class, OnRequestContent.class);
        when(policyManifest.method(OnRequestContent.class)).thenReturn(onRequestContentMethod);

        DummyPolicy dummyPolicy = mock(DummyPolicy.class);
        when(policyPluginFactory.create(DummyPolicy.class, null)).thenReturn(dummyPolicy);
        final ArgumentCaptor<PolicyChain> chainCaptor = ArgumentCaptor.forClass(PolicyChain.class);

        io.gravitee.gateway.policy.Policy policy = Mockito.spy(
            policyFactory.create(StreamType.ON_REQUEST, policyManifest, null, fakePolicyMetadata())
        );
        final PolicyDebugDecorator debugPolicy = spy(
            new PolicyDebugDecorator(StreamType.ON_REQUEST, policy, new PolicyMetadata("dummy", "{}"))
        );

        debugPolicy.stream(policyChain, context);

        Assert.assertFalse(debugPolicy.isRunnable());
        Assert.assertTrue(debugPolicy.isStreamable());
        verify(dummyPolicy, never()).onRequest(policyChain, request, response);
        verify(dummyPolicy, atLeastOnce()).onRequestContent(chainCaptor.capture(), eq(request), eq(response));
        verify(dummyPolicy, never()).onResponse(policyChain, request, response);
        verify(dummyPolicy, never()).onResponseContent(policyChain, request, response);
        verify(debugPolicy, atLeastOnce()).stream(policyChain, context);

        verify(context, never()).beforePolicyExecution(any());
        verify(context, never()).afterPolicyExecution(any());
        assertThat(chainCaptor.getValue()).isInstanceOf(DebugStreamablePolicyChain.class);
    }

    @Test
    public void onRequestContentNoTransformation() throws Exception {
        PolicyMetadata policyMetadata = mock(PolicyMetadata.class);
        when(policyMetadata.policy()).then((Answer<Class>) invocationOnMock -> NoTransformationPolicy.class);
        Method onRequestContentMethod = resolvePolicyMethod(NoTransformationPolicy.class, OnRequestContent.class);
        when(policyMetadata.method(OnRequestContent.class)).thenReturn(onRequestContentMethod);

        NoTransformationPolicy noTransformationPolicy = mock(NoTransformationPolicy.class);
        when(policyPluginFactory.create(NoTransformationPolicy.class, null)).thenReturn(noTransformationPolicy);
        final ArgumentCaptor<PolicyChain> chainCaptor = ArgumentCaptor.forClass(PolicyChain.class);

        io.gravitee.gateway.policy.Policy policy = Mockito.spy(policyFactory.create(StreamType.ON_REQUEST, policyMetadata, null));
        final PolicyDebugDecorator debugPolicy = spy(new PolicyDebugDecorator(StreamType.ON_REQUEST, policy));

        debugPolicy.stream(policyChain, context);

        Assert.assertFalse(debugPolicy.isRunnable());
        Assert.assertTrue(debugPolicy.isStreamable());
        verify(noTransformationPolicy, never()).onRequest(policyChain, request, response);
        verify(noTransformationPolicy, atLeastOnce()).onRequestContent(chainCaptor.capture(), eq(request), eq(response));
        verify(noTransformationPolicy, never()).onResponse(policyChain, request, response);
        verify(noTransformationPolicy, never()).onResponseContent(policyChain, request, response);
        verify(debugPolicy, atLeastOnce()).stream(policyChain, context);

        verify(context, times(1)).saveNoTransformationDebugStep(any());
        assertThat(chainCaptor.getValue()).isInstanceOf(DebugStreamablePolicyChain.class);
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
            policyFactory.create(StreamType.ON_RESPONSE, policyManifest, null, fakePolicyMetadata())
        );
        final PolicyDebugDecorator debugPolicy = spy(
            new PolicyDebugDecorator(StreamType.ON_RESPONSE, policy, new PolicyMetadata("dummy", "{}"))
        );
        final ArgumentCaptor<PolicyChain> chainCaptor = ArgumentCaptor.forClass(PolicyChain.class);

        debugPolicy.stream(policyChain, context);

        Assert.assertFalse(debugPolicy.isRunnable());
        Assert.assertTrue(debugPolicy.isStreamable());
        verify(dummyPolicy, never()).onRequest(policyChain, request, response);
        verify(dummyPolicy, never()).onRequestContent(policyChain, request, response);
        verify(dummyPolicy, never()).onResponse(policyChain, request, response);
        verify(dummyPolicy, atLeastOnce()).onResponseContent(chainCaptor.capture(), eq(request), eq(response));
        verify(debugPolicy, atLeastOnce()).stream(policyChain, context);

        verify(context, never()).beforePolicyExecution(any());
        verify(context, never()).afterPolicyExecution(any());
        assertThat(chainCaptor.getValue()).isInstanceOf(DebugStreamablePolicyChain.class);
    }

    @Test
    public void onResponseContentNoTransformation() throws Exception {
        PolicyMetadata policyMetadata = mock(PolicyMetadata.class);
        when(policyMetadata.policy()).then((Answer<Class>) invocationOnMock -> NoTransformationPolicy.class);
        Method onResponseContentMethod = resolvePolicyMethod(NoTransformationPolicy.class, OnResponseContent.class);
        when(policyMetadata.method(OnResponseContent.class)).thenReturn(onResponseContentMethod);

        NoTransformationPolicy noTransformationPolicy = mock(NoTransformationPolicy.class);
        when(policyPluginFactory.create(NoTransformationPolicy.class, null)).thenReturn(noTransformationPolicy);

        io.gravitee.gateway.policy.Policy policy = Mockito.spy(policyFactory.create(StreamType.ON_RESPONSE, policyMetadata, null));
        final PolicyDebugDecorator debugPolicy = spy(new PolicyDebugDecorator(StreamType.ON_RESPONSE, policy));
        final ArgumentCaptor<PolicyChain> chainCaptor = ArgumentCaptor.forClass(PolicyChain.class);

        debugPolicy.stream(policyChain, context);

        Assert.assertFalse(debugPolicy.isRunnable());
        Assert.assertTrue(debugPolicy.isStreamable());
        verify(noTransformationPolicy, never()).onRequest(policyChain, request, response);
        verify(noTransformationPolicy, never()).onRequestContent(policyChain, request, response);
        verify(noTransformationPolicy, never()).onResponse(policyChain, request, response);
        verify(noTransformationPolicy, atLeastOnce()).onResponseContent(chainCaptor.capture(), eq(request), eq(response));
        verify(debugPolicy, atLeastOnce()).stream(policyChain, context);

        verify(context, times(1)).saveNoTransformationDebugStep(any());
        assertThat(chainCaptor.getValue()).isInstanceOf(DebugStreamablePolicyChain.class);
    }

    private Method resolvePolicyMethod(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        Set<Method> methods = ReflectionUtils.getMethods(clazz, withModifier(Modifier.PUBLIC), withAnnotation(annotationClass));

        if (methods.isEmpty()) {
            return null;
        }

        return methods.iterator().next();
    }

    private PolicyMetadata fakePolicyMetadata() {
        return new PolicyMetadata("dummy-policy", "{}");
    }
}
