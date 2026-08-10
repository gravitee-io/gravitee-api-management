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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalCategoryMapper {
    PortalCategoryMapper INSTANCE = Mappers.getMapper(PortalCategoryMapper.class);

    @Mapping(target = "id", source = "id")
    io.gravitee.rest.api.portal.rest.model.PortalCategory map(PortalCategory portalCategory);

    List<io.gravitee.rest.api.portal.rest.model.PortalCategory> map(List<PortalCategory> portalCategories);

    default String mapId(PortalCategoryId id) {
        return id != null ? id.toString() : null;
    }
}
