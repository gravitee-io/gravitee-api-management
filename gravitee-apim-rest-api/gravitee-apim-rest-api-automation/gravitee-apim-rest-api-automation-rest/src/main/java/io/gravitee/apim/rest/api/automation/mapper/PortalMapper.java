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
package io.gravitee.apim.rest.api.automation.mapper;

import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.rest.api.automation.model.PortalState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * @author GraviteeSource Team
 */
@Mapper
public interface PortalMapper {
    PortalMapper INSTANCE = Mappers.getMapper(PortalMapper.class);

    default PortalState toPortalState(Portal portal, String hrid) {
        var state = new PortalState(
            portal.getId() != null ? portal.getId().toString() : null,
            portal.getEnvironmentId(),
            portal.getOrganizationId(),
            null
        );
        state.setHrid(hrid);
        mapPortalToState(portal, state);
        return state;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "errors", ignore = true)
    @Mapping(target = "hrid", ignore = true)
    @Mapping(target = "navigation", ignore = true)
    void mapPortalToState(Portal portal, @MappingTarget PortalState state);
}
