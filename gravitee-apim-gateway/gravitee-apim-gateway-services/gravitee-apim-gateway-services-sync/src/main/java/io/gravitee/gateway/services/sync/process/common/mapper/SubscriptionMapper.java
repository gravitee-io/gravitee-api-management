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

    public static final String METADATA_REFERENCE_ID = "referenceId";
    public static final String METADATA_REFERENCE_TYPE = "referenceType";

    private final ObjectMapper objectMapper;
    private final ApiProductRegistry apiProductRegistry;

    public List<Subscription> to(io.gravitee.repository.management.model.Subscription subscriptionModel) {
        try {
            if (subscriptionModel.getReferenceType() == SubscriptionReferenceType.API_PRODUCT) {
                return explodeApiProductSubscription(subscriptionModel);
            }

            // Regular API subscription - return as single-item list
            return List.of(mapToGatewaySubscription(subscriptionModel));
        } catch (Exception e) {
            log.error("Unable to map subscription from model [{}].", subscriptionModel.getId(), e);
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
                Subscription sub = mapToGatewaySubscription(subscriptionModel);
                sub.setApi(apiId); // Override with individual API

                // Preserve product info in metadata for debugging/tracking
                Map<String, String> metadata = sub.getMetadata();
                if (metadata == null) {
                    metadata = new HashMap<>();
                    sub.setMetadata(metadata);
                }
                metadata.put("productId", productId);
                metadata.put("exploded", "true");

                return sub;
            })
            .toList();
    }

    private Subscription mapToGatewaySubscription(io.gravitee.repository.management.model.Subscription subscriptionModel) {
        Subscription subscription = new Subscription();
        subscription.setApi(resolveApiId(subscriptionModel));
        subscription.setApplication(subscriptionModel.getApplication());
        subscription.setApplicationName(subscriptionModel.getApplicationName());
        subscription.setClientId(subscriptionModel.getClientId());
        subscription.setClientCertificate(subscriptionModel.getClientCertificate());
        subscription.setStartingAt(subscriptionModel.getStartingAt());
        subscription.setEndingAt(subscriptionModel.getEndingAt());
        subscription.setId(subscriptionModel.getId());
        subscription.setPlan(subscriptionModel.getPlan());
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
                log.error("Unable to parse subscription configuration for [{}]", subscriptionModel.getId(), e);
            }
        }
        Map<String, String> metadata = subscriptionModel.getMetadata() != null
            ? new HashMap<>(subscriptionModel.getMetadata())
            : new HashMap<>();
        if (subscriptionModel.getReferenceId() != null) {
            metadata.put(METADATA_REFERENCE_ID, subscriptionModel.getReferenceId());
        }
        if (subscriptionModel.getReferenceType() != null) {
            metadata.put(METADATA_REFERENCE_TYPE, subscriptionModel.getReferenceType().name());
        }
        subscription.setMetadata(metadata);
        subscription.setEnvironmentId(subscriptionModel.getEnvironmentId());
        return subscription;
    }

    /** Resolve API id: API ref uses referenceId or legacy api; API Product uses api. */
    private String resolveApiId(io.gravitee.repository.management.model.Subscription subscriptionModel) {
        if (subscriptionModel.getReferenceType() == SubscriptionReferenceType.API) {
            return subscriptionModel.getReferenceId() != null ? subscriptionModel.getReferenceId() : subscriptionModel.getApi();
        }
        return subscriptionModel.getApi();
    }
}
