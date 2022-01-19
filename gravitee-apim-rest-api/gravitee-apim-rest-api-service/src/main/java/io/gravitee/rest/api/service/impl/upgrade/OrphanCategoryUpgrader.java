/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import static io.gravitee.rest.api.service.impl.upgrade.UpgradeStatus.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Category;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.Upgrader;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class OrphanCategoryUpgrader implements Upgrader, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanCategoryUpgrader.class);

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InstallationService installationService;

    @Override
    public boolean upgrade() {
        InstallationEntity installation = installationService.getOrInitialize();
        if (isStatus(installation, SUCCESS)) {
            LOGGER.info("Skipping {} execution because it has already been successfully executed", this.getClass().getSimpleName());
            return false;
        }

        if (isStatus(installation, RUNNING)) {
            LOGGER.warn("Skipping {} execution because it is already running", this.getClass().getSimpleName());
            return false;
        }

        try {
            setExecutionStatus(installation, RUNNING);
            deleteOrphanCategoryReferences();
            setExecutionStatus(installation, SUCCESS);
        } catch (TechnicalException e) {
            LOGGER.error("Error while deleting orphan categories, execution will be running again on next start", e);
            setExecutionStatus(installation, FAILURE);
        }
        return true;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    private void deleteOrphanCategoryReferences() throws TechnicalException {
        Set<Api> updatedApis = findAndFixApisWithOrphanCategories();
        for (Api api : updatedApis) {
            LOGGER.info("Removing orphan categories for API [{}]", api.getId());
            apiRepository.update(api);
        }
    }

    private Set<Api> findAndFixApisWithOrphanCategories() throws TechnicalException {
        Set<String> existingCategoryIds = getExistingCategoryIds();

        return apiRepository
            .findAll()
            .stream()
            .filter(api -> hasOrphanCategories(api, existingCategoryIds))
            .peek(api -> removeOrphanCategories(api, existingCategoryIds))
            .collect(Collectors.toSet());
    }

    private void removeOrphanCategories(Api api, Set<String> existingCategoryIds) {
        HashSet<String> updatedCategories = new HashSet<>(api.getCategories());
        updatedCategories.retainAll(existingCategoryIds);
        api.setCategories(updatedCategories);
    }

    private boolean hasOrphanCategories(Api api, Set<String> existingCategoryIds) {
        if (CollectionUtils.isEmpty(api.getCategories())) {
            return false;
        }
        HashSet<String> orphanCategories = new HashSet<>(api.getCategories());
        orphanCategories.removeAll(existingCategoryIds);
        return !orphanCategories.isEmpty();
    }

    private Set<String> getExistingCategoryIds() throws TechnicalException {
        return categoryRepository.findAll().stream().map(Category::getId).collect(Collectors.toSet());
    }

    private void setExecutionStatus(InstallationEntity installation, UpgradeStatus status) {
        installation.getAdditionalInformation().put(InstallationService.ORPHAN_CATEGORY_UPGRADER_STATUS, status.toString());
        installationService.setAdditionalInformation(installation.getAdditionalInformation());
    }

    private boolean isStatus(InstallationEntity installation, UpgradeStatus status) {
        return status.toString().equals(installation.getAdditionalInformation().get(InstallationService.ORPHAN_CATEGORY_UPGRADER_STATUS));
    }
}
