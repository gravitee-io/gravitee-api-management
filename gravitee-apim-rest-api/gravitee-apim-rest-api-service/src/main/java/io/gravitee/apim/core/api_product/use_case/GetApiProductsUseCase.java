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
package io.gravitee.apim.core.api_product.use_case;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiProductPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.EventType;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class GetApiProductsUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;
    private final EventLatestQueryService eventLatestQueryService;
    private final PlanQueryService planQueryService;
    private final ObjectMapper objectMapper;

    public Output execute(Input input) {
        if (input.apiProductId() != null) {
            Optional<ApiProduct> apiProduct = apiProductQueryService
                .findById(input.apiProductId())
                .map(product -> addPrimaryOwnerToApiProduct(product, input.organizationId()))
                .map(this::computeDeploymentState);
            return Output.single(apiProduct);
        } else if (input.environmentId() != null) {
            Set<ApiProduct> apiProducts = apiProductQueryService
                .findByEnvironmentId(input.environmentId())
                .stream()
                .map(product -> addPrimaryOwnerToApiProduct(product, input.organizationId()))
                .map(this::computeDeploymentState)
                .collect(Collectors.toSet());
            return Output.multiple(apiProducts);
        } else {
            throw new IllegalArgumentException("environmentId must be provided for listing API Products");
        }
    }

    private ApiProduct addPrimaryOwnerToApiProduct(ApiProduct apiProduct, String organizationId) {
        try {
            PrimaryOwnerEntity primaryOwner = apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwner(
                organizationId,
                apiProduct.getId()
            );
            apiProduct.setPrimaryOwner(primaryOwner);
        } catch (ApiProductPrimaryOwnerNotFoundException e) {
            log.debug("Failed to retrieve primary owner for API Product [{}]: {}", apiProduct.getId(), e.getMessage());
        }
        return apiProduct;
    }

    private ApiProduct computeDeploymentState(ApiProduct product) {
        try {
            Optional<Event> latestDeployEvent = eventLatestQueryService.findLatestByEntityId(
                product.getId(),
                EventType.DEPLOY_API_PRODUCT,
                Event.EventProperties.API_PRODUCT_ID
            );

            if (latestDeployEvent.isEmpty()) {
                product.setDeploymentState(ApiProduct.DeploymentState.NEED_REDEPLOY);
                return product;
            }

            Event event = latestDeployEvent.get();
            String payload = event.getPayload();
            if (payload == null || payload.isBlank()) {
                product.setDeploymentState(ApiProduct.DeploymentState.NEED_REDEPLOY);
                return product;
            }

            ApiProduct deployedProduct;
            try {
                deployedProduct = objectMapper.readValue(payload, ApiProduct.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize deploy event payload for API Product [{}]: {}", product.getId(), e.getMessage());
                product.setDeploymentState(ApiProduct.DeploymentState.NEED_REDEPLOY);
                return product;
            }
            if (deployedProduct == null) {
                product.setDeploymentState(ApiProduct.DeploymentState.NEED_REDEPLOY);
                return product;
            }

            if (!apiIdsUnchanged(product.getApiIds(), deployedProduct.getApiIds())) {
                product.setDeploymentState(ApiProduct.DeploymentState.NEED_REDEPLOY);
                return product;
            }

            if (event.getUpdatedAt() == null) {
                product.setDeploymentState(ApiProduct.DeploymentState.NEED_REDEPLOY);
                return product;
            }
            Instant lastDeployedAt = event.getUpdatedAt().toInstant();
            boolean anyPlanModifiedAfterDeploy = planQueryService
                .findAllForApiProduct(product.getId())
                .stream()
                .filter(plan -> plan.getPlanStatus() != PlanStatus.STAGING)
                .anyMatch(plan -> plan.getNeedRedeployAt() != null && plan.getNeedRedeployAt().toInstant().isAfter(lastDeployedAt));

            product.setDeploymentState(
                anyPlanModifiedAfterDeploy ? ApiProduct.DeploymentState.NEED_REDEPLOY : ApiProduct.DeploymentState.DEPLOYED
            );
        } catch (Exception e) {
            log.warn("Failed to compute deployment state for API Product [{}]: {}", product.getId(), e.getMessage());
            product.setDeploymentState(ApiProduct.DeploymentState.NEED_REDEPLOY);
        }
        return product;
    }

    /**
     * True if the list of APIs in the product is unchanged compared to the deployed version.
     * NEED_REDEPLOY when apiIds change (user must deploy to sync gateway).
     */
    private static boolean apiIdsUnchanged(Set<String> current, Set<String> deployed) {
        if (current == null && deployed == null) return true;
        if (current == null || deployed == null) return false;
        return current.size() == deployed.size() && current.containsAll(deployed);
    }

    public record Input(String environmentId, String apiProductId, String organizationId) {
        public static Input of(String environmentId, String organizationId) {
            return new Input(environmentId, null, organizationId);
        }

        public static Input of(String environmentId, String apiProductId, String organizationId) {
            return new Input(environmentId, apiProductId, organizationId);
        }
    }

    public record Output(Set<ApiProduct> apiProducts, Optional<ApiProduct> apiProduct) {
        public static Output multiple(Set<ApiProduct> apiProducts) {
            return new Output(apiProducts, Optional.empty());
        }

        public static Output single(Optional<ApiProduct> apiProduct) {
            return new Output(null, apiProduct);
        }
    }
}
