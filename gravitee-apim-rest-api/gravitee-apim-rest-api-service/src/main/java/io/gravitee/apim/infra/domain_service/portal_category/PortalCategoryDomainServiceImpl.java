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
package io.gravitee.apim.infra.domain_service.portal_category;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.portal_category.crud_service.PortalCategoryCrudService;
import io.gravitee.apim.core.portal_category.domain_service.PortalCategoryDomainService;
import io.gravitee.apim.core.portal_category.exception.PortalCategoryNotFoundException;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
@RequiredArgsConstructor
public class PortalCategoryDomainServiceImpl implements PortalCategoryDomainService {

    private final PortalCategoryCrudService portalCategoryCrudService;

    @Override
    public void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationDomainException("Portal category title cannot be blank");
        }
    }

    @Override
    public PortalCategory findByIdAndEnvironmentId(String environmentId, PortalCategoryId id) {
        return portalCategoryCrudService
            .get(id)
            .filter(portalCategory -> environmentId.equals(portalCategory.getEnvironmentId()))
            .orElseThrow(() -> new PortalCategoryNotFoundException(id.toString()));
    }
}
