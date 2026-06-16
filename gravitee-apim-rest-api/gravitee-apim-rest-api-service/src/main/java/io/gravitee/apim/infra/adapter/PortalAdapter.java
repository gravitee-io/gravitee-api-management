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

import com.fasterxml.jackson.core.type.TypeReference;
import io.gravitee.apim.core.portal.model.NavigationPath;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.node.logging.NodeLoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;

@Mapper
public interface PortalAdapter {
    Logger log = NodeLoggerFactory.getLogger(PortalAdapter.class);
    PortalAdapter INSTANCE = Mappers.getMapper(PortalAdapter.class);

    default Portal toEntity(io.gravitee.repository.management.model.Portal portal) {
        if (portal == null) {
            return null;
        }
        return Portal.of(
            PortalId.of(portal.getId()),
            portal.getEnvironmentId(),
            portal.getOrganizationId(),
            portal.getName(),
            deserializePortalNavigation(portal.getPortalNavigation())
        );
    }

    default io.gravitee.repository.management.model.Portal toRepository(Portal portal) {
        if (portal == null) {
            return null;
        }
        return io.gravitee.repository.management.model.Portal.builder()
            .id(portal.getId().toString())
            .environmentId(portal.getEnvironmentId())
            .organizationId(portal.getOrganizationId())
            .name(portal.getName())
            .portalNavigation(serializePortalNavigation(portal.getPortalNavigation()))
            .build();
    }

    TypeReference<List<NavigationPathJson>> NAVIGATION_PATH_LIST = new TypeReference<>() {};

    record NavigationPathJson(String path, String displayName) {
        static NavigationPathJson from(NavigationPath p) {
            return new NavigationPathJson(p.path(), p.displayName().orElse(null));
        }

        NavigationPath toDomain() {
            return new NavigationPath(path, Optional.ofNullable(displayName));
        }
    }

    default String serializePortalNavigation(List<NavigationPath> portalNavigation) {
        if (portalNavigation == null || portalNavigation.isEmpty()) {
            return null;
        }
        try {
            return GraviteeJacksonMapper.getInstance().writeValueAsString(portalNavigation.stream().map(NavigationPathJson::from).toList());
        } catch (IOException ioe) {
            throw new RuntimeException("Unexpected error while serializing portal portalNavigation", ioe);
        }
    }

    default List<NavigationPath> deserializePortalNavigation(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return GraviteeJacksonMapper.getInstance()
                .readValue(json, NAVIGATION_PATH_LIST)
                .stream()
                .map(NavigationPathJson::toDomain)
                .toList();
        } catch (IOException ioe) {
            log.error("Unexpected error while deserializing portal portalNavigation", ioe);
            return List.of();
        }
    }
}
