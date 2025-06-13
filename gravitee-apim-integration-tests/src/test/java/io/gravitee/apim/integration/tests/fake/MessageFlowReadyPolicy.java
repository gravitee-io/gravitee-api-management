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
package io.gravitee.apim.integration.tests.fake;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.api.context.*;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class MessageFlowReadyPolicy implements Policy {

    private static final Map<Object, ReplaySubject<Void>> readyObsMap = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return "message-flow-ready";
    }

    public Completable onMessageRequest(MessageExecutionContext ctx) {
        return prepareReadyObs(ctx).andThen(ctx.request().onMessages(upstream -> upstream.doOnSubscribe(s -> completeReadyObs(ctx))));
    }

    public Completable onMessageResponse(MessageExecutionContext ctx) {
        return prepareReadyObs(ctx).andThen(ctx.response().onMessages(upstream -> upstream.doOnSubscribe(s -> completeReadyObs(ctx))));
    }

    /**
     * Get the observable indicating that the message flow is ready (subscribed in reactive words).
     *
     * @param id either a transaction identifier corresponding to the X-Gravitee-Transaction-Id header, either the subscription in case of a PUSH subscription.
     *
     * @return the observable indicating that the message flow is ready.
     */
    public static Completable readyObs(Object id) {
        return Completable
            .defer(() -> {
                while (true) {
                    final ReplaySubject<Void> obs = readyObsMap.get(id);
                    if (obs != null) {
                        // Even if we capture the message flow subscription, it could take time to effectively be connected to the backend. Apply a small delay to avoid side effects.
                        return obs
                            .ignoreElements()
                            .doOnComplete(() -> log.info("Message flow should be ready"))
                            .delaySubscription(100, TimeUnit.MILLISECONDS, Schedulers.newThread());
                    } else {
                        Thread.sleep(5);
                    }
                }
            })
            .subscribeOn(Schedulers.newThread());
    }

    private void completeReadyObs(MessageExecutionContext ctx) {
        readyObsMap.get(getReadyObsId(ctx)).onComplete();
    }

    @NonNull
    private Completable prepareReadyObs(GenericExecutionContext ctx) {
        return Completable.fromRunnable(() -> readyObsMap.putIfAbsent(getReadyObsId(ctx), ReplaySubject.create()));
    }

    private Object getReadyObsId(GenericExecutionContext ctx) {
        final Subscription subscription = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION);
        if (subscription != null && subscription.getType() == Subscription.Type.PUSH) {
            return subscription;
        }
        return ctx.request().transactionId();
    }
}
