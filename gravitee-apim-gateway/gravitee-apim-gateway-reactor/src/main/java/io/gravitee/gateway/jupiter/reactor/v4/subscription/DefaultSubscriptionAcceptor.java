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

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactor.handler.ReactorHandler;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultSubscriptionAcceptor implements SubscriptionAcceptor {

    private final String apiId;
    private ReactorHandler reactor;

    public DefaultSubscriptionAcceptor(final ReactorHandler reactor, final String apiId) {
        this.reactor = reactor;
        this.apiId = apiId;
    }

    public DefaultSubscriptionAcceptor(final String apiId) {
        this(null, apiId);
    }

    @Override
    public boolean accept(Subscription subscription) {
        return apiId.equals(subscription.getApi());
    }

    @Override
    public String apiId() {
        return apiId;
    }

    @Override
    public ReactorHandler reactor() {
        return reactor;
    }

    public void reactor(ReactorHandler reactor) {
        this.reactor = reactor;
    }

    @Override
    public int compareTo(SubscriptionAcceptor o) {
        return 0;
    }
}
