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

import static io.gravitee.repository.management.model.Api.AuditEvent.API_UPDATED;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class ApiCategoryServiceImpl implements ApiCategoryService {

    private final ApiRepository apiRepository;
    private final CategoryService categoryService;
    private final ApiNotificationService apiNotificationService;
    private final AuditService auditService;

    public ApiCategoryServiceImpl(
        @Lazy final ApiRepository apiRepository,
        final CategoryService categoryService,
        final ApiNotificationService apiNotificationService,
        final AuditService auditService
    ) {
        this.apiRepository = apiRepository;
        this.categoryService = categoryService;
        this.apiNotificationService = apiNotificationService;
        this.auditService = auditService;
    }

    @Override
    public Set<CategoryEntity> listCategories(Collection<String> apis, String environment) {
        try {
            ApiCriteria criteria = new ApiCriteria.Builder().ids(apis.toArray(new String[apis.size()])).build();
            Set<String> categoryIds = apiRepository.listCategories(criteria);
            return categoryService.findByIdIn(environment, categoryIds);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to list categories for APIs {}", apis, ex);
            throw new TechnicalManagementException("An error occurs while trying to list categories for APIs {}" + apis, ex);
        }
    }

    @Override
    public void deleteCategoryFromAPIs(ExecutionContext executionContext, final String categoryId) {
        apiRepository
            .search(new ApiCriteria.Builder().category(categoryId).build(), null, ApiFieldFilter.allFields())
            .forEach(api -> removeCategory(executionContext, api, categoryId));
    }

    private void removeCategory(ExecutionContext executionContext, Api api, String categoryId) {
        try {
            Api apiSnapshot = new Api(api);
            api.getCategories().remove(categoryId);
            api.setUpdatedAt(new Date());
            apiRepository.update(api);
            apiNotificationService.triggerUpdateNotification(executionContext, api);
            auditService.createApiAuditLog(
                executionContext,
                api.getId(),
                Collections.emptyMap(),
                API_UPDATED,
                api.getUpdatedAt(),
                apiSnapshot,
                api
            );
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error has occurred while removing category " + categoryId + " from API " + api.getId(),
                e
            );
        }
    }
}
