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

import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalSummary;
import io.gravitee.apim.core.portal.use_case.GetPortalsStatsUseCase;
import io.gravitee.rest.api.management.v2.rest.model.Portal;
import io.gravitee.rest.api.management.v2.rest.model.PortalQuota;
import io.gravitee.rest.api.management.v2.rest.model.PortalsStats;
import io.gravitee.rest.api.management.v2.rest.utils.ManagementApiLinkHelper;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PortalMapper {
    PortalMapper INSTANCE = Mappers.getMapper(PortalMapper.class);

    default Portal map(PortalSummary summary, UriInfo uriInfo) {
        var portal = summary.portal();
        return new Portal()
            .id(portal.getId().id())
            .name(portal.getName())
            .url(portal.getUrl().toString())
            ._default(portal.isDefault())
            .logoUrl(summary.hasLogo() ? ManagementApiLinkHelper.portalLogoURL(uriInfo.getBaseUriBuilder(), portal.getId()) : null)
            .publishedApisCount(summary.publishedApisCount());
    }

    default List<Portal> map(List<PortalSummary> summaries, UriInfo uriInfo) {
        return summaries
            .stream()
            .map(summary -> map(summary, uriInfo))
            .toList();
    }

    default PortalsStats map(GetPortalsStatsUseCase.Output stats) {
        return new PortalsStats()
            .quota(new PortalQuota().used(stats.quota().used()).limit(stats.quota().limit()))
            .publishedApisCount(stats.publishedApisCount());
    }

    default PortalId toPortalId(String portalId) {
        try {
            return PortalId.of(portalId);
        } catch (IllegalArgumentException e) {
            // No portal can have an id that is not a UUID.
            throw new PortalNotFoundException(portalId);
        }
    }
}
