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
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationSourcedItemsDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalPageContentValidatorService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.Objects;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class UpdatePortalPageContentUseCase {

    private final PortalNavigationAccessDomainService accessService;

    private final PortalPageContentCrudService portalPageContentCrudService;
    private final PortalPageContentValidatorService validatorService;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationSourcedItemsDomainService sourcedItemsDomainService;

    public Output execute(Input input) {
        var scope = input.scope();
        var existingContent = accessService.requireContent(
            input.organizationId(),
            input.environmentId(),
            scope,
            PortalPageContentId.of(input.portalPageContentId())
        );
        return updateContent(input, existingContent);
    }

    private Output updateContent(Input input, PortalPageContent<?> existingContent) {
        boolean managedByFetcher = portalNavigationItemsQueryService
            .findNavigationPageByPortalPageContentId(input.environmentId(), existingContent.getId())
            .filter(page -> isSourced(page, input.environmentId()))
            .isPresent();
        if (managedByFetcher) {
            throw InvalidPortalNavigationItemDataException.sourcedPageContentIsReadOnly(input.portalPageContentId());
        }

        PortalPageContent<?> targetContent = withRequestedType(existingContent, input.updatePortalPageContent());

        validatorService.validateContentSize(input.updatePortalPageContent().getContent());
        validatorService.validateForUpdate(targetContent, input.updatePortalPageContent());

        targetContent.update(input.updatePortalPageContent());

        return new Output(portalPageContentCrudService.update(targetContent));
    }

    /** File import can change the content type: rebuild the content under its new type, keeping the same id. */
    private PortalPageContent<?> withRequestedType(PortalPageContent<?> existingContent, UpdatePortalPageContent update) {
        var requestedType = update.getType();
        if (requestedType == null || requestedType == existingContent.getType()) {
            return existingContent;
        }
        return PortalPageContent.of(
            requestedType,
            existingContent.getId(),
            existingContent.getOrganizationId(),
            existingContent.getEnvironmentId(),
            update.getContent(),
            existingContent.getAutomationMetadata()
        );
    }

    private boolean isSourced(PortalNavigationPage page, String environmentId) {
        return page.getSource() != null || sourcedItemsDomainService.findSourcedAncestor(environmentId, page).isPresent();
    }

    @Builder
    public record Input(
        String organizationId,
        String environmentId,
        String portalPageContentId,
        UpdatePortalPageContent updatePortalPageContent,
        NavigationItemReference scope
    ) {
        public Input {
            Objects.requireNonNull(scope, "scope");
        }
    }

    public record Output(PortalPageContent<?> portalPageContent) {}
}
