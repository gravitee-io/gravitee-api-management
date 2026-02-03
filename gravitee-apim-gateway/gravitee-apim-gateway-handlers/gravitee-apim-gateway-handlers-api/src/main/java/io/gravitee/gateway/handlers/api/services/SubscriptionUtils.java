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
package io.gravitee.gateway.handlers.api.services;

import io.gravitee.gateway.api.service.Subscription;
import java.util.Map;

/**
 * Utility class for subscription-related operations.
 *
 * @author GraviteeSource Team
 */
public final class SubscriptionUtils {

    private static final String METADATA_REFERENCE_TYPE = "referenceType";
    private static final String REFERENCE_TYPE_API_PRODUCT = "API_PRODUCT";

    private SubscriptionUtils() {
        // Utility class - no instantiation
    }

    /**
     * Checks if a subscription is an API Product subscription.
     *
     * @param subscription the subscription to check
     * @return true if the subscription is an API Product subscription, false otherwise
     */
    public static boolean isApiProductSubscription(Subscription subscription) {
        if (subscription == null) {
            return false;
        }
        Map<String, String> metadata = subscription.getMetadata();
        return metadata != null && REFERENCE_TYPE_API_PRODUCT.equals(metadata.get(METADATA_REFERENCE_TYPE));
    }
}
