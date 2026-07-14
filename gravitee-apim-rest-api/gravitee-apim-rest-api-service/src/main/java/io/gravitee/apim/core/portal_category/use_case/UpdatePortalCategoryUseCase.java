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
package io.gravitee.apim.core.portal_category.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_category.crud_service.PortalCategoryCrudService;
import io.gravitee.apim.core.portal_category.domain_service.PortalCategoryDomainService;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_category.model.UpdatePortalCategory;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@UseCase
@RequiredArgsConstructor
public class UpdatePortalCategoryUseCase {

    private final PortalCategoryDomainService portalCategoryDomainService;
    private final PortalCategoryCrudService portalCategoryCrudService;

    public Output execute(Input input) {
        var updatePortalCategory = input.updatePortalCategory();
        portalCategoryDomainService.validateTitle(updatePortalCategory.getTitle());

        var existing = portalCategoryDomainService.findByIdAndEnvironmentId(input.environmentId(), input.portalCategoryId());
        existing.update(updatePortalCategory);

        var updated = portalCategoryCrudService.update(existing);
        return new Output(updated);
    }

    public record Input(String environmentId, PortalCategoryId portalCategoryId, UpdatePortalCategory updatePortalCategory) {}

    public record Output(PortalCategory portalCategory) {}
}
