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
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.membership.domain_service.ApiProductPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiProductPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class GetApiProductsByApiIdUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiProductPrimaryOwnerDomainService apiProductPrimaryOwnerDomainService;

    public Output execute(Input input) {
        Set<ApiProduct> apiProducts = apiProductQueryService
            .findByApiId(input.apiId())
            .stream()
            .map(product -> addPrimaryOwnerToApiProduct(product, input.organizationId()))
            .collect(Collectors.toSet());
        return new Output(apiProducts);
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

    public record Input(String apiId, String organizationId) {
        public static Input of(String apiId, String organizationId) {
            return new Input(apiId, organizationId);
        }
    }

    public record Output(Set<ApiProduct> apiProducts) {}
}
