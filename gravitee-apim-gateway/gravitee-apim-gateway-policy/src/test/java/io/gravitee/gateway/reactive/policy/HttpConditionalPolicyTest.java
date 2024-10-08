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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpResponse;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.gateway.reactive.core.condition.ConditionFilter;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpRequestInternal;
import io.gravitee.gateway.reactive.core.context.HttpResponseInternal;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpConditionalPolicyTest {

    protected static final String CONDITION = "{#context.attributes != null}";
    protected static final String POLICY_ID = "policyId";

    @Mock
    private Policy policy;

    @Mock
    private ConditionFilter<BaseExecutionContext, HttpConditionalPolicy> conditionFilter;

    @Mock(extraInterfaces = { HttpExecutionContextInternal.class })
    private HttpExecutionContext ctx;

    @Mock(extraInterfaces = HttpRequestInternal.class)
    private HttpRequest request;

    @Mock(extraInterfaces = HttpResponseInternal.class)
    private HttpResponse response;

    @Spy
    private Completable spyCompletable = Completable.complete();

    @BeforeEach
    void init() {
        lenient().when(policy.onRequest(ctx)).thenReturn(spyCompletable);
        lenient().when(policy.onMessageRequest(ctx)).thenReturn(spyCompletable);
        lenient().when(policy.onResponse(ctx)).thenReturn(spyCompletable);
        lenient().when(policy.onMessageResponse(ctx)).thenReturn(spyCompletable);

        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldNotExecuteConditionOnRequestWhenNoCondition(String condition) {
        final HttpConditionalPolicy cut = new HttpConditionalPolicy(policy, condition, conditionFilter);

        cut.onRequest(ctx).test().assertComplete();

        verify(policy).onRequest(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
        verifyNoInteractions(conditionFilter);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldNotExecuteConditionOnResponseWhenNoCondition(String condition) {
        final HttpConditionalPolicy cut = new HttpConditionalPolicy(policy, condition, conditionFilter);

        cut.onResponse(ctx).test().assertComplete();

        verify(policy).onResponse(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
        verifyNoInteractions(conditionFilter);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldExecuteConditionOnMessageRequestWhenNoCondition(String condition) {
        final HttpConditionalPolicy cut = new HttpConditionalPolicy(policy, condition, conditionFilter);

        cut.onMessageRequest(ctx).test().assertComplete();

        verify(policy, never()).onMessageRequest(ctx);
        verifyNoMoreInteractions(policy);
        verifyNoInteractions(conditionFilter);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldExecuteConditionOnMessageResponseWhenNoCondition(String condition) {
        final HttpConditionalPolicy cut = new HttpConditionalPolicy(policy, condition, conditionFilter);

        cut.onMessageResponse(ctx).test().assertComplete();

        verify(policy, never()).onMessageResponse(ctx);
        verifyNoMoreInteractions(policy);
        verifyNoInteractions(conditionFilter);
    }

    @Test
    void shouldExecuteConditionAndPolicyOnRequest() {
        final HttpConditionalPolicy cut = new HttpConditionalPolicy(policy, CONDITION, conditionFilter);

        when(conditionFilter.filter(ctx, cut)).thenReturn(Maybe.just(cut));

        cut.onRequest(ctx).test().assertComplete();

        verify(policy).onRequest(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
    }

    @Test
    void shouldNotExecutePolicyOnRequestWhenConditionIsNotMet() {
        final HttpConditionalPolicy cut = new HttpConditionalPolicy(policy, CONDITION, conditionFilter);

        when(conditionFilter.filter(ctx, cut)).thenReturn(Maybe.empty());

        cut.onRequest(ctx).test().assertComplete();

        verify(policy, never()).onRequest(ctx);
        verify(spyCompletable, never()).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
    }

    @Test
    void shouldExecuteConditionAndPolicyOnResponse() {
        final HttpConditionalPolicy cut = new HttpConditionalPolicy(policy, CONDITION, conditionFilter);

        when(conditionFilter.filter(ctx, cut)).thenReturn(Maybe.just(cut));

        cut.onResponse(ctx).test().assertComplete();

        verify(policy).onResponse(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
    }

    @Test
    void shouldNotExecutePolicyOnResponseWhenConditionIsNotMet() {
        final HttpConditionalPolicy cut = new HttpConditionalPolicy(policy, CONDITION, conditionFilter);

        when(conditionFilter.filter(ctx, cut)).thenReturn(Maybe.empty());

        cut.onResponse(ctx).test().assertComplete();

        verify(policy, never()).onResponse(ctx);
        verify(spyCompletable, never()).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
    }

    @Test
    void shouldGetOriginalPolicyId() {
        final HttpConditionalPolicy cut = new HttpConditionalPolicy(policy, null, conditionFilter);

        when(policy.id()).thenReturn(POLICY_ID);
        assertEquals(POLICY_ID, cut.id());
    }

    @Test
    void shouldGetCondition() {
        final HttpConditionalPolicy cut = new HttpConditionalPolicy(policy, CONDITION, conditionFilter);

        assertEquals(CONDITION, cut.getCondition());
    }
}
