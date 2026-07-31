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
package io.gravitee.apim.infra.crud_service.portal_category;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_category.crud_service.PortalCategoryCrudService;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.infra.adapter.PortalCategoryAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalCategoryRepository;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
public class PortalCategoryCrudServiceImpl implements PortalCategoryCrudService {

    private final PortalCategoryRepository portalCategoryRepository;

    public PortalCategoryCrudServiceImpl(@Lazy PortalCategoryRepository portalCategoryRepository) {
        this.portalCategoryRepository = portalCategoryRepository;
    }

    @Override
    public PortalCategory create(PortalCategory portalCategory) {
        try {
            var repositoryModel = PortalCategoryAdapter.INSTANCE.toRepository(portalCategory);
            var created = portalCategoryRepository.create(repositoryModel);
            return PortalCategoryAdapter.INSTANCE.toModel(created);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to create a portal category", e);
        }
    }

    @Override
    public PortalCategory update(PortalCategory portalCategory) {
        try {
            var repositoryModel = PortalCategoryAdapter.INSTANCE.toRepository(portalCategory);
            var updated = portalCategoryRepository.update(repositoryModel);
            return PortalCategoryAdapter.INSTANCE.toModel(updated);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to update portal category: " + portalCategory.getId(), e);
        }
    }

    @Override
    public void delete(PortalCategoryId id) {
        try {
            portalCategoryRepository.delete(id.toString());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to delete portal category: " + id, e);
        }
    }

    @Override
    public Optional<PortalCategory> get(PortalCategoryId id) {
        try {
            return portalCategoryRepository.findById(id.toString()).map(PortalCategoryAdapter.INSTANCE::toModel);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to find portal category: " + id, e);
        }
    }
}
