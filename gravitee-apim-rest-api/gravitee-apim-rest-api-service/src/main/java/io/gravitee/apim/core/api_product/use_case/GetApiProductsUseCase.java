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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.domain_service.ApiProductAccessibleIdsDomainService;
import io.gravitee.apim.core.api_product.domain_service.ApiProductDeploymentStateDomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.model.ApiProductKindFilter;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApiProductsUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;
    private final ApiProductDeploymentStateDomainService apiProductDeploymentStateDomainService;
    private final ApiProductAccessibleIdsDomainService apiProductAccessibleIdsDomainService;

    public Output execute(Input input) {
        if (input.apiProductId() != null) {
            Optional<ApiProduct> apiProduct = apiProductQueryService
                .findById(input.apiProductId())
                .filter(p -> p.getEnvironmentId().equals(input.environmentId()))
                .filter(input.kindFilter()::matches)
                .map(product -> addPrimaryOwner(product, input.organizationId()))
                .map(apiProductDeploymentStateDomainService::computeDeploymentState);
            return Output.single(apiProduct);
        }

        if (input.environmentId() == null) {
            throw new IllegalArgumentException("environmentId must be provided for listing API Products");
        }

        Set<ApiProduct> apiProducts = findAccessibleApiProducts(input)
            .stream()
            .filter(input.kindFilter()::matches)
            .collect(Collectors.toSet());
        addPrimaryOwners(apiProducts, input.organizationId());
        apiProductDeploymentStateDomainService.computeDeploymentState(apiProducts);
        return Output.multiple(apiProducts);
    }

    private Set<ApiProduct> findAccessibleApiProducts(Input input) {
        if (input.isAdmin()) {
            return apiProductQueryService.findByEnvironmentId(input.environmentId());
        }
        Set<String> allowedIds = apiProductAccessibleIdsDomainService.findAccessibleApiProductIds(input.environmentId(), input.userId());
        if (allowedIds.isEmpty()) {
            return Set.of();
        }
        return apiProductQueryService.findByEnvironmentIdAndIdIn(input.environmentId(), allowedIds);
    }

    private ApiProduct addPrimaryOwner(ApiProduct apiProduct, String organizationId) {
        addPrimaryOwners(Set.of(apiProduct), organizationId);
        return apiProduct;
    }

    private void addPrimaryOwners(Set<ApiProduct> apiProducts, String organizationId) {
        Map<String, PrimaryOwnerEntity> primaryOwnersById = apiProductPrimaryOwnerDomainService.getApiProductPrimaryOwners(
            organizationId,
            apiProducts.stream().map(ApiProduct::getId).collect(Collectors.toSet())
        );
        apiProducts.forEach(apiProduct -> apiProduct.setPrimaryOwner(primaryOwnersById.get(apiProduct.getId())));
    }

    public record Input(
        String environmentId,
        String apiProductId,
        String organizationId,
        String userId,
        boolean isAdmin,
        ApiProductKindFilter kindFilter
    ) {
        public Input {
            kindFilter = kindFilter == null ? ApiProductKindFilter.any() : kindFilter;
        }

        public static Input of(String environmentId, String organizationId, String userId, boolean isAdmin) {
            return new Input(environmentId, null, organizationId, userId, isAdmin, ApiProductKindFilter.any());
        }

        public static Input of(
            String environmentId,
            String organizationId,
            String userId,
            boolean isAdmin,
            ApiProductKindFilter kindFilter
        ) {
            return new Input(environmentId, null, organizationId, userId, isAdmin, kindFilter);
        }

        public static Input of(String environmentId, String apiProductId, String organizationId) {
            return new Input(environmentId, apiProductId, organizationId, null, false, ApiProductKindFilter.any());
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
