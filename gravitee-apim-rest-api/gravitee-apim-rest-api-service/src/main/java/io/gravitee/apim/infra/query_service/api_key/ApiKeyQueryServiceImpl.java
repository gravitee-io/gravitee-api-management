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

import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.model.ExpiringApiKey;
import io.gravitee.apim.core.api_key.model.ExpiringApiKeySubscription;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.infra.adapter.ApiKeyAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class ApiKeyQueryServiceImpl implements ApiKeyQueryService {

    private final ApiKeyRepository apiKeyRepository;
    private final SubscriptionRepository subscriptionRepository;

    public ApiKeyQueryServiceImpl(@Lazy ApiKeyRepository apiKeyRepository, @Lazy SubscriptionRepository subscriptionRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Optional<ApiKeyEntity> findById(String id) {
        try {
            return apiKeyRepository.findById(id).map(ApiKeyAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find API key by id: " + id, e);
        }
    }

    @Override
    public Stream<ApiKeyEntity> findByApplication(String applicationId) {
        try {
            return apiKeyRepository.findByApplication(applicationId).stream().map(ApiKeyAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find API keys by application id: " + applicationId, e);
        }
    }

    @Override
    public Optional<ApiKeyEntity> findByKeyAndApiId(String key, String apiId) {
        try {
            return apiKeyRepository.findByKeyAndApi(key, apiId).map(ApiKeyAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to find API key by [key=%s] and [apiId=%s]", key, apiId),
                e
            );
        }
    }

    @Override
    public Stream<ApiKeyEntity> findBySubscription(String subscriptionId) {
        try {
            return apiKeyRepository.findBySubscription(subscriptionId).stream().map(ApiKeyAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurs while trying to find API keys by subscription id: " + subscriptionId,
                e
            );
        }
    }

    @Override
    public List<ExpiringApiKey> findExpiringApiKeys(Instant now, List<Integer> daysBuckets, long windowMs) {
        if (daysBuckets == null || daysBuckets.isEmpty()) {
            return List.of();
        }
        // Outer envelope of all per-bucket windows: one repository call covers every bucket. Per-bucket
        // assignment then happens client-side. Trades a wider DB filter for N→1 round-trips.
        long oneDayMs = Duration.ofDays(1).toMillis();
        long min = daysBuckets.stream().min(Comparator.naturalOrder()).get();
        long max = daysBuckets.stream().max(Comparator.naturalOrder()).get();
        long nowMs = now.toEpochMilli();
        long expireAfter = nowMs + min * oneDayMs;
        long expireBefore = nowMs + max * oneDayMs + windowMs;

        ApiKeyCriteria criteria = ApiKeyCriteria.builder()
            .includeRevoked(false)
            .includeFederated(true)
            .expireAfter(expireAfter)
            .expireBefore(expireBefore)
            .build();

        List<ApiKey> apiKeys;
        try {
            apiKeys = apiKeyRepository.findByCriteriaUnordered(criteria);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while querying expiring API keys", e);
        }
        if (apiKeys.isEmpty()) {
            return List.of();
        }
        Set<String> subscriptionIds = apiKeys
            .stream()
            .flatMap(k -> k.getSubscriptions() == null ? Stream.<String>empty() : k.getSubscriptions().stream())
            .collect(Collectors.toSet());
        Map<String, Subscription> subscriptionsById;
        try {
            subscriptionsById = subscriptionIds.isEmpty()
                ? Map.of()
                : subscriptionRepository.findByIdIn(subscriptionIds).stream().collect(Collectors.toMap(Subscription::getId, s -> s));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while resolving subscriptions for expiring API keys", e);
        }
        return apiKeys
            .stream()
            .map(k -> toExpiring(k, subscriptionsById))
            .toList();
    }

    private static ExpiringApiKey toExpiring(ApiKey apiKey, Map<String, Subscription> subscriptionsById) {
        ZonedDateTime expireAt = apiKey.getExpireAt() == null ? null : apiKey.getExpireAt().toInstant().atZone(ZoneOffset.UTC);
        List<ExpiringApiKeySubscription> subs = apiKey.getSubscriptions() == null
            ? List.of()
            : apiKey
                .getSubscriptions()
                .stream()
                .map(subId -> {
                    Subscription s = subscriptionsById.get(subId);
                    if (s == null) {
                        // Data skew (subscription deleted between key write and our query). Surfacing here
                        // gives ops a trail; the key still flows through with whatever subs remain.
                        log.warn("API key {} references unknown subscription {} — skipping that projection", apiKey.getId(), subId);
                    }
                    return s;
                })
                .filter(java.util.Objects::nonNull)
                .map(s -> new ExpiringApiKeySubscription(s.getId(), s.getApi(), s.getPlan(), s.getSubscribedBy()))
                .toList();
        return new ExpiringApiKey(
            apiKey.getId(),
            apiKey.getKey(),
            expireAt,
            apiKey.getDaysToExpirationOnLastNotification(),
            apiKey.getApplication(),
            subs
        );
    }

    @Override
    public Optional<ApiKeyEntity> findByKeyAndReferenceIdAndReferenceType(String key, String referenceId, String referenceType) {
        try {
            if (
                !SubscriptionReferenceType.API.name().equals(referenceType) &&
                !SubscriptionReferenceType.API_PRODUCT.name().equals(referenceType)
            ) {
                throw new IllegalArgumentException("Unsupported reference type: " + referenceType);
            }
            return apiKeyRepository
                .findByKeyAndReferenceIdAndReferenceType(key, referenceId, referenceType)
                .map(ApiKeyAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format(
                    "An error occurs while trying to find API key by [key=%s], [referenceId=%s], [referenceType=%s]",
                    key,
                    referenceId,
                    referenceType
                ),
                e
            );
        }
    }
}
