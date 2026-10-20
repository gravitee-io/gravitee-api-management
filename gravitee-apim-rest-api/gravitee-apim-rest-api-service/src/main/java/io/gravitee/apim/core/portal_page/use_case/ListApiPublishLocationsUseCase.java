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
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Lists where an API can be published. Callers hold a permission on one API, not on the portal, so the
 * output is deliberately limited to what is needed to pick a section and never exposes the portal tree.
 */
@UseCase
@RequiredArgsConstructor
public class ListApiPublishLocationsUseCase {

    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;

    public Output execute(Input input) {
        var locations = apiOwnedNavigationDomainService
            .findPublishLocations(input.environmentId())
            .stream()
            .map(section -> new PublishLocation(section.getId(), section.getTitle()))
            .toList();
        return new Output(locations);
    }

    public record Input(String environmentId) {}

    public record Output(List<PublishLocation> locations) {}

    public record PublishLocation(PortalNavigationItemId id, String name) {}
}
