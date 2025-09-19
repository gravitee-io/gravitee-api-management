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
package io.gravitee.apim.infra.crud_service.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.SubscriptionFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.subscription.model.SubscriptionConfiguration;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.apim.infra.adapter.SubscriptionAdapterImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonSerializer;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SubscriptionCrudServiceImplTest {

    SubscriptionRepository subscriptionRepository;
    SubscriptionAdapter mapper;
    SubscriptionCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);

        mapper = new SubscriptionAdapterImpl();
        mapper.setJsonDeserializer(new JacksonJsonDeserializer());
        mapper.setJsonSerializer(new JacksonJsonSerializer());

        service = new SubscriptionCrudServiceImpl(subscriptionRepository, mapper);
    }

    @Nested
    class Get {

        @Test
        void should_return_subscription_and_adapt_it() throws TechnicalException {
            // Given
            var subscriptionId = "subscription-id";
            when(subscriptionRepository.findById(subscriptionId)).thenAnswer(invocation ->
                Optional.of(aSubscription().id(invocation.getArgument(0)).build())
            );

            // When
            var result = service.get(subscriptionId);

            // Then
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(result.getId()).isEqualTo(subscriptionId);
                soft.assertThat(result.getApiId()).isEqualTo("api-id");
                soft.assertThat(result.getPlanId()).isEqualTo("plan-id");
                soft.assertThat(result.getApplicationId()).isEqualTo("application-id");
                soft.assertThat(result.getClientId()).isEqualTo("client-id");
                soft.assertThat(result.getRequestMessage()).isEqualTo("request-message");
                soft.assertThat(result.getReasonMessage()).isEqualTo("reason-message");
                soft.assertThat(result.getStatus()).isEqualTo(SubscriptionEntity.Status.CLOSED);
                soft.assertThat(result.getConsumerStatus()).isEqualTo(SubscriptionEntity.ConsumerStatus.STOPPED);
                soft.assertThat(result.getProcessedBy()).isEqualTo("a-user");
                soft.assertThat(result.getSubscribedBy()).isEqualTo("another-user");
                soft.assertThat(result.getCreatedAt()).isEqualTo(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getProcessedAt()).isEqualTo(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getStartingAt()).isEqualTo(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getEndingAt()).isEqualTo(Instant.parse("2021-02-01T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getPausedAt()).isEqualTo(Instant.parse("2020-02-04T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getConsumerPausedAt()).isEqualTo(Instant.parse("2020-02-05T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getClosedAt()).isEqualTo(Instant.parse("2020-02-06T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getUpdatedAt()).isEqualTo(Instant.parse("2020-02-06T20:22:02.00Z").atZone(ZoneOffset.UTC));
                soft.assertThat(result.getGeneralConditionsContentRevision()).isEqualTo(12);
                soft.assertThat(result.getGeneralConditionsContentPageId()).isEqualTo("page-id");
                soft.assertThat(result.getGeneralConditionsAccepted()).isTrue();
                soft.assertThat(result.getDaysToExpirationOnLastNotification()).isEqualTo(310);
                soft
                    .assertThat(result.getConfiguration())
                    .isEqualTo(
                        SubscriptionConfiguration.builder()
                            .entrypointId("entrypoint-id")
                            .channel("my-channel")
                            .entrypointConfiguration("\"{}\"")
                            .build()
                    );
                soft.assertThat(result.getMetadata()).containsExactly(Map.entry("metadata1", "value1"));
                soft.assertThat(result.getType()).isEqualTo(SubscriptionEntity.Type.STANDARD);
                soft.assertThat(result.getFailureCause()).isEqualTo("failure-cause");
            });
        }

        @Test
        void should_throw_when_no_subscription_found() throws TechnicalException {
            // Given
            String subscriptionId = "unknown";
            when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> service.get(subscriptionId));

            // Then
            assertThat(throwable)
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessage("Subscription [" + subscriptionId + "] cannot be found.");
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            String subscriptionId = "my-subscription";
            when(subscriptionRepository.findById(subscriptionId)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.get(subscriptionId));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find a subscription by id: " + subscriptionId);
        }
    }

    @Nested
    class Update {

        @Test
        @SneakyThrows
        void should_update_an_existing_subscription() {
            SubscriptionEntity subscription = SubscriptionFixtures.aSubscription();
            service.update(subscription);

            var captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).update(captor.capture());

            assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(mapper.fromEntity(subscription));
        }

        @Test
        @SneakyThrows
        void should_return_the_updated_subscription() {
            when(subscriptionRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toUpdate = SubscriptionFixtures.aSubscription();
            var result = service.update(toUpdate);

            assertThat(result).isEqualTo(toUpdate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(subscriptionRepository.update(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.update(SubscriptionFixtures.aSubscription()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to update the subscription: subscription-id");
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_membership() throws TechnicalException {
            service.delete("subscription-id");
            verify(subscriptionRepository).delete("subscription-id");
        }

        @Test
        void should_throw_if_deletion_problem_occurs() throws TechnicalException {
            doThrow(new TechnicalException("exception")).when(subscriptionRepository).delete("subscription-id");
            assertThatThrownBy(() -> service.delete("subscription-id"))
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to delete the subscription with id: subscription-id");
            verify(subscriptionRepository).delete("subscription-id");
        }
    }

    private Subscription.SubscriptionBuilder aSubscription() {
        return Subscription.builder()
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
                { "entrypointId": "entrypoint-id", "channel": "my-channel", "entrypointConfiguration": "{}" }
                """
            )
            .metadata(Map.of("metadata1", "value1"))
            .failureCause("failure-cause");
    }
}
