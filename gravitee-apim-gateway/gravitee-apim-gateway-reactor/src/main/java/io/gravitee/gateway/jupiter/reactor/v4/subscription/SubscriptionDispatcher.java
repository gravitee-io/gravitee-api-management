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
package io.gravitee.gateway.jupiter.reactor.v4.subscription;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.gateway.api.service.Subscription;
import io.reactivex.rxjava3.core.Completable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SubscriptionDispatcher extends LifecycleComponent<SubscriptionDispatcher> {
    /**
     * Dispatches the subscription {@link io.gravitee.gateway.reactor.handler.ReactorHandler}.
     *
     * @param subscription the subscription .
     *
     * @return a {@link Completable} that completes when the subscription has been fully dispatched.
     */
    Completable dispatch(Subscription subscription);
}
