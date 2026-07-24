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
package io.gravitee.apim.infra.query_service.portal_category;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.query_service.PortalCategoryQueryService;
import io.gravitee.apim.infra.adapter.PortalCategoryAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalCategoryRepository;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
public class PortalCategoryQueryServiceImpl implements PortalCategoryQueryService {

    private final PortalCategoryRepository portalCategoryRepository;

    public PortalCategoryQueryServiceImpl(@Lazy PortalCategoryRepository portalCategoryRepository) {
        this.portalCategoryRepository = portalCategoryRepository;
    }

    @Override
    public List<PortalCategory> findByEnvironmentId(String environmentId) {
        try {
            return portalCategoryRepository
                .findAllByEnvironmentId(environmentId)
                .stream()
                .map(PortalCategoryAdapter.INSTANCE::toModel)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                "An error occurred while trying to find portal categories for environment: " + environmentId,
                e
            );
        }
    }
}
