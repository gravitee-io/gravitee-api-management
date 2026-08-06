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
package io.gravitee.gateway.core.subscription;

import io.gravitee.gateway.api.service.Subscription;

/**
 * The entity a subscription is attached to.
 *
 * <p>A regular subscription belongs to an API; an API Product subscription belongs to the product,
 * not to any single member API. Runtime caches index a subscription — and its API keys — under that
 * scope, so a product subscription has exactly one entry however many APIs the product exposes.
 * Lookups arrive with an API and resolve the products it belongs to.</p>
 */
public final class SubscriptionScope {

    private SubscriptionScope() {}

    public static String of(final Subscription subscription) {
        return subscription.getApiProductId() != null ? subscription.getApiProductId() : subscription.getApi();
    }
}
