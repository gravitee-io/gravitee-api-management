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
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.ApiProductTemplateModel;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.v4.ApiProductGroupService;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class ApiProductGroupServiceImpl implements ApiProductGroupService {

    private final ApiProductsRepository apiProductsRepository;
    private final NotifierService notifierService;
    private final MembershipService membershipService;
    private final UserService userService;

    public ApiProductGroupServiceImpl(
        @Lazy final ApiProductsRepository apiProductsRepository,
        final NotifierService notifierService,
        @Lazy final MembershipService membershipService,
        final UserService userService
    ) {
        this.apiProductsRepository = apiProductsRepository;
        this.notifierService = notifierService;
        this.membershipService = membershipService;
        this.userService = userService;
    }

    @Override
    public void addGroup(ExecutionContext executionContext, String apiProductId, String groupId) {
        try {
            log.debug("Add group {} to API Product {}", groupId, apiProductId);

            ApiProduct apiProduct = findApiProductInEnvironment(executionContext, apiProductId);
            apiProduct.addGroup(groupId);
            apiProductsRepository.update(apiProduct);
            triggerUpdateNotification(executionContext, apiProduct);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to add group {} to API Product {}: {}", groupId, apiProductId, ex);
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
                triggerUpdateNotification(executionContext, apiProduct);
            }
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to remove group {} from API Product {}: {}", groupId, apiProductId, ex);
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

    private void triggerUpdateNotification(ExecutionContext executionContext, ApiProduct apiProduct) {
        PrimaryOwnerEntity primaryOwner = null;
        try {
            String ownerUserId = membershipService.getPrimaryOwnerUserId(
                executionContext.getOrganizationId(),
                MembershipReferenceType.API_PRODUCT,
                apiProduct.getId()
            );
            if (ownerUserId != null) {
                primaryOwner = new PrimaryOwnerEntity(userService.findById(executionContext, ownerUserId));
            }
        } catch (Exception e) {
            log.debug("Could not resolve primary owner for API Product {} notification", apiProduct.getId(), e);
        }
        ApiProductTemplateModel model = ApiProductTemplateModel.builder()
            .id(apiProduct.getId())
            .name(apiProduct.getName())
            .version(apiProduct.getVersion() != null ? apiProduct.getVersion() : "")
            .primaryOwner(primaryOwner)
            .build();
        notifierService.trigger(
            executionContext,
            ApiHook.API_UPDATED,
            NotificationReferenceType.API_PRODUCT,
            apiProduct.getId(),
            new NotificationParamsBuilder().apiProduct(model).build()
        );
    }
}
