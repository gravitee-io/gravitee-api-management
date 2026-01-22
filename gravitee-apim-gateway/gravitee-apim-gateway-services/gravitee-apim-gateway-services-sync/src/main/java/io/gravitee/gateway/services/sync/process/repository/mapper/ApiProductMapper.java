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
package io.gravitee.gateway.services.sync.process.repository.mapper;

import static io.gravitee.repository.management.model.Event.EventProperties.API_PRODUCT_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.repository.management.model.Event;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Date;
import java.util.Set;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class ApiProductMapper {

    private final ObjectMapper objectMapper;
    private final EnvironmentService environmentService;

    /**
     * Extract API Product ID from event
     */
    public Maybe<String> toId(Event apiProductEvent) {
        return Maybe.fromCallable(() -> {
            String apiProductId = null;
            if (apiProductEvent.getProperties() != null) {
                apiProductId = apiProductEvent.getProperties().get(API_PRODUCT_ID.getValue());
            }
            if (apiProductId == null) {
                log.warn("Unable to extract API Product info from event [{}].", apiProductEvent.getId());
            }
            return apiProductId;
        });
    }

    /**
     * Convert Event to ReactableApiProduct wrapped in ApiProductReactorDeployable
     *
     * @param event the repository event containing API Product data
     * @return ApiProductReactorDeployable or null if mapping fails
     */
    public Maybe<ReactableApiProduct> to(Event event) {
        return Maybe.fromCallable(() -> {
            try {
                if (event.getPayload() == null || event.getPayload().isBlank()) {
                    log.warn("Unable to extract API Product definition from event [{}] because payload is empty.", event.getId());
                    return null;
                }
                // Parse the event payload
                ApiProductPayload payload = objectMapper.readValue(event.getPayload(), ApiProductPayload.class);

                final ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
                    .id(payload.getId())
                    .name(payload.getName())
                    .description(payload.getDescription())
                    .version(payload.getVersion())
                    .apiIds(payload.getApiIds())
                    .environmentId(payload.getEnvironmentId())
                    .deployedAt(new Date(event.getCreatedAt().getTime()))
                    .build();

                // Fill environment and organization details
                environmentService.fill(payload.getEnvironmentId(), reactableApiProduct);

                return reactableApiProduct;
            } catch (Exception e) {
                log.error("Unable to extract API Product definition from event [{}].", event.getId(), e);
                return null;
            }
        });
    }

    /**
     * Inner class representing the Event payload structure from MAPI
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ApiProductPayload {

        private String id;
        private String name;
        private String description;
        private String version;
        private Set<String> apiIds;
        private String environmentId;
        private String environmentHrid;
        private String organizationId;
        private String organizationHrid;
    }
}
