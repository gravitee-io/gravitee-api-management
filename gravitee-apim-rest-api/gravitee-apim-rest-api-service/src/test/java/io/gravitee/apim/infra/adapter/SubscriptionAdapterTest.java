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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.SubscriptionFixtures;
import io.gravitee.apim.core.subscription.model.SubscriptionConfiguration;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonSerializer;
import io.gravitee.repository.management.model.Subscription;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubscriptionAdapterTest {

    SubscriptionAdapter subscriptionAdapter;

    @BeforeAll
    void beforeAll() {
        subscriptionAdapter = new SubscriptionAdapterImpl();
        subscriptionAdapter.setJsonSerializer(new JacksonJsonSerializer());
        subscriptionAdapter.setJsonDeserializer(new JacksonJsonDeserializer());
    }

    @Nested
    class ToEntity {

        @Test
        void should_convert_subscription_to_subscription_entity() {
            Subscription subscription = aRepositorySubscription().build();

            SubscriptionEntity subscriptionEntity = subscriptionAdapter.toEntity(subscription);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(subscriptionEntity.getId()).isEqualTo("subscription-id");
                soft.assertThat(subscriptionEntity.getApiId()).isEqualTo("api-id");
                soft.assertThat(subscriptionEntity.getPlanId()).isEqualTo("plan-id");
                soft.assertThat(subscriptionEntity.getApplicationId()).isEqualTo("application-id");
                soft.assertThat(subscriptionEntity.getClientId()).isEqualTo("client-id");
                soft.assertThat(subscriptionEntity.getRequestMessage()).isEqualTo("request-message");
                soft.assertThat(subscriptionEntity.getReasonMessage()).isEqualTo("reason-message");
                soft.assertThat(subscriptionEntity.getStatus()).isEqualTo(SubscriptionEntity.Status.CLOSED);
                soft.assertThat(subscriptionEntity.getConsumerStatus()).isEqualTo(SubscriptionEntity.ConsumerStatus.STOPPED);
                soft.assertThat(subscriptionEntity.getProcessedBy()).isEqualTo("a-user");
                soft.assertThat(subscriptionEntity.getSubscribedBy()).isEqualTo("another-user");
                soft
                    .assertThat(subscriptionEntity.getCreatedAt())
                    .isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft
                    .assertThat(subscriptionEntity.getProcessedAt())
                    .isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft
                    .assertThat(subscriptionEntity.getStartingAt())
                    .isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft
                    .assertThat(subscriptionEntity.getEndingAt())
                    .isEqualTo(Instant.parse("2021-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft
                    .assertThat(subscriptionEntity.getPausedAt())
                    .isEqualTo(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft
                    .assertThat(subscriptionEntity.getConsumerPausedAt())
                    .isEqualTo(Instant.parse("2020-02-05T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft
                    .assertThat(subscriptionEntity.getClosedAt())
                    .isEqualTo(Instant.parse("2020-02-06T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft
                    .assertThat(subscriptionEntity.getUpdatedAt())
                    .isEqualTo(Instant.parse("2020-02-06T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(subscriptionEntity.getGeneralConditionsContentRevision()).isEqualTo(12);
                soft.assertThat(subscriptionEntity.getGeneralConditionsContentPageId()).isEqualTo("page-id");
                soft.assertThat(subscriptionEntity.getGeneralConditionsAccepted()).isTrue();
                soft.assertThat(subscriptionEntity.getDaysToExpirationOnLastNotification()).isEqualTo(310);
                soft
                    .assertThat(subscriptionEntity.getConfiguration())
                    .isEqualTo(
                        SubscriptionConfiguration.builder()
                            .entrypointId("entrypoint-id")
                            .channel("my-channel")
                            .entrypointConfiguration("{}")
                            .build()
                    );
                soft.assertThat(subscriptionEntity.getMetadata()).containsExactly(Map.entry("metadata1", "value1"));
                soft.assertThat(subscriptionEntity.getType()).isEqualTo(SubscriptionEntity.Type.STANDARD);
                soft.assertThat(subscriptionEntity.getFailureCause()).isEqualTo("failure-cause");
            });
        }

        @Test
        void should_convert_subscription_to_subscription_entity_with_null_configuration() {
            Subscription subscription = Subscription.builder().build();

            SubscriptionEntity subscriptionEntity = subscriptionAdapter.toEntity(subscription);
            assertThat(subscriptionEntity.getConfiguration()).isNull();
        }
    }

    @Nested
    class FromEntity {

        @Test
        void should_convert_subscription_entity_to_subscription() {
            SubscriptionEntity subscriptionEntity = SubscriptionFixtures.aSubscription()
                .toBuilder()
                .reasonMessage("reason-message")
                .status(SubscriptionEntity.Status.CLOSED)
                .consumerStatus(SubscriptionEntity.ConsumerStatus.STOPPED)
                .closedAt(Instant.parse("2020-02-06T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .pausedAt(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .consumerPausedAt(Instant.parse("2020-02-05T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .failureCause("failure-cause")
                .configuration(
                    SubscriptionConfiguration.builder()
                        .entrypointId("entrypoint-id")
                        .channel("my-channel")
                        .entrypointConfiguration(
                            """
                            {"callback":"http://webhook.site"}
                            """
                        )
                        .build()
                )
                .build();

            Subscription subscription = subscriptionAdapter.fromEntity(subscriptionEntity);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(subscription.getId()).isEqualTo("subscription-id");
                soft.assertThat(subscription.getApi()).isEqualTo("api-id");
                soft.assertThat(subscription.getPlan()).isEqualTo("plan-id");
                soft.assertThat(subscription.getApplication()).isEqualTo("application-id");
                soft.assertThat(subscription.getClientId()).isEqualTo("client-id");
                soft.assertThat(subscription.getRequest()).isEqualTo("request-message");
                soft.assertThat(subscription.getReason()).isEqualTo("reason-message");
                soft.assertThat(subscription.getStatus()).isEqualTo(Subscription.Status.CLOSED);
                soft.assertThat(subscription.getConsumerStatus()).isEqualTo(Subscription.ConsumerStatus.STOPPED);
                soft.assertThat(subscription.getProcessedBy()).isEqualTo("a-user");
                soft.assertThat(subscription.getSubscribedBy()).isEqualTo("another-user");
                soft.assertThat(subscription.getCreatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")));
                soft.assertThat(subscription.getProcessedAt()).isEqualTo(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")));
                soft.assertThat(subscription.getStartingAt()).isEqualTo(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")));
                soft.assertThat(subscription.getEndingAt()).isEqualTo(Date.from(Instant.parse("2021-02-01T20:22:02.00Z")));
                soft.assertThat(subscription.getPausedAt()).isEqualTo(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")));
                soft.assertThat(subscription.getConsumerPausedAt()).isEqualTo(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")));
                soft.assertThat(subscription.getClosedAt()).isEqualTo(Date.from(Instant.parse("2020-02-06T20:22:02.00Z")));
                soft.assertThat(subscription.getUpdatedAt()).isEqualTo(Date.from(Instant.parse("2020-02-06T20:22:02.00Z")));
                soft.assertThat(subscription.getGeneralConditionsContentRevision()).isEqualTo(12);
                soft.assertThat(subscription.getGeneralConditionsContentPageId()).isEqualTo("page-id");
                soft.assertThat(subscription.getGeneralConditionsAccepted()).isTrue();
                soft.assertThat(subscription.getDaysToExpirationOnLastNotification()).isEqualTo(310);

                soft
                    .assertThat(subscription.getConfiguration())
                    .isEqualTo(
                        """
                        {"entrypointId":"entrypoint-id","channel":"my-channel","entrypointConfiguration":{"callback":"http://webhook.site"}
                        }"""
                    );
                soft.assertThat(subscription.getMetadata()).containsExactly(Map.entry("metadata1", "value1"));
                soft.assertThat(subscription.getType()).isEqualTo(Subscription.Type.STANDARD);
                soft.assertThat(subscription.getFailureCause()).isEqualTo("failure-cause");
            });
        }

        @Test
        void should_convert_subscription_entity_to_subscription_with_null_configuration() {
            var entity = SubscriptionFixtures.aSubscription().toBuilder().configuration(null).build();

            var subscription = subscriptionAdapter.fromEntity(entity);
            assertThat(subscription.getConfiguration()).isNull();
        }
    }

    private Subscription.SubscriptionBuilder aRepositorySubscription() {
        return Subscription.builder()
            .id("subscription-id")
            .api("api-id")
            .plan("plan-id")
            .application("application-id")
            .clientId("client-id")
            .request("request-message")
            .reason("reason-message")
            .status(Subscription.Status.CLOSED)
            .consumerStatus(Subscription.ConsumerStatus.STOPPED)
            .type(Subscription.Type.STANDARD)
            .processedBy("a-user")
            .subscribedBy("another-user")
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-06T20:22:02.00Z")))
            .processedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
            .pausedAt(Date.from(Instant.parse("2020-02-04T20:22:02.00Z")))
            .consumerPausedAt(Date.from(Instant.parse("2020-02-05T20:22:02.00Z")))
            .startingAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .endingAt(Date.from(Instant.parse("2021-02-01T20:22:02.00Z")))
            .closedAt(Date.from(Instant.parse("2020-02-06T20:22:02.00Z")))
            .generalConditionsAccepted(true)
            .generalConditionsContentRevision(12)
            .generalConditionsContentPageId("page-id")
            .daysToExpirationOnLastNotification(310)
            .configuration(
                """
                { "entrypointId": "entrypoint-id", "channel": "my-channel", "entrypointConfiguration": {} }
                """
            )
            .metadata(Map.of("metadata1", "value1"))
            .failureCause("failure-cause");
    }
}
