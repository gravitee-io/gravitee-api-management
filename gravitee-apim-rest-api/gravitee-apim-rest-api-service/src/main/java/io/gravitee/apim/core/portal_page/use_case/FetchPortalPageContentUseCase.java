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
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemSourceDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class FetchPortalPageContentUseCase {

    private final PortalNavigationItemsQueryService queryService;
    private final PortalNavigationItemDomainService domainService;
    private final PortalNavigationItemSourceDomainService sourceDomainService;

    public Output execute(Input input) {
        var item = queryService.findByIdAndEnvironmentId(input.environmentId(), PortalNavigationItemId.of(input.navigationItemId()));
        if (item == null) {
            throw new PortalNavigationItemNotFoundException(input.navigationItemId());
        }
        if (!(item instanceof PortalNavigationPage page) || page.getSource() == null) {
            throw InvalidPortalNavigationItemDataException.noSourceConfigured(input.navigationItemId());
        }

        var updatedItem = domainService.fetchPageContent(page);
        if (updatedItem.getSource() != null) {
            sourceDomainService.removeSensitiveData(updatedItem.getSource());
        }
        return new Output(updatedItem);
    }

    @Builder
    public record Input(String environmentId, String navigationItemId) {}

    public record Output(PortalNavigationItem updatedItem) {}
}
