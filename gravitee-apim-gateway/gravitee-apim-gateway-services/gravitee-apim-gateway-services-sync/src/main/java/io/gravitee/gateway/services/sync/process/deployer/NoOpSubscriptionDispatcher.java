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
package io.gravitee.gateway.services.sync.process.deployer;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.reactivex.rxjava3.core.Completable;
import lombok.extern.slf4j.Slf4j;

/**
 * No-op subscription dispatcher used when no implementation has been found in spring context.
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class NoOpSubscriptionDispatcher implements SubscriptionDispatcher {

    @Override
    public Completable dispatch(Subscription subscription) {
        log.warn("Cannot dispatch subscription [{}]. Please install apim-reactor-message plugin.", subscription.getId());
        return Completable.complete();
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return Lifecycle.State.CLOSED;
    }

    @Override
    public SubscriptionDispatcher start() throws Exception {
        return this;
    }

    @Override
    public SubscriptionDispatcher stop() throws Exception {
        return this;
    }
}
