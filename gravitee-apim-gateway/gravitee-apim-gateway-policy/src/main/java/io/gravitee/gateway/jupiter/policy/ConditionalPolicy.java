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
package io.gravitee.gateway.jupiter.policy;

import io.gravitee.definition.model.ConditionSupplier;
import io.gravitee.definition.model.MessageConditionSupplier;
import io.gravitee.gateway.jupiter.api.context.GenericExecutionContext;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.gateway.jupiter.core.condition.ConditionFilter;
import io.gravitee.gateway.jupiter.core.condition.MessageConditionFilter;
import io.gravitee.gateway.jupiter.core.context.MutableExecutionContext;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.core.context.OnMessagesInterceptor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConditionalPolicy implements Policy, ConditionSupplier, MessageConditionSupplier {

    public static final Logger LOGGER = LoggerFactory.getLogger(ConditionalPolicy.class);
    protected static final String ATTR_SKIP_MESSAGE_POLICY = "skipMessagePolicy";

    protected final Policy policy;
    protected final String condition;
    protected final String messageCondition;
    private final ConditionFilter<ConditionalPolicy> conditionFilter;
    private final MessageConditionFilter<ConditionalPolicy> messageConditionFilter;

    private final boolean conditionDefined;
    private final boolean messageConditionDefined;

    public ConditionalPolicy(
        Policy policy,
        String condition,
        String messageCondition,
        ConditionFilter<ConditionalPolicy> conditionFilter,
        MessageConditionFilter<ConditionalPolicy> messageConditionFilter
    ) {
        this.policy = policy;
        this.condition = condition;
        this.messageCondition = messageCondition;
        this.conditionFilter = conditionFilter;
        this.messageConditionFilter = messageConditionFilter;

        this.conditionDefined = condition != null && !condition.isBlank();
        this.messageConditionDefined = messageCondition != null && !messageCondition.isBlank();
    }

    @Override
    public String id() {
        return policy.id();
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return onCondition(ctx, policy.onRequest(ctx));
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        return onCondition(ctx, policy.onResponse(ctx));
    }

    @Override
    public Completable onMessageRequest(final MessageExecutionContext ctx) {
        final MutableRequest request = ((MutableExecutionContext) ctx).request();

        return onCondition(ctx, onMessagesCondition(ctx, request, policy.onMessageRequest(ctx)));
    }

    @Override
    public Completable onMessageResponse(final MessageExecutionContext ctx) {
        final MutableResponse response = ((MutableExecutionContext) ctx).response();

        return onCondition(ctx, onMessagesCondition(ctx, response, policy.onMessageResponse(ctx)));
    }

    @Override
    public String getCondition() {
        return condition;
    }

    @Override
    public String getMessageCondition() {
        return messageCondition;
    }

    private Completable onCondition(GenericExecutionContext ctx, Completable toExecute) {
        if (!conditionDefined) {
            return toExecute;
        }

        return conditionFilter.filter(ctx, this).flatMapCompletable(conditionalPolicy -> toExecute);
    }

    private Completable onMessagesCondition(MessageExecutionContext ctx, OnMessagesInterceptor interceptor, Completable toExecute) {
        if (!messageConditionDefined) {
            return toExecute;
        }

        return toExecute
            .doFinally(interceptor::unsetMessagesInterceptor)
            .doOnSubscribe(disposable -> interceptor.setMessagesInterceptor(onMessages -> messagesInterceptor(ctx, onMessages)));
    }

    private Single<Message> onMessageCondition(MessageExecutionContext ctx, Message message) {
        return messageConditionFilter
            .filter(ctx, this, message)
            .isEmpty()
            .map(
                skipMessagePolicy -> {
                    message.attribute(ATTR_SKIP_MESSAGE_POLICY, skipMessagePolicy);
                    return message;
                }
            );
    }

    private FlowableTransformer<Message, Message> messagesInterceptor(
        MessageExecutionContext ctx,
        FlowableTransformer<Message, Message> onMessages
    ) {
        return upstream ->
            upstream
                .flatMapSingle(message -> onMessageCondition(ctx, message))
                .groupBy(message -> message.attribute(ATTR_SKIP_MESSAGE_POLICY))
                .flatMap(
                    messages -> {
                        final boolean skipPolicy = Boolean.TRUE.equals(messages.getKey());
                        if (skipPolicy) {
                            return messages;
                        }
                        return messages.compose(onMessages);
                    }
                );
    }
}
