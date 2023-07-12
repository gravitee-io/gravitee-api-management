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
package io.gravitee.gateway.jupiter.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.gateway.jupiter.core.condition.ConditionFilter;
import io.gravitee.gateway.jupiter.core.condition.MessageConditionFilter;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ConditionalPolicyTest {

    protected static final String CONDITION = "{#context.attributes != null}";
    protected static final String MESSAGE_CONDITION = "{#message.content != null}";
    protected static final String MESSAGE_CONTENT = "test";
    protected static final String TRANSFORMED_MESSAGE_CONTENT = "Transformed test";
    protected static final Flowable<Message> MESSAGES = Flowable.just(new DefaultMessage(MESSAGE_CONTENT));
    protected static final FlowableTransformer<Message, Message> ON_MESSAGES = upstream ->
        upstream.map(message -> new DefaultMessage(TRANSFORMED_MESSAGE_CONTENT));
    protected static final String POLICY_ID = "policyId";

    @Mock
    private Policy policy;

    @Mock
    private ConditionFilter<ConditionalPolicy> conditionFilter;

    @Mock
    private MessageConditionFilter<ConditionalPolicy> messageConditionFilter;

    @Mock(extraInterfaces = MutableExecutionContext.class)
    private ExecutionContext ctx;

    @Mock(extraInterfaces = MutableRequest.class)
    private Request request;

    @Mock(extraInterfaces = MutableResponse.class)
    private Response response;

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
        final ConditionalPolicy cut = new ConditionalPolicy(policy, condition, null, conditionFilter, messageConditionFilter);

        cut.onRequest(ctx).test().assertComplete();

        verify(policy).onRequest(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
        verifyNoInteractions(conditionFilter);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldNotExecuteConditionOnResponseWhenNoCondition(String condition) {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, condition, null, conditionFilter, messageConditionFilter);

        cut.onResponse(ctx).test().assertComplete();

        verify(policy).onResponse(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
        verifyNoInteractions(conditionFilter);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldExecuteConditionOnMessageRequestWhenNoCondition(String condition) {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, condition, null, conditionFilter, messageConditionFilter);

        cut.onMessageRequest(ctx).test().assertComplete();

        verify(policy).onMessageRequest(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
        verifyNoInteractions(conditionFilter);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldExecuteConditionOnMessageResponseWhenNoCondition(String condition) {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, condition, null, conditionFilter, messageConditionFilter);

        cut.onMessageResponse(ctx).test().assertComplete();

        verify(policy).onMessageResponse(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
        verifyNoInteractions(conditionFilter);
    }

    @Test
    void shouldExecuteConditionAndPolicyOnRequest() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, CONDITION, null, conditionFilter, messageConditionFilter);

        when(conditionFilter.filter(ctx, cut)).thenReturn(Maybe.just(cut));

        cut.onRequest(ctx).test().assertComplete();

        verify(policy).onRequest(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
    }

    @Test
    void shouldNotExecutePolicyOnRequestWhenConditionIsNotMet() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, CONDITION, null, conditionFilter, messageConditionFilter);

        when(conditionFilter.filter(ctx, cut)).thenReturn(Maybe.empty());

        cut.onRequest(ctx).test().assertComplete();

        verify(policy).onRequest(ctx);
        verify(spyCompletable, never()).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
    }

    @Test
    void shouldExecuteConditionAndPolicyOnResponse() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, CONDITION, null, conditionFilter, messageConditionFilter);

        when(conditionFilter.filter(ctx, cut)).thenReturn(Maybe.just(cut));

        cut.onResponse(ctx).test().assertComplete();

        verify(policy).onResponse(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
    }

    @Test
    void shouldNotExecutePolicyOnResponseWhenConditionIsNotMet() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, CONDITION, null, conditionFilter, messageConditionFilter);

        when(conditionFilter.filter(ctx, cut)).thenReturn(Maybe.empty());

        cut.onResponse(ctx).test().assertComplete();

        verify(policy).onResponse(ctx);
        verify(spyCompletable, never()).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
    }

    @Test
    void shouldExecuteMessageConditionOnMessageRequest() {
        final Flowable<Message> messages = Flowable.just(new DefaultMessage(MESSAGE_CONTENT));
        final FlowableTransformer<Message, Message> onMessages = upstream ->
            upstream.map(message -> new DefaultMessage(TRANSFORMED_MESSAGE_CONTENT));
        final ConditionalPolicy cut = new ConditionalPolicy(policy, null, MESSAGE_CONDITION, conditionFilter, messageConditionFilter);
        final ArgumentCaptor<Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>>> onMessagesInterceptor =
            ArgumentCaptor.forClass(Function.class);

        doNothing().when(((MutableRequest) request)).setMessagesInterceptor(onMessagesInterceptor.capture());
        when(messageConditionFilter.filter(eq(ctx), eq(cut), any(Message.class))).thenReturn(Maybe.just(cut));

        cut.onMessageRequest(ctx).test().assertComplete();

        verify(policy).onMessageRequest(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);

        final TestSubscriber<Message> obs = messages.compose(onMessagesInterceptor.getValue().apply(onMessages)).test();
        obs.assertComplete();

        // Message content has changed because the condition was evaluated to true.
        obs.assertValue(message -> message.content().toString().equals(TRANSFORMED_MESSAGE_CONTENT));
    }

    @Test
    void shouldNotExecutePolicyOnMessageRequestWhenConditionIsNotMet() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, null, MESSAGE_CONDITION, conditionFilter, messageConditionFilter);
        final ArgumentCaptor<Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>>> onMessagesInterceptor =
            ArgumentCaptor.forClass(Function.class);

        doNothing().when(((MutableRequest) request)).setMessagesInterceptor(onMessagesInterceptor.capture());
        when(messageConditionFilter.filter(eq(ctx), eq(cut), any(Message.class))).thenReturn(Maybe.empty());

        cut.onMessageRequest(ctx).test().assertComplete();

        verify(policy).onMessageRequest(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);

        final TestSubscriber<Message> obs = MESSAGES.compose(onMessagesInterceptor.getValue().apply(ON_MESSAGES)).test();
        obs.assertComplete();

        // Message content hasn't changed because the condition was evaluated to false.
        obs.assertValue(message -> message.content().toString().equals(MESSAGE_CONTENT));
    }

    @Test
    void shouldExecutePolicyOnMessageRequestWithoutInterceptorWhenNoMessageCondition() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, null, null, conditionFilter, messageConditionFilter);

        cut.onMessageRequest(ctx).test().assertComplete();

        verify(policy).onMessageRequest(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
    }

    @Test
    void shouldExecuteMessageConditionOnMessageResponse() {
        final Flowable<Message> messages = Flowable.just(new DefaultMessage(MESSAGE_CONTENT));
        final FlowableTransformer<Message, Message> onMessages = upstream ->
            upstream.map(message -> new DefaultMessage(TRANSFORMED_MESSAGE_CONTENT));
        final ConditionalPolicy cut = new ConditionalPolicy(policy, null, MESSAGE_CONDITION, conditionFilter, messageConditionFilter);
        final ArgumentCaptor<Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>>> onMessagesInterceptor =
            ArgumentCaptor.forClass(Function.class);

        doNothing().when(((MutableResponse) response)).setMessagesInterceptor(onMessagesInterceptor.capture());
        when(messageConditionFilter.filter(eq(ctx), eq(cut), any(Message.class))).thenReturn(Maybe.just(cut));

        cut.onMessageResponse(ctx).test().assertComplete();

        verify(policy).onMessageResponse(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);

        final TestSubscriber<Message> obs = messages.compose(onMessagesInterceptor.getValue().apply(onMessages)).test();
        obs.assertComplete();

        // Message content has changed because the condition was evaluated to true.
        obs.assertValue(message -> message.content().toString().equals(TRANSFORMED_MESSAGE_CONTENT));
    }

    @Test
    void shouldNotExecutePolicyOnMessageResponseWhenConditionIsNotMet() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, null, MESSAGE_CONDITION, conditionFilter, messageConditionFilter);
        final ArgumentCaptor<Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>>> onMessagesInterceptor =
            ArgumentCaptor.forClass(Function.class);

        doNothing().when(((MutableResponse) response)).setMessagesInterceptor(onMessagesInterceptor.capture());
        when(messageConditionFilter.filter(eq(ctx), eq(cut), any(Message.class))).thenReturn(Maybe.empty());

        cut.onMessageResponse(ctx).test().assertComplete();

        verify(policy).onMessageResponse(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);

        final TestSubscriber<Message> obs = MESSAGES.compose(onMessagesInterceptor.getValue().apply(ON_MESSAGES)).test();
        obs.assertComplete();

        // Message content hasn't changed because the condition was evaluated to false.
        obs.assertValue(message -> message.content().toString().equals(MESSAGE_CONTENT));
    }

    @Test
    void shouldExecutePolicyOnMessageResponseWithoutInterceptorWhenNoMessageCondition() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, null, null, conditionFilter, messageConditionFilter);

        cut.onMessageResponse(ctx).test().assertComplete();

        verify(policy).onMessageResponse(ctx);
        verify(spyCompletable).subscribe(any(CompletableObserver.class));
        verifyNoMoreInteractions(policy);
    }

    @Test
    void shouldGetOriginalPolicyId() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, null, null, conditionFilter, messageConditionFilter);

        when(policy.id()).thenReturn(POLICY_ID);
        assertEquals(POLICY_ID, cut.id());
    }

    @Test
    void shouldGetCondition() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, CONDITION, MESSAGE_CONDITION, conditionFilter, messageConditionFilter);

        assertEquals(CONDITION, cut.getCondition());
    }

    @Test
    void shouldGetMessageCondition() {
        final ConditionalPolicy cut = new ConditionalPolicy(policy, CONDITION, MESSAGE_CONDITION, conditionFilter, messageConditionFilter);

        assertEquals(MESSAGE_CONDITION, cut.getMessageCondition());
    }
}
