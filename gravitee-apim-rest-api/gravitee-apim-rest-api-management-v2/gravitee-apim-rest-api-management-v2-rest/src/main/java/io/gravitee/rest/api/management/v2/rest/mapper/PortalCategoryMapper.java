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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.apim.core.portal_category.model.CreatePortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.apim.core.portal_category.model.UpdatePortalCategory;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author GraviteeSource Team
 */
@Mapper
public interface PortalCategoryMapper {
    PortalCategoryMapper INSTANCE = Mappers.getMapper(PortalCategoryMapper.class);

    io.gravitee.rest.api.management.v2.rest.model.PortalCategory map(PortalCategory portalCategory);

    List<io.gravitee.rest.api.management.v2.rest.model.PortalCategory> mapList(List<PortalCategory> portalCategories);

    CreatePortalCategory map(io.gravitee.rest.api.management.v2.rest.model.CreatePortalCategory createPortalCategory);

    UpdatePortalCategory map(io.gravitee.rest.api.management.v2.rest.model.UpdatePortalCategory updatePortalCategory);

    default UUID map(PortalCategoryId id) {
        return id == null ? null : id.id();
    }
}
