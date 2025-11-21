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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdatePortalNavigationItemUseCase {

    private final PortalNavigationItemCrudService portalNavigationItemCrudService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    public Output execute(Input input) {
        var navItem = input.updatePortalNavigationItem;
        PortalNavigationItem item = portalNavigationItemsQueryService.findByIdAndEnvironmentId(input.environmentId(), navItem.getId());
        if (item == null) {
            throw new InvalidDataException("Portal navigation item [%s] not found.".formatted(navItem.getId()));
        }
        item.update(navItem);
        return new Output(portalNavigationItemCrudService.update(item));
    }

    @Builder
    public record Input(String organizationId, String environmentId, CreatePortalNavigationItem updatePortalNavigationItem) {}

    public record Output(PortalNavigationItem updatedItem) {}
}
