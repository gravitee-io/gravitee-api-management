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
package io.gravitee.apim.infra.crud_service.api_product;

import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.ApiProductAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class ApiProductCrudServiceImpl implements ApiProductCrudService {

    private final ApiProductsRepository apiProductsRepository;

    public ApiProductCrudServiceImpl(@Lazy ApiProductsRepository apiProductsRepository) {
        this.apiProductsRepository = apiProductsRepository;
    }

    @Override
    public ApiProduct create(ApiProduct apiProduct) {
        try {
            log.debug("Creating API Product: {}", apiProduct.getId());

            var repositoryModel = ApiProductAdapter.INSTANCE.toRepository(apiProduct);
            var createdRepositoryModel = apiProductsRepository.create(repositoryModel);
            var createdDomainModel = ApiProductAdapter.INSTANCE.toModel(createdRepositoryModel);

            log.debug("API Product created successfully: {}", createdDomainModel.getId());
            return createdDomainModel;
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to create the API Product: " + apiProduct.getId(), e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            apiProductsRepository.delete(id);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to delete the api product: " + id, e);
        }
    }

    @Override
    public ApiProduct update(ApiProduct updateApiProduct) {
        try {
            var repositoryModel = ApiProductAdapter.INSTANCE.toRepository(updateApiProduct);
            if (repositoryModel.getUpdatedAt() == null) {
                repositoryModel.setUpdatedAt(java.util.Date.from(java.time.ZonedDateTime.now().toInstant()));
            }
            if (updateApiProduct.getApiIds() != null) {
                repositoryModel.setApiIds(new ArrayList<>(updateApiProduct.getApiIds()));
            }
            var updatedRepositoryModel = apiProductsRepository.update(repositoryModel);
            return ApiProductAdapter.INSTANCE.toModel(updatedRepositoryModel);
        } catch (TechnicalException e) {
            throw TechnicalManagementException.ofTryingToUpdateWithId(ApiProduct.class, updateApiProduct.getId(), e);
        }
    }

    @Override
    public ApiProduct get(String id) {
        try {
            var foundApi = apiProductsRepository.findById(id);
            if (foundApi.isPresent()) {
                return ApiProductAdapter.INSTANCE.toModel(foundApi.get());
            }
        } catch (TechnicalException e) {
            log.error("An error occurred while finding Api Product by id {}", id, e);
        }
        throw new ApiProductNotFoundException(id);
    }
}
