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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author GraviteeSource Team
 */
@Mapper
public interface PortalCategoryAdapter {
    PortalCategoryAdapter INSTANCE = Mappers.getMapper(PortalCategoryAdapter.class);

    default PortalCategory toModel(io.gravitee.repository.management.model.PortalCategory repository) {
        if (repository == null) {
            return null;
        }
        return PortalCategory.of(
            PortalCategoryId.of(repository.getId()),
            repository.getEnvironmentId(),
            repository.getTitle(),
            repository.getDescription(),
            repository.isVisible()
        );
    }

    default io.gravitee.repository.management.model.PortalCategory toRepository(PortalCategory domain) {
        if (domain == null) {
            return null;
        }
        return io.gravitee.repository.management.model.PortalCategory.builder()
            .id(domain.getId().toString())
            .environmentId(domain.getEnvironmentId())
            .title(domain.getTitle())
            .description(domain.getDescription())
            .visible(domain.isVisible())
            .build();
    }
}
