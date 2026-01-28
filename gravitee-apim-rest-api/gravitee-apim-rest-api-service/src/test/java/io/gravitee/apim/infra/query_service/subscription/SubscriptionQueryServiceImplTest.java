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
package io.gravitee.apim.infra.query_service.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.infra.adapter.SubscriptionAdapter;
import io.gravitee.apim.infra.adapter.SubscriptionAdapterImpl;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonSerializer;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SubscriptionQueryServiceImplTest {

    SubscriptionRepository subscriptionRepository;
    SubscriptionAdapter mapper;

    SubscriptionQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(SubscriptionRepository.class);

        mapper = new SubscriptionAdapterImpl();
        mapper.setJsonDeserializer(new JacksonJsonDeserializer());
        mapper.setJsonSerializer(new JacksonJsonSerializer());

        service = new SubscriptionQueryServiceImpl(subscriptionRepository, mapper);
    }

    @Nested
    class FindExpiredSubscriptions {

        @Test
        void should_query_for_expired_subscriptions() throws TechnicalException {
            // Given
            var now = new Date().getTime();
            when(subscriptionRepository.search(any())).thenAnswer(invocation -> List.of());

            // When
            service.findExpiredSubscriptions();

            // Then
            var captor = ArgumentCaptor.forClass(SubscriptionCriteria.class);
            verify(subscriptionRepository).search(captor.capture());
            assertThat(captor.getValue()).satisfies(criteria -> {
                assertThat(criteria.getStatuses()).containsExactly(Subscription.Status.ACCEPTED.name());
                assertThat(criteria.getEndingAtBefore()).isBetween(now, new Date().getTime());
                assertThat(criteria.getFrom()).isEqualTo(-1);
                assertThat(criteria.getTo()).isEqualTo(-1);
                assertThat(criteria.getEndingAtAfter()).isEqualTo(-1);
                assertThat(criteria.getClientId()).isNull();
                assertThat(criteria.getApis()).isNull();
                assertThat(criteria.getApplications()).isNull();
                assertThat(criteria.getExcludedApis()).isNull();
                assertThat(criteria.getIds()).isNull();
                assertThat(criteria.getPlans()).isNull();
                assertThat(criteria.getPlanSecurityTypes()).isNull();
                assertThat(criteria.isIncludeWithoutEnd()).isFalse();
            });
        }

        @Test
        void should_return_subscriptions_and_adapt_them() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenAnswer(invocation ->
                List.of(aSubscription("s1").status(Subscription.Status.ACCEPTED).build())
            );

            // When
            var result = service.findExpiredSubscriptions();

            // Then
            assertThat(result).containsExactly(
                SubscriptionEntity.builder()
                    .id("s1")
                    .apiId("api-id")
                    .planId("plan-id")
                    .applicationId("application-id")
                    .status(SubscriptionEntity.Status.ACCEPTED)
                    .consumerStatus(SubscriptionEntity.ConsumerStatus.STARTED)
                    .type(SubscriptionEntity.Type.STANDARD)
                    .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .endingAt(Instant.parse("2021-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .build()
            );
        }

        @Test
        void should_return_empty_stream_when_no_subscriptions_found() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenReturn(List.of());

            // When
            var result = service.findExpiredSubscriptions();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findExpiredSubscriptions());

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find expired subscription");
        }
    }

    @Nested
    class FindActiveSubscriptionsByPlan {

        @Test
        void should_query_for_active_subscriptions_for_a_given_plan() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenAnswer(invocation -> List.of());

            // When
            service.findActiveSubscriptionsByPlan("plan-id");

            // Then
            var captor = ArgumentCaptor.forClass(SubscriptionCriteria.class);
            verify(subscriptionRepository).search(captor.capture());
            assertThat(captor.getValue()).satisfies(criteria -> {
                assertThat(criteria.getStatuses()).containsExactly(
                    Subscription.Status.ACCEPTED.name(),
                    Subscription.Status.PENDING.name(),
                    Subscription.Status.PAUSED.name()
                );
                assertThat(criteria.getEndingAtBefore()).isEqualTo(-1);
                assertThat(criteria.getFrom()).isEqualTo(-1);
                assertThat(criteria.getTo()).isEqualTo(-1);
                assertThat(criteria.getEndingAtAfter()).isEqualTo(-1);
                assertThat(criteria.getClientId()).isNull();
                assertThat(criteria.getApis()).isNull();
                assertThat(criteria.getApplications()).isNull();
                assertThat(criteria.getExcludedApis()).isNull();
                assertThat(criteria.getIds()).isNull();
                assertThat(criteria.getPlans()).containsExactly("plan-id");
                assertThat(criteria.getPlanSecurityTypes()).isNull();
                assertThat(criteria.isIncludeWithoutEnd()).isFalse();
            });
        }

        @Test
        void should_return_subscriptions_and_adapt_them() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenAnswer(invocation ->
                List.of(aSubscription("s1").status(Subscription.Status.ACCEPTED).build())
            );

            // When
            var result = service.findActiveSubscriptionsByPlan("plan-id");

            // Then
            assertThat(result).containsExactly(
                SubscriptionEntity.builder()
                    .id("s1")
                    .apiId("api-id")
                    .planId("plan-id")
                    .applicationId("application-id")
                    .status(SubscriptionEntity.Status.ACCEPTED)
                    .consumerStatus(SubscriptionEntity.ConsumerStatus.STARTED)
                    .type(SubscriptionEntity.Type.STANDARD)
                    .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .endingAt(Instant.parse("2021-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .build()
            );
        }

        @Test
        void should_return_empty_stream_when_no_subscriptions_found() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenReturn(List.of());

            // When
            var result = service.findActiveSubscriptionsByPlan("plan-id");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findActiveSubscriptionsByPlan("plan-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find active plan's subscription");
        }
    }

    @Nested
    class FindActiveByApplicationIdAndApiId {

        @Test
        void should_return_active_subscription_for_given_api_and_application_() throws TechnicalException {
            // Given
            var id = "s1";
            var apiId = "api-id";
            var appId = "app-id";
            when(subscriptionRepository.search(any())).thenAnswer(invocation ->
                List.of(aSubscription(id).api(apiId).application(appId).build())
            );

            // When
            var result = service.findActiveByApplicationIdAndApiId(appId, apiId);

            // Then
            assertThat(result)
                .hasSize(1)
                .element(0)
                .extracting(SubscriptionEntity::getId, SubscriptionEntity::getApiId, SubscriptionEntity::getApplicationId)
                .containsExactly(id, apiId, appId);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findActiveByApplicationIdAndApiId("app-id", "api-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find subscriptions");
        }
    }

    @Nested
    class FindSubscriptionsByPlan {

        @Test
        void should_return_subscription_for_specific_plan() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenAnswer(invocation -> List.of(aSubscription("subscription-id").build()));

            // When
            var result = service.findActiveSubscriptionsByPlan("plan-id");

            // Then
            assertThat(result).containsExactly(
                SubscriptionEntity.builder()
                    .id("subscription-id")
                    .apiId("api-id")
                    .planId("plan-id")
                    .applicationId("application-id")
                    .status(SubscriptionEntity.Status.ACCEPTED)
                    .consumerStatus(SubscriptionEntity.ConsumerStatus.STARTED)
                    .type(SubscriptionEntity.Type.STANDARD)
                    .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .endingAt(Instant.parse("2021-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .build()
            );
        }

        @Test
        void should_return_empty_stream_when_no_subscriptions_found() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenReturn(List.of());

            // When
            var result = service.findSubscriptionsByPlan("plan-id");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(subscriptionRepository.search(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findSubscriptionsByPlan("plan-id"));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find plan's subscription");
        }
    }

    @Nested
    class FindByIdAndReferenceIdAndReferenceType {

        @Test
        void should_return_subscription_when_found() throws TechnicalException {
            var subscriptionId = "sub-1";
            var referenceId = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";
            var repoSubscription = aSubscription(subscriptionId)
                .referenceId(referenceId)
                .referenceType(SubscriptionReferenceType.API_PRODUCT)
                .build();
            when(
                subscriptionRepository.findByIdAndReferenceIdAndReferenceType(
                    subscriptionId,
                    referenceId,
                    io.gravitee.repository.management.model.SubscriptionReferenceType.API_PRODUCT
                )
            ).thenReturn(Optional.of(repoSubscription));

            var result = service.findByIdAndReferenceIdAndReferenceType(
                subscriptionId,
                referenceId,
                io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API_PRODUCT
            );

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(subscriptionId);
            assertThat(result.get().getReferenceId()).isEqualTo(referenceId);
            assertThat(result.get().getReferenceType()).isEqualTo(
                io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API_PRODUCT
            );
        }

        @Test
        void should_return_empty_when_not_found() throws TechnicalException {
            when(subscriptionRepository.findByIdAndReferenceIdAndReferenceType(any(), any(), any())).thenReturn(Optional.empty());

            var result = service.findByIdAndReferenceIdAndReferenceType(
                "sub-1",
                "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
                io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API_PRODUCT
            );

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(subscriptionRepository.findByIdAndReferenceIdAndReferenceType(any(), any(), any())).thenThrow(TechnicalException.class);

            var throwable = catchThrowable(() ->
                service.findByIdAndReferenceIdAndReferenceType(
                    "sub-1",
                    "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88",
                    io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API_PRODUCT
                )
            );

            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to find subscription by id and reference");
        }
    }

    private Subscription.SubscriptionBuilder aSubscription(String subscriptionId) {
        return Subscription.builder()
            .id(subscriptionId)
            .api("api-id")
            .plan("plan-id")
            .application("application-id")
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .endingAt(Date.from(Instant.parse("2021-02-02T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
            .consumerStatus(Subscription.ConsumerStatus.STARTED)
            .status(Subscription.Status.ACCEPTED)
            .type(Subscription.Type.STANDARD);
    }
}
