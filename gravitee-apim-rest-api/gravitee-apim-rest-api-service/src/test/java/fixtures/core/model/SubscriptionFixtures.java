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
package fixtures.core.model;

import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Supplier;

public class SubscriptionFixtures {

    private SubscriptionFixtures() {}

    private static final Supplier<SubscriptionEntity.SubscriptionEntityBuilder> BASE = () ->
        SubscriptionEntity.builder()
            .id("subscription-id")
            .apiId("api-id")
            .referenceId("api-id")
            .referenceType(SubscriptionReferenceType.API)
            .planId("plan-id")
            .applicationId("application-id")
            .clientId("client-id")
            .requestMessage("request-message")
            .status(SubscriptionEntity.Status.ACCEPTED)
            .consumerStatus(SubscriptionEntity.ConsumerStatus.STARTED)
            .type(SubscriptionEntity.Type.STANDARD)
            .processedBy("a-user")
            .subscribedBy("another-user")
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-06T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .processedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .startingAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .endingAt(Instant.parse("2021-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .generalConditionsAccepted(true)
            .generalConditionsContentRevision(12)
            .generalConditionsContentPageId("page-id")
            .daysToExpirationOnLastNotification(310)
            .configuration(null)
            .metadata(Map.of("metadata1", "value1"));

    public static SubscriptionEntity aSubscription() {
        return BASE.get().build();
    }
}
