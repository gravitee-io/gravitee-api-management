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
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.domain_service.ApiOwnedNavigationDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemValidatorService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationFolder;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Lists an API in the portal and publishes its documentation with it. The documentation is not moved:
 * it stays stored against the API and is shown under the new navigation entry when the tree is read.
 */
@UseCase
@RequiredArgsConstructor
public class PublishApiToPortalUseCase {

    private final ApiOwnedNavigationDomainService apiOwnedNavigationDomainService;
    private final PortalNavigationItemValidatorService validatorService;
    private final PortalNavigationItemDomainService domainService;

    public Output execute(Input input) {
        if (input.sectionId() == null) {
            throw InvalidPortalNavigationItemDataException.fieldIsEmpty("sectionId");
        }
        var section = apiOwnedNavigationDomainService.requirePublishLocation(input.environmentId(), input.sectionId());

        var listingToCreate = CreatePortalNavigationItem.builder()
            .id(PortalNavigationItemId.random())
            .type(PortalNavigationItemType.API)
            .apiId(input.apiId())
            .area(PortalArea.TOP_NAVBAR)
            .parentId(section.getId())
            .published(true)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .build();
        // ApiItemCreateRule rejects an API that is already listed, including by a listing the portal editor unpublished
        validatorService.validateAll(List.of(listingToCreate), input.environmentId());

        var listing = (PortalNavigationApi) domainService.create(input.organizationId(), input.environmentId(), listingToCreate);
        apiOwnedNavigationDomainService.setPublished(input.environmentId(), input.apiId(), true);

        return new Output(listing, section);
    }

    public record Input(String organizationId, String environmentId, String apiId, PortalNavigationItemId sectionId) {}

    public record Output(PortalNavigationApi listing, PortalNavigationFolder section) {}
}
