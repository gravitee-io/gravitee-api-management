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
package io.gravitee.apim.integration.tests.plan;

import io.gravitee.gateway.api.service.Subscription;

import java.time.Instant;
import java.util.Date;

import static java.time.temporal.ChronoUnit.HOURS;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanHelper {

    public static final String PLAN_ID = "plan-id";
    public static final String APPLICATION_ID = "application-id";
    public static final String SUBSCRIPTION_ID = "subscription-id";

    public static String getApiPath(final String apiId) {
        return "/" + apiId;
    }

    /**
     * Generate the Subscription object that would be returned by the SubscriptionService
     * @return the Subscription object
     */
    public static Subscription createSubscription(final String apiId, final boolean isExpired) {
        final Subscription subscription = new Subscription();
        subscription.setApplication(APPLICATION_ID);
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setPlan(PLAN_ID);
        subscription.setApi(apiId);
        if (isExpired) {
            subscription.setEndingAt(new Date(Instant.now().minus(1, HOURS).toEpochMilli()));
        }
        return subscription;
    }
}
