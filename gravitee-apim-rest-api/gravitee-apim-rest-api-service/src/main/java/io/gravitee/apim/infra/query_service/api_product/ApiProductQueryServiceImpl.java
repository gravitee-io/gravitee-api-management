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
package io.gravitee.apim.infra.query_service.api_product;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.ApiProductAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApiProductQueryServiceImpl implements ApiProductQueryService {

    private final ApiProductsRepository apiProductRepository;

    public ApiProductQueryServiceImpl(@Lazy ApiProductsRepository apiProductRepository) {
        this.apiProductRepository = apiProductRepository;
    }

    @Override
    public Optional<ApiProduct> findByEnvironmentIdAndName(String environmentId, String name) {
        try {
            log.debug("Finding API Product by environmentId: {} and name: {}", environmentId, name);

            return apiProductRepository.findByEnvironmentIdAndName(environmentId, name).map(ApiProductAdapter.INSTANCE::toModel);
        } catch (TechnicalException ex) {
            String errorMessage = String.format(
                "An error occurred while finding API Product by environmentId: %s and name: %s",
                environmentId,
                name
            );
            throw new TechnicalDomainException(errorMessage, ex);
        }
    }

    @Override
    public Set<ApiProduct> findByEnvironmentId(String environmentId) {
        try {
            log.debug("Finding all API Products for environment: {}", environmentId);

            Set<io.gravitee.repository.management.model.ApiProduct> repoApiProducts = apiProductRepository.findByEnvironmentId(
                environmentId
            );

            return repoApiProducts.stream().map(ApiProductAdapter.INSTANCE::toModel).collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Failed to find API Products", e);
        }
    }

    @Override
    public Optional<ApiProduct> findById(String apiProductId) {
        try {
            log.debug("Finding API Product by apiProductId: {}", apiProductId);
            if (apiProductId == null) {
                log.debug("apiProductId is null, returning empty");
                return Optional.empty();
            }
            Optional<io.gravitee.repository.management.model.ApiProduct> repoApiProduct = apiProductRepository.findById(apiProductId);
            return repoApiProduct.map(ApiProductAdapter.INSTANCE::toModel);
        } catch (TechnicalException ex) {
            String errorMessage = String.format("An error occurred while finding API Product by apiProductId: %s", apiProductId);
            throw new TechnicalDomainException(errorMessage, ex);
        }
    }

    @Override
    public Set<ApiProduct> findByApiId(String apiId) {
        try {
            log.debug("Finding all API Products containing API ID: {}", apiId);
            Set<io.gravitee.repository.management.model.ApiProduct> repoApiProducts = apiProductRepository.findByApiId(apiId);
            return repoApiProducts.stream().map(ApiProductAdapter.INSTANCE::toModel).collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Failed to find API Products by API ID", e);
        }
    }
}
