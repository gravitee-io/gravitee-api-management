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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.service.impl.upgrade.upgrader.UpgraderOrder.CLASSIC_CATEGORIES_MIGRATION_UPGRADER;

import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_category.query_service.PortalCategoryQueryService;
import io.gravitee.apim.core.portal_category.use_case.CreatePortalCategoryUseCase;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.UpdatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Category;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * One-time migration of Classic portal categories to Portal Next categories, so portal admins
 * don't need to recreate their category taxonomy after an upgrade. Also migrates the classic
 * API-to-category assignments onto the corresponding Portal Next API navigation items.
 *
 * Ships in the same release as the Portal Next category feature, and the per-environment skip in
 * {@link #applyUpgrade()} means it effectively runs once per environment, against that release's
 * schema. It is deliberately not hardened against the domain/service dependencies it calls (e.g.
 * {@link CreatePortalCategoryUseCase}) changing shape in a later release.
 *
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class ClassicCategoriesMigrationUpgrader implements Upgrader {

    private final EnvironmentRepository environmentRepository;
    private final CategoryRepository categoryRepository;
    private final ApiRepository apiRepository;
    private final PortalCategoryQueryService portalCategoryQueryService;
    private final CreatePortalCategoryUseCase createPortalCategoryUseCase;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationItemCrudService portalNavigationItemCrudService;

    public ClassicCategoriesMigrationUpgrader(
        @Lazy EnvironmentRepository environmentRepository,
        @Lazy CategoryRepository categoryRepository,
        @Lazy ApiRepository apiRepository,
        PortalCategoryQueryService portalCategoryQueryService,
        CreatePortalCategoryUseCase createPortalCategoryUseCase,
        PortalNavigationItemsQueryService portalNavigationItemsQueryService,
        PortalNavigationItemCrudService portalNavigationItemCrudService
    ) {
        this.environmentRepository = environmentRepository;
        this.categoryRepository = categoryRepository;
        this.apiRepository = apiRepository;
        this.portalCategoryQueryService = portalCategoryQueryService;
        this.createPortalCategoryUseCase = createPortalCategoryUseCase;
        this.portalNavigationItemsQueryService = portalNavigationItemsQueryService;
        this.portalNavigationItemCrudService = portalNavigationItemCrudService;
    }

    @Override
    public int getOrder() {
        return CLASSIC_CATEGORIES_MIGRATION_UPGRADER;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(this::applyUpgrade);
    }

    private boolean applyUpgrade() throws TechnicalException {
        for (final var environment : environmentRepository.findAll()) {
            // Portal Next categories already exist for this environment: either this upgrader
            // already ran, or an admin already created Portal Next categories by hand. Either way,
            // migrating classic categories on top would create duplicates.
            if (!portalCategoryQueryService.findByEnvironmentId(environment.getId()).isEmpty()) {
                continue;
            }

            var classicCategories = categoryRepository.findAllByEnvironment(environment.getId());
            if (classicCategories.isEmpty()) {
                continue;
            }

            var apiIdsByClassicCategory = findApiIdsByClassicCategory(environment.getId());
            for (final var classicCategory : classicCategories) {
                migrateClassicCategory(environment.getId(), classicCategory, apiIdsByClassicCategory);
            }
        }
        return true;
    }

    private void migrateClassicCategory(String environmentId, Category classicCategory, Map<String, Set<String>> apiIdsByClassicCategory) {
        PortalCategory createdCategory;
        try {
            createdCategory = createPortalCategoryUseCase
                .execute(
                    new CreatePortalCategoryUseCase.Input(
                        environmentId,
                        ClassicCategoryToPortalCategoryMapper.toCreatePortalCategory(classicCategory)
                    )
                )
                .portalCategory();
        } catch (Exception e) {
            log.warn("Unable to migrate classic category {} to a Portal Next category", classicCategory.getId(), e);
            return;
        }

        var apiIds = new HashSet<String>();
        apiIds.addAll(apiIdsByClassicCategory.getOrDefault(classicCategory.getId(), Set.of()));
        apiIds.addAll(apiIdsByClassicCategory.getOrDefault(classicCategory.getKey(), Set.of()));
        if (apiIds.isEmpty()) {
            return;
        }

        assignApisToPortalCategory(environmentId, apiIds, createdCategory.getId());
    }

    /**
     * Maps each classic category identifier (id or key - {@link Category#getCategories()}'s
     * contents are ambiguous depending on the API's history) to the set of API ids assigned to it,
     * computed once per environment rather than once per classic category.
     */
    private Map<String, Set<String>> findApiIdsByClassicCategory(String environmentId) {
        var apiIdsByCategory = new HashMap<String, Set<String>>();
        for (final var api : apiRepository.search(
            new ApiCriteria.Builder().environmentId(environmentId).build(),
            ApiFieldFilter.defaultFields()
        )) {
            if (api.getCategories() == null) {
                continue;
            }
            for (final var classicCategoryIdOrKey : api.getCategories()) {
                apiIdsByCategory.computeIfAbsent(classicCategoryIdOrKey, key -> new HashSet<>()).add(api.getId());
            }
        }
        return apiIdsByCategory;
    }

    private void assignApisToPortalCategory(String environmentId, Set<String> apiIds, PortalCategoryId portalCategoryId) {
        var navigationApis = portalNavigationItemsQueryService
            .search(
                PortalNavigationItemQueryCriteria.builder()
                    .environmentId(environmentId)
                    .type(PortalNavigationItemType.API)
                    .apiIds(apiIds)
                    .build()
            )
            .stream()
            .filter(PortalNavigationApi.class::isInstance)
            .map(PortalNavigationApi.class::cast)
            .toList();

        for (final var navigationApi : navigationApis) {
            try {
                assignCategoryToNavigationApi(navigationApi, portalCategoryId);
            } catch (Exception e) {
                log.warn("Unable to assign portal category {} to navigation item {}", portalCategoryId, navigationApi.getId(), e);
            }
        }
    }

    private void assignCategoryToNavigationApi(PortalNavigationApi navigationApi, PortalCategoryId portalCategoryId) {
        var newCategoryIds = new ArrayList<>(navigationApi.getCategoryIds());
        newCategoryIds.add(portalCategoryId);

        navigationApi.update(
            UpdatePortalNavigationItem.builder()
                .title(navigationApi.getTitle())
                .segment(navigationApi.getSegment())
                .order(navigationApi.getOrder())
                .published(navigationApi.getPublished())
                .visibility(navigationApi.getVisibility())
                .source(navigationApi.getSource())
                .categoryIds(newCategoryIds)
                .build()
        );
        portalNavigationItemCrudService.update(navigationApi);
    }
}
