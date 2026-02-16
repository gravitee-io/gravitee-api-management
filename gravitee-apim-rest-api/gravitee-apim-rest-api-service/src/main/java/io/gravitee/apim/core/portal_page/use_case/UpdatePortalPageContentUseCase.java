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
import io.gravitee.apim.core.portal_page.domain_service.PortalPageContentValidator;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class UpdatePortalPageContentUseCase {

    private final PortalPageContentQueryService portalPageContentQueryService;
    private final PortalPageContentCrudService portalPageContentCrudService;
    private final List<PortalPageContentValidator> contentValidators;

    public Output execute(Input input) {
        // Check if portal page content is existing
        PortalPageContent existingContent = portalPageContentQueryService
            .findById(PortalPageContentId.of(input.portalPageContentId()))
            .orElseThrow(() -> new PageContentNotFoundException(input.portalPageContentId()));

        // Check if org id and env id are appropriate
        if (
            !existingContent.getOrganizationId().equals(input.organizationId()) ||
            !existingContent.getEnvironmentId().equals(input.environmentId())
        ) {
            throw new PageContentNotFoundException(input.portalPageContentId());
        }

        existingContent.update(input.updatePortalPageContent(), contentValidators);

        return new Output(portalPageContentCrudService.update(existingContent));
    }

    @Builder
    public record Input(
        String organizationId,
        String environmentId,
        String portalPageContentId,
        UpdatePortalPageContent updatePortalPageContent
    ) {}

    public record Output(PortalPageContent portalPageContent) {}
}
