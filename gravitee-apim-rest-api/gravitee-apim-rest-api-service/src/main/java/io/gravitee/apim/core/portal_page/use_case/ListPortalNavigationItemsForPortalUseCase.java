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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemReadDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ListPortalNavigationItemsForPortalUseCase {

    private final PortalNavigationItemReadDomainService reader;

    public Output execute(Input input) {
        var items = reader.readForPortal(
            input.organizationId(),
            input.environmentId(),
            input.portalId(),
            input.portalArea(),
            input.parentId(),
            input.loadChildren()
        );
        var apis = input.includeApis()
            ? reader.resolveApis(input.organizationId(), input.environmentId(), items)
            : Map.<PortalNavigationItemId, Api>of();
        return new Output(items, apis);
    }

    public record Input(
        String organizationId,
        String environmentId,
        PortalId portalId,
        PortalArea portalArea,
        Optional<PortalNavigationItemId> parentId,
        boolean loadChildren,
        boolean includeApis
    ) {
        public Input {
            Objects.requireNonNull(portalId, "portalId");
        }
    }

    public record Output(List<PortalNavigationItem> items, Map<PortalNavigationItemId, Api> apis) {}
}
