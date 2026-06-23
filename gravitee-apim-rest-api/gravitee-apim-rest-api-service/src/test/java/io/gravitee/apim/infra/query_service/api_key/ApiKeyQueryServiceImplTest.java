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
package io.gravitee.apim.infra.query_service.api_key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.model.ExpiringApiKey;
import io.gravitee.apim.core.api_key.model.ExpiringApiKeySubscription;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ApiKeyQueryServiceImplTest {

    ApiKeyRepository apiKeyRepository;
    SubscriptionRepository subscriptionRepository;

    ApiKeyQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        apiKeyRepository = mock(ApiKeyRepository.class);
        subscriptionRepository = mock(SubscriptionRepository.class);

        service = new ApiKeyQueryServiceImpl(apiKeyRepository, subscriptionRepository);
    }

    @Nested
    class FindById {

        @Test
        void should_return_api_key_and_adapt_it() throws TechnicalException {
            // Given
            var id = "api-key-id";
            when(apiKeyRepository.findById(id)).thenAnswer(invocation -> Optional.of(anApiKey().id(invocation.getArgument(0)).build()));

            // When
            var result = service.findById(id);

            // Then
            assertThat(result).contains(
                ApiKeyEntity.builder()
                    .id(id)
                    .subscriptions(List.of("subscription-id"))
                    .key("c080f684-2c35-40a1-903c-627c219e0567")
                    .applicationId("application-id")
                    .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .expireAt(Instant.parse("2021-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .revokedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .revoked(true)
                    .paused(true)
                    .daysToExpirationOnLastNotification(310)
                    .build()
            );
        }

        @Test
        void should_return_empty_when_no_api_key_found() throws TechnicalException {
            // Given
            String id = "unknown";
            when(apiKeyRepository.findById(id)).thenReturn(Optional.empty());

            // When
            var result = service.findById(id);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            String id = "my-api-key";
            when(apiKeyRepository.findById(id)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findById(id));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find API key by id: " + id);
        }
    }

    @Nested
    class FindByApplicationId {

        @Test
        void should_return_api_keys_and_adapt_them() throws TechnicalException {
            // Given
            var applicationId = "application-id";
            when(apiKeyRepository.findByApplication(applicationId)).thenAnswer(invocation ->
                List.of(anApiKeyForApplication(invocation.getArgument(0)).build())
            );

            // When
            var result = service.findByApplication(applicationId);

            // Then
            assertThat(result).containsExactly(
                ApiKeyEntity.builder()
                    .id("api-key-id")
                    .applicationId(applicationId)
                    .subscriptions(List.of("subscription-id"))
                    .key("c080f684-2c35-40a1-903c-627c219e0567")
                    .applicationId("application-id")
                    .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .expireAt(Instant.parse("2021-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .revokedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .revoked(true)
                    .paused(true)
                    .daysToExpirationOnLastNotification(310)
                    .build()
            );
        }

        @Test
        void should_return_empty_stream_when_no_api_keys() throws TechnicalException {
            // Given
            String applicationId = "unknown";
            when(apiKeyRepository.findByApplication(applicationId)).thenReturn(List.of());

            // When
            var result = service.findByApplication(applicationId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            String applicationId = "my-subscription";
            when(apiKeyRepository.findByApplication(applicationId)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findByApplication(applicationId));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find API keys by application id: " + applicationId);
        }
    }

    @Nested
    class FindByKeyAndEnvironmentId {

        @Test
        void should_return_api_keys_and_adapt_them() throws TechnicalException {
            var key = "my-key";
            var environmentId = "env-id";
            when(apiKeyRepository.findByKeyAndEnvironmentId(key, environmentId)).thenReturn(List.of(anApiKey().key(key).build()));

            var result = service.findByKeyAndEnvironmentId(key, environmentId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getKey()).isEqualTo(key);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            var key = "my-key";
            var environmentId = "env-id";
            when(apiKeyRepository.findByKeyAndEnvironmentId(key, environmentId)).thenThrow(TechnicalException.class);

            Throwable throwable = catchThrowable(() -> service.findByKeyAndEnvironmentId(key, environmentId));

            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find API keys by [key=my-key] and [environmentId=env-id]");
        }
    }

    @Nested
    class FindAllByKeyAndReferenceIdAndReferenceType {

        @Test
        void should_return_all_api_keys_and_adapt_them() throws TechnicalException {
            var key = "my-key";
            var referenceId = "api-id";
            var referenceType = "API";
            when(apiKeyRepository.findAllByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType)).thenReturn(
                List.of(anApiKey().key(key).build(), anApiKey().id("second-key").key(key).build())
            );

            var result = service.findAllByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApiKeyEntity::getKey).containsOnly(key);
        }

        @Test
        void should_throw_when_reference_type_is_unsupported() {
            Throwable throwable = catchThrowable(() -> service.findAllByKeyAndReferenceIdAndReferenceType("key", "ref", "UNSUPPORTED"));

            assertThat(throwable).isInstanceOf(IllegalArgumentException.class).hasMessage("Unsupported reference type: UNSUPPORTED");
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            when(apiKeyRepository.findAllByKeyAndReferenceIdAndReferenceType("key", "ref", "API")).thenThrow(TechnicalException.class);

            Throwable throwable = catchThrowable(() -> service.findAllByKeyAndReferenceIdAndReferenceType("key", "ref", "API"));

            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find API keys by [key=key], [referenceId=ref], [referenceType=API]");
        }
    }

    @Nested
    class FindByKeyAndApiId {

        @Test
        void should_prefer_active_key_when_multiple_matches_exist() throws TechnicalException {
            var key = "my-key";
            var apiId = "my-api";
            var inactiveKey = anApiKey()
                .id("inactive-key")
                .key(key)
                .revoked(true)
                .paused(false)
                .expireAt(Date.from(Instant.parse("2051-02-01T20:22:02.00Z")))
                .updatedAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                .build();
            var activeKey = anApiKey()
                .id("active-key")
                .key(key)
                .revoked(false)
                .paused(false)
                .expireAt(Date.from(Instant.parse("2051-02-01T20:22:02.00Z")))
                .updatedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                .build();
            when(apiKeyRepository.findAllByKeyAndApi(key, apiId)).thenReturn(List.of(inactiveKey, activeKey));
            when(subscriptionRepository.findByIdIn(Set.of("subscription-id"))).thenReturn(
                List.of(Subscription.builder().id("subscription-id").api(apiId).status(Subscription.Status.ACCEPTED).build())
            );

            var result = service.findByKeyAndApiId(key, apiId);

            assertThat(result).map(ApiKeyEntity::getId).contains("active-key");
        }

        @Test
        void should_return_empty_when_none_are_active() throws TechnicalException {
            var key = "my-key";
            var apiId = "my-api";
            var olderInactiveKey = anApiKey()
                .id("older-inactive-key")
                .key(key)
                .revoked(true)
                .updatedAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                .build();
            var newerInactiveKey = anApiKey()
                .id("newer-inactive-key")
                .key(key)
                .revoked(true)
                .updatedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                .build();
            when(apiKeyRepository.findAllByKeyAndApi(key, apiId)).thenReturn(List.of(olderInactiveKey, newerInactiveKey));
            when(subscriptionRepository.findByIdIn(Set.of("subscription-id"))).thenReturn(
                List.of(Subscription.builder().id("subscription-id").api(apiId).status(Subscription.Status.CLOSED).build())
            );

            var result = service.findByKeyAndApiId(key, apiId);

            assertThat(result).isEmpty();
        }

        @Test
        void should_return_api_key_and_adapt_it() throws TechnicalException {
            // Given
            var key = "my-key";
            var apiId = "my-api";
            when(apiKeyRepository.findAllByKeyAndApi(key, apiId)).thenAnswer(invocation ->
                List.of(anActiveApiKey().key(invocation.getArgument(0)).build())
            );
            mockActiveSubscription("subscription-id", apiId);

            // When
            var result = service.findByKeyAndApiId(key, apiId);

            // Then
            assertThat(result).contains(
                ApiKeyEntity.builder()
                    .id("api-key-id")
                    .subscriptions(List.of("subscription-id"))
                    .key(key)
                    .applicationId("application-id")
                    .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .expireAt(Instant.parse("2051-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .revoked(false)
                    .paused(false)
                    .daysToExpirationOnLastNotification(310)
                    .build()
            );
        }

        @Test
        void should_return_empty_when_no_api_key_found() throws TechnicalException {
            // Given
            var key = "my-key";
            var apiId = "my-api";
            when(apiKeyRepository.findAllByKeyAndApi(key, apiId)).thenReturn(List.of());

            // When
            var result = service.findByKeyAndApiId(key, apiId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            var key = "my-key";
            var apiId = "my-api";
            when(apiKeyRepository.findAllByKeyAndApi(key, apiId)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findByKeyAndApiId(key, apiId));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find API key by [key=my-key] and [apiId=my-api]");
        }

        @Test
        void should_throw_when_subscription_resolution_fails() throws TechnicalException {
            var key = "my-key";
            var apiId = "my-api";
            when(apiKeyRepository.findAllByKeyAndApi(key, apiId)).thenReturn(List.of(anActiveApiKey().key(key).build()));
            when(subscriptionRepository.findByIdIn(Set.of("subscription-id"))).thenThrow(TechnicalException.class);

            Throwable throwable = catchThrowable(() -> service.findByKeyAndApiId(key, apiId));

            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while resolving subscriptions for API keys");
        }

        @Test
        void should_return_empty_when_api_key_has_no_subscriptions() throws TechnicalException {
            var key = "my-key";
            var apiId = "my-api";
            var keyWithoutSubscriptions = anActiveApiKey().key(key).subscriptions(null).build();
            when(apiKeyRepository.findAllByKeyAndApi(key, apiId)).thenReturn(List.of(keyWithoutSubscriptions));

            var result = service.findByKeyAndApiId(key, apiId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FindByKeyAndReferenceIdAndReferenceType {

        @Test
        void should_return_api_key_and_adapt_it() throws TechnicalException {
            var key = "my-key";
            var referenceId = "api-id";
            var referenceType = "API";
            when(apiKeyRepository.findAllByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType)).thenAnswer(invocation ->
                List.of(anActiveApiKey().key(invocation.getArgument(0)).build())
            );
            mockActiveSubscription("subscription-id", referenceId);

            var result = service.findByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType);

            assertThat(result).contains(
                ApiKeyEntity.builder()
                    .id("api-key-id")
                    .subscriptions(List.of("subscription-id"))
                    .key(key)
                    .applicationId("application-id")
                    .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .expireAt(Instant.parse("2051-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .revoked(false)
                    .paused(false)
                    .daysToExpirationOnLastNotification(310)
                    .build()
            );
        }

        @Test
        void should_return_empty_when_no_api_key_found() throws TechnicalException {
            var key = "my-key";
            var referenceId = "api-id";
            var referenceType = "API_PRODUCT";
            when(apiKeyRepository.findAllByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType)).thenReturn(List.of());

            var result = service.findByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType);

            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            var key = "my-key";
            var referenceId = "api-id";
            var referenceType = "API";
            when(apiKeyRepository.findAllByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType)).thenThrow(
                TechnicalException.class
            );

            Throwable throwable = catchThrowable(() -> service.findByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType));

            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find API key by [key=my-key], [referenceId=api-id], [referenceType=API]");
        }

        @Test
        void should_throw_when_reference_type_is_unsupported() {
            Throwable throwable = catchThrowable(() -> service.findByKeyAndReferenceIdAndReferenceType("key", "ref", "INVALID"));

            assertThat(throwable).isInstanceOf(IllegalArgumentException.class).hasMessage("Unsupported reference type: INVALID");
        }

        @Test
        void should_prefer_active_key_for_api_product_reference() throws TechnicalException {
            var key = "my-key";
            var referenceId = "product-id";
            var referenceType = SubscriptionReferenceType.API_PRODUCT.name();
            var inactiveKey = anApiKey()
                .id("inactive-key")
                .key(key)
                .revoked(true)
                .subscriptions(List.of("subscription-id"))
                .updatedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
                .build();
            var activeKey = anActiveApiKey()
                .id("active-key")
                .key(key)
                .updatedAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                .build();
            when(apiKeyRepository.findAllByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType)).thenReturn(
                List.of(inactiveKey, activeKey)
            );
            when(subscriptionRepository.findByIdIn(Set.of("subscription-id"))).thenReturn(
                List.of(
                    Subscription.builder()
                        .id("subscription-id")
                        .referenceId(referenceId)
                        .referenceType(io.gravitee.repository.management.model.SubscriptionReferenceType.API_PRODUCT)
                        .status(Subscription.Status.ACCEPTED)
                        .build()
                )
            );

            var result = service.findByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType);

            assertThat(result).map(ApiKeyEntity::getId).contains("active-key");
        }
    }

    @Nested
    class FindBySubscriptionId {

        @Test
        void should_return_api_keys_and_adapt_them() throws TechnicalException {
            // Given
            var subscriptionId = "subscription-id";
            when(apiKeyRepository.findBySubscription(subscriptionId)).thenAnswer(invocation ->
                Set.of(anApiKeyForSubscription(invocation.getArgument(0)).build())
            );

            // When
            var result = service.findBySubscription(subscriptionId);

            // Then
            assertThat(result).containsExactly(
                ApiKeyEntity.builder()
                    .id("api-key-id")
                    .subscriptions(List.of(subscriptionId))
                    .key("c080f684-2c35-40a1-903c-627c219e0567")
                    .applicationId("application-id")
                    .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .expireAt(Instant.parse("2021-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .revokedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                    .revoked(true)
                    .paused(true)
                    .daysToExpirationOnLastNotification(310)
                    .build()
            );
        }

        @Test
        void should_return_empty_stream_when_no_api_keys() throws TechnicalException {
            // Given
            String subscriptionId = "unknown";
            when(apiKeyRepository.findBySubscription(subscriptionId)).thenReturn(Set.of());

            // When
            var result = service.findBySubscription(subscriptionId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            String subscriptionId = "my-subscription";
            when(apiKeyRepository.findBySubscription(subscriptionId)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findBySubscription(subscriptionId));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while trying to find API keys by subscription id: " + subscriptionId);
        }
    }

    @Nested
    class FindExpiringApiKeys {

        @Test
        void should_return_empty_list_without_hitting_repo_when_days_buckets_is_empty() throws TechnicalException {
            var result = service.findExpiringApiKeys(Instant.ofEpochMilli(1_700_000_000_000L), List.of(), 60_000L);

            assertThat(result).isEmpty();
            Mockito.verifyNoInteractions(apiKeyRepository);
            Mockito.verifyNoInteractions(subscriptionRepository);
        }

        @Test
        void should_emit_one_unordered_query_with_union_window_and_correct_flags() throws TechnicalException {
            var now = Instant.ofEpochMilli(1_700_000_000_000L);
            long oneDay = 24L * 60L * 60L * 1000L;
            long windowMs = 60L * 60L * 1000L;
            when(apiKeyRepository.findByCriteriaUnordered(any(ApiKeyCriteria.class))).thenReturn(List.of());

            service.findExpiringApiKeys(now, List.of(30, 45, 90), windowMs);

            ArgumentCaptor<ApiKeyCriteria> captor = ArgumentCaptor.forClass(ApiKeyCriteria.class);
            Mockito.verify(apiKeyRepository, Mockito.times(1)).findByCriteriaUnordered(captor.capture());
            ApiKeyCriteria criteria = captor.getValue();
            assertThat(criteria.isIncludeRevoked()).isFalse();
            assertThat(criteria.isIncludeFederated()).isTrue();
            assertThat(criteria.isIncludeWithoutExpiration()).isFalse();
            assertThat(criteria.getExpireAfter()).isEqualTo(now.toEpochMilli() + 30 * oneDay);
            assertThat(criteria.getExpireBefore()).isEqualTo(now.toEpochMilli() + 90 * oneDay + windowMs);
        }

        @Test
        void should_skip_orphaned_subscription_references() throws TechnicalException {
            var now = Instant.ofEpochMilli(1_700_000_000_000L);
            ApiKey repoKey = ApiKey.builder()
                .id("key-id")
                .key("the-key-value")
                .application("app-id")
                .subscriptions(List.of("sub-found", "sub-missing"))
                .expireAt(new Date(now.toEpochMilli() + 30L * 24 * 60 * 60 * 1000))
                .build();
            when(apiKeyRepository.findByCriteriaUnordered(any(ApiKeyCriteria.class))).thenReturn(List.of(repoKey));
            // sub-missing is intentionally not returned — simulates deletion between key write and our query
            Subscription subFound = Subscription.builder().id("sub-found").api("api-x").plan("plan-x").subscribedBy("user-x").build();
            when(subscriptionRepository.findByIdIn(any())).thenReturn(List.of(subFound));

            List<ExpiringApiKey> result = service.findExpiringApiKeys(now, List.of(30), 60_000L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).subscriptions()).containsExactly(
                new ExpiringApiKeySubscription("sub-found", "api-x", "plan-x", "user-x")
            );
        }

        @Test
        void should_map_api_keys_with_inline_subscription_projections_resolved_in_one_batch() throws TechnicalException {
            var now = Instant.ofEpochMilli(1_700_000_000_000L);
            Date expireDate = new Date(now.toEpochMilli() + 30L * 24 * 60 * 60 * 1000);
            ApiKey repoKey = ApiKey.builder()
                .id("key-id")
                .key("the-key-value")
                .application("app-id")
                .subscriptions(List.of("sub-a", "sub-b"))
                .expireAt(expireDate)
                .daysToExpirationOnLastNotification(45)
                .build();
            when(apiKeyRepository.findByCriteriaUnordered(any(ApiKeyCriteria.class))).thenReturn(List.of(repoKey));

            Subscription subA = Subscription.builder().id("sub-a").api("api-a").plan("plan-a").subscribedBy("user-a").build();
            Subscription subB = Subscription.builder().id("sub-b").api("api-b").plan("plan-b").subscribedBy("user-b").build();
            when(subscriptionRepository.findByIdIn(any())).thenReturn(List.of(subA, subB));

            List<ExpiringApiKey> result = service.findExpiringApiKeys(now, List.of(30), 60_000L);

            assertThat(result).hasSize(1);
            ExpiringApiKey mapped = result.get(0);
            assertThat(mapped.id()).isEqualTo("key-id");
            assertThat(mapped.key()).isEqualTo("the-key-value");
            assertThat(mapped.applicationId()).isEqualTo("app-id");
            assertThat(mapped.daysToExpirationOnLastNotification()).isEqualTo(45);
            assertThat(mapped.expireAt().toInstant()).isEqualTo(expireDate.toInstant());
            assertThat(mapped.subscriptions()).containsExactlyInAnyOrder(
                new ExpiringApiKeySubscription("sub-a", "api-a", "plan-a", "user-a"),
                new ExpiringApiKeySubscription("sub-b", "api-b", "plan-b", "user-b")
            );

            ArgumentCaptor<Collection<String>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
            Mockito.verify(subscriptionRepository, Mockito.times(1)).findByIdIn(idsCaptor.capture());
            assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder("sub-a", "sub-b");
        }

        @Test
        void should_throw_technical_management_exception_when_repo_fails() throws TechnicalException {
            when(apiKeyRepository.findByCriteriaUnordered(any(ApiKeyCriteria.class))).thenThrow(TechnicalException.class);

            Throwable thrown = catchThrowable(() -> service.findExpiringApiKeys(Instant.now(), List.of(30), 60_000L));

            assertThat(thrown).isInstanceOf(TechnicalManagementException.class);
        }

        @Test
        void should_throw_when_subscription_resolution_fails() throws TechnicalException {
            var now = Instant.ofEpochMilli(1_700_000_000_000L);
            ApiKey repoKey = ApiKey.builder()
                .id("key-id")
                .key("the-key-value")
                .application("app-id")
                .subscriptions(List.of("sub-a"))
                .expireAt(new Date(now.toEpochMilli() + 30L * 24 * 60 * 60 * 1000))
                .build();
            when(apiKeyRepository.findByCriteriaUnordered(any(ApiKeyCriteria.class))).thenReturn(List.of(repoKey));
            when(subscriptionRepository.findByIdIn(any())).thenThrow(TechnicalException.class);

            Throwable thrown = catchThrowable(() -> service.findExpiringApiKeys(now, List.of(30), 60_000L));

            assertThat(thrown)
                .isInstanceOf(TechnicalManagementException.class)
                .hasMessage("An error occurs while resolving subscriptions for expiring API keys");
        }

        @Test
        void should_return_empty_list_when_days_buckets_is_null() throws TechnicalException {
            var result = service.findExpiringApiKeys(Instant.ofEpochMilli(1_700_000_000_000L), null, 60_000L);

            assertThat(result).isEmpty();
            Mockito.verifyNoInteractions(apiKeyRepository);
        }
    }

    private ApiKey.ApiKeyBuilder anActiveApiKey() {
        return anApiKey().revoked(false).paused(false).revokedAt(null).expireAt(Date.from(Instant.parse("2051-02-01T20:22:02.00Z")));
    }

    private void mockActiveSubscription(String subscriptionId, String apiId) throws TechnicalException {
        when(subscriptionRepository.findByIdIn(Set.of(subscriptionId))).thenReturn(
            List.of(Subscription.builder().id(subscriptionId).api(apiId).status(Subscription.Status.ACCEPTED).build())
        );
    }

    private ApiKey.ApiKeyBuilder anApiKeyForApplication(String applicationId) {
        return anApiKey().application(applicationId);
    }

    private ApiKey.ApiKeyBuilder anApiKeyForSubscription(String subscriptionId) {
        return anApiKey().subscriptions(List.of(subscriptionId));
    }

    private ApiKey.ApiKeyBuilder anApiKey() {
        return ApiKey.builder()
            .key("c080f684-2c35-40a1-903c-627c219e0567")
            .id("api-key-id")
            .application("application-id")
            .subscriptions(List.of("subscription-id"))
            .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
            .updatedAt(Date.from(Instant.parse("2020-02-02T20:22:02.00Z")))
            .revokedAt(Date.from(Instant.parse("2020-02-03T20:22:02.00Z")))
            .expireAt(Date.from(Instant.parse("2021-02-01T20:22:02.00Z")))
            .revoked(true)
            .paused(true)
            .daysToExpirationOnLastNotification(310);
    }
}
