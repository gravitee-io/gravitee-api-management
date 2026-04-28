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
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiProductGroupService;
import io.gravitee.rest.api.service.v4.ApiProductNotificationService;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class ApiProductGroupServiceImpl implements ApiProductGroupService {

    private final ApiProductsRepository apiProductsRepository;
    private final ApiProductNotificationService apiProductNotificationService;

    public ApiProductGroupServiceImpl(
        @Lazy final ApiProductsRepository apiProductsRepository,
        final ApiProductNotificationService apiProductNotificationService
    ) {
        this.apiProductsRepository = apiProductsRepository;
        this.apiProductNotificationService = apiProductNotificationService;
    }

    @Override
    public void addGroup(ExecutionContext executionContext, String apiProductId, String groupId) {
        try {
            log.debug("Add group {} to API Product {}", groupId, apiProductId);

            ApiProduct apiProduct = findApiProductInEnvironment(executionContext, apiProductId);
            if (!apiProduct.addGroup(groupId)) {
                return;
            }
            apiProductsRepository.update(apiProduct);
            apiProductNotificationService.triggerUpdateNotification(executionContext, apiProduct);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                "An error occurs while trying to add group " + groupId + " to API Product " + apiProductId,
                ex
            );
        }
    }

    @Override
    public void removeGroup(ExecutionContext executionContext, String apiProductId, String groupId) {
        try {
            log.debug("Remove group {} from API Product {}", groupId, apiProductId);

            ApiProduct apiProduct = findApiProductInEnvironment(executionContext, apiProductId);
            if (apiProduct.getGroups() != null && apiProduct.getGroups().remove(groupId)) {
                apiProductsRepository.update(apiProduct);
                apiProductNotificationService.triggerUpdateNotification(executionContext, apiProduct);
            }
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                "An error occurs while trying to remove group " + groupId + " from API Product " + apiProductId,
                ex
            );
        }
    }

    private ApiProduct findApiProductInEnvironment(ExecutionContext executionContext, String apiProductId) throws TechnicalException {
        Optional<ApiProduct> optProduct = apiProductsRepository.findById(apiProductId);

        if (executionContext.hasEnvironmentId()) {
            optProduct = optProduct.filter(p -> executionContext.getEnvironmentId().equals(p.getEnvironmentId()));
        }

        return optProduct.orElseThrow(() -> new ApiProductNotFoundException("API Product not found: " + apiProductId));
    }
}
