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
package io.gravitee.gateway.services.sync.process.common.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionConfiguration;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@CustomLog
public class SubscriptionMapper {

    /**
     * Deduplicates the low-cardinality fields shared by many subscriptions (a few thousand
     * distinct apis/plans/applications for millions of cached subscriptions, ~90 bytes wasted per
     * private copy). Weak interner: entries are reclaimed by the GC as soon as no cached
     * subscription references them, so the pool cleans itself up and never outlives its values.
     */
    private static final Interner<String> STRING_INTERNER = Interners.newWeakInterner();

    private final ObjectMapper objectMapper;
    private final ApiProductRegistry apiProductRegistry;

    public List<Subscription> to(io.gravitee.repository.management.model.Subscription subscriptionModel) {
        try {
            if (subscriptionModel.getReferenceType() == SubscriptionReferenceType.API_PRODUCT) {
                return explodeApiProductSubscription(subscriptionModel);
            }

            // Regular API subscription - return as single-item list
            return List.of(toSubscription(subscriptionModel));
        } catch (Exception e) {
            log.warn("Unable to map subscription from model [{}].", subscriptionModel.getId(), e);
            return List.of(); // Return empty list on error
        }
    }

    private List<Subscription> explodeApiProductSubscription(io.gravitee.repository.management.model.Subscription subscriptionModel) {
        String productId = subscriptionModel.getReferenceId();
        if (productId == null) {
            log.warn("API Product subscription [{}] has null referenceId, skipping", subscriptionModel.getId());
            return List.of();
        }

        // Get product from registry (requires productId and environmentId)
        String environmentId = subscriptionModel.getEnvironmentId();
        if (environmentId == null) {
            log.warn("API Product subscription [{}] has null environmentId, skipping", subscriptionModel.getId());
            return List.of();
        }
        ReactableApiProduct product = apiProductRegistry.get(productId, environmentId);
        if (product == null) {
            return List.of(); // Product not deployed yet - skip for now
        }

        Set<String> apiIds = product.getApiIds();

        if (apiIds == null || apiIds.isEmpty()) {
            log.warn("API Product [{}] has no APIs, subscription [{}] not deployed", productId, subscriptionModel.getId());
            return List.of();
        }

        // Create one subscription per API in product
        return apiIds
            .stream()
            .map(apiId -> {
                Subscription sub = toSubscription(subscriptionModel);
                sub.setApi(apiId); // Override with individual API
                sub.setApiProductId(productId);

                return sub;
            })
            .toList();
    }

    private Subscription toSubscription(io.gravitee.repository.management.model.Subscription subscriptionModel) {
        Subscription subscription = new Subscription();
        subscription.setApi(intern(resolveApiId(subscriptionModel)));
        subscription.setApplication(intern(subscriptionModel.getApplication()));
        subscription.setApplicationName(intern(subscriptionModel.getApplicationName()));
        subscription.setClientId(subscriptionModel.getClientId());
        subscription.setClientCertificate(subscriptionModel.getClientCertificate());
        subscription.setStartingAt(subscriptionModel.getStartingAt());
        subscription.setEndingAt(subscriptionModel.getEndingAt());
        subscription.setId(subscriptionModel.getId());
        subscription.setPlan(intern(subscriptionModel.getPlan()));
        if (subscriptionModel.getStatus() != null) {
            subscription.setStatus(subscriptionModel.getStatus().name());
        }
        if (subscriptionModel.getConsumerStatus() != null) {
            subscription.setConsumerStatus(Subscription.ConsumerStatus.valueOf(subscriptionModel.getConsumerStatus().name()));
        }
        if (subscriptionModel.getType() != null) {
            subscription.setType(Subscription.Type.valueOf(subscriptionModel.getType().name().toUpperCase()));
        }
        if (subscriptionModel.getConfiguration() != null) {
            try {
                subscription.setConfiguration(
                    objectMapper.readValue(subscriptionModel.getConfiguration(), SubscriptionConfiguration.class)
                );
            } catch (Exception e) {
                log.warn("Unable to parse subscription configuration for [{}]", subscriptionModel.getId(), e);
            }
        }
        // Share the JVM's empty-map singleton instead of allocating one HashMap per subscription:
        // on large deployments most subscriptions carry no metadata and the per-instance maps
        // account for hundreds of MB. Consumers are read-only (template engine variable). The
        // non-empty case keeps HashMap on purpose: unlike Map.copyOf it tolerates null values.
        Map<String, String> sourceMetadata = subscriptionModel.getMetadata();
        subscription.setMetadata(sourceMetadata == null || sourceMetadata.isEmpty() ? Map.of() : new HashMap<>(sourceMetadata));
        subscription.setEnvironmentId(intern(subscriptionModel.getEnvironmentId()));
        return subscription;
    }

    /**
     * Creates a Subscription for a specific API from a repository model.
     * Used when unregistering product subscriptions for APIs removed from the product.
     */
    public Subscription toSubscriptionForApi(io.gravitee.repository.management.model.Subscription subscriptionModel, String apiId) {
        Subscription sub = toSubscription(subscriptionModel);
        sub.setApi(apiId);
        if (subscriptionModel.getReferenceId() != null) {
            sub.setApiProductId(subscriptionModel.getReferenceId());
        }
        return sub;
    }

    /** Never intern id/clientId/clientCertificate: unique per subscription, they would only pollute the pool. */
    private static String intern(String value) {
        return value == null ? null : STRING_INTERNER.intern(value);
    }

    /** Resolve API id: API ref uses referenceId or legacy api; API Product uses api. */
    private String resolveApiId(io.gravitee.repository.management.model.Subscription subscriptionModel) {
        if (subscriptionModel.getReferenceType() == SubscriptionReferenceType.API) {
            return subscriptionModel.getReferenceId() != null ? subscriptionModel.getReferenceId() : subscriptionModel.getApi();
        }
        return subscriptionModel.getApi();
    }
}
