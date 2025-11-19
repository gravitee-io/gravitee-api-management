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
package io.gravitee.apim.infra.query_service.portal_page;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.infra.adapter.PortalNavigationItemAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalNavigationItemRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PortalNavigationItemsQueryServiceImpl implements PortalNavigationItemsQueryService {

    private final PortalNavigationItemRepository portalNavigationItemRepository;
    private final PortalNavigationItemAdapter portalNavigationItemAdapter = PortalNavigationItemAdapter.INSTANCE;

    public PortalNavigationItemsQueryServiceImpl(@Lazy final PortalNavigationItemRepository portalNavigationItemRepository) {
        this.portalNavigationItemRepository = portalNavigationItemRepository;
    }

    @Override
    public PortalNavigationItem findByIdAndEnvironmentId(String environmentId, PortalNavigationItemId id) {
        try {
            var result = portalNavigationItemRepository.findById(id.json());
            if (result.isPresent() && result.get().getEnvironmentId().equals(environmentId)) {
                return portalNavigationItemAdapter.toEntity(result.get());
            }
            return null;
        } catch (TechnicalException e) {
            String errorMessage = String.format(
                "An error occurred while finding portal navigation item by id %s and environmentId %s",
                id,
                environmentId
            );
            throw new TechnicalDomainException(errorMessage, e);
        }
    }

    @Override
    public List<PortalNavigationItem> findByParentIdAndEnvironmentId(String environmentId, PortalNavigationItemId parentId) {
        try {
            var results = portalNavigationItemRepository.findAllByParentIdAndEnvironmentId(parentId.json(), environmentId);
            return results.stream().map(portalNavigationItemAdapter::toEntity).collect(Collectors.toList());
        } catch (TechnicalException e) {
            String errorMessage = String.format(
                "An error occurred while finding portal navigation items by parentId %s and environmentId %s",
                parentId,
                environmentId
            );
            throw new TechnicalDomainException(errorMessage, e);
        }
    }

    @Override
    public List<PortalNavigationItem> findTopLevelItemsByEnvironmentIdAndPortalArea(String environmentId, PortalArea portalArea) {
        try {
            var area = switch (portalArea) {
                case HOMEPAGE -> io.gravitee.repository.management.model.PortalNavigationItem.Area.HOMEPAGE;
                case TOP_NAVBAR -> io.gravitee.repository.management.model.PortalNavigationItem.Area.TOP_NAVBAR;
            };
            var results = portalNavigationItemRepository.findAllByAreaAndEnvironmentIdAndParentIdIsNull(area, environmentId);
            return results.stream().map(portalNavigationItemAdapter::toEntity).collect(Collectors.toList());
        } catch (TechnicalException e) {
            String errorMessage = String.format(
                "An error occurred while finding top level portal navigation items by environmentId %s and area %s",
                environmentId,
                portalArea
            );
            throw new TechnicalDomainException(errorMessage, e);
        }
    }
}
