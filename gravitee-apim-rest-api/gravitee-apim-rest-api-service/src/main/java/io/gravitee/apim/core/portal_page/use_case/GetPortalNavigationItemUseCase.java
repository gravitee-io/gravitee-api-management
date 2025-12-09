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
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalNavigationItemUseCase {

    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;

    public Output execute(Input input) {
        final PortalNavigationItem foundItem = Optional.ofNullable(
            portalNavigationItemsQueryService.findByIdAndEnvironmentId(input.environmentId(), input.portalNavigationItemId())
        ).orElseThrow(() -> new PortalNavigationItemNotFoundException(input.portalNavigationItemId().id().toString()));

        input.viewerContext().validateAccess(foundItem);

        return new Output(foundItem);
    }

    public record Input(
        PortalNavigationItemId portalNavigationItemId,
        String environmentId,
        PortalNavigationItemViewerContext viewerContext
    ) {}

    public record Output(PortalNavigationItem portalNavigationItem) {}
}
