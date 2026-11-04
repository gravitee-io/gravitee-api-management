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
package io.gravitee.gateway.handlers.guardian;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.handlers.guardian.reactor.GuardianReactor;
import io.gravitee.gateway.handlers.guardian.registry.GuardianRegistry;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.reactivex.rxjava3.core.Completable;

/**
 * Stands for the Guardian reactor plugin on a mocked {@link EventManager}: deploying or updating a Guardian starts a
 * reactor registered in the {@link GuardianRegistry}, as the plugin does.
 */
public final class FakeGuardianReactorPlugin {

    private FakeGuardianReactorPlugin() {}

    public static void install(EventManager eventManager, GuardianRegistry guardianRegistry) {
        for (ReactorEvent event : new ReactorEvent[] { ReactorEvent.DEPLOY, ReactorEvent.UPDATE }) {
            lenient()
                .doAnswer(invocation -> {
                    guardianRegistry.register(reactorOf(invocation.getArgument(1)));
                    return null;
                })
                .when(eventManager)
                .publishEvent(eq(event), any(ReactableGuardian.class));
        }
    }

    public static GuardianReactor reactorOf(ReactableGuardian reactableGuardian) {
        return new GuardianReactor() {
            @Override
            public ReactableGuardian reactableGuardian() {
                return reactableGuardian;
            }

            @Override
            public Completable execute(HttpPlainExecutionContext ctx, BasePolicy source) {
                return Completable.complete();
            }
        };
    }
}
