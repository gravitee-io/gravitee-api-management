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
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.OpenApiConfiguration;
import io.gravitee.apim.core.portal_page.model.OpenApiPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class UpdatePortalPageContentConfigurationUseCase {

    private final PortalPageContentQueryService portalPageContentQueryService;
    private final PortalPageContentCrudService portalPageContentCrudService;

    public Output execute(Input input) {
        PortalPageContent<?> existingContent = portalPageContentQueryService
            .findById(PortalPageContentId.of(input.portalPageContentId()))
            .orElseThrow(() -> new PageContentNotFoundException(input.portalPageContentId()));

        if (
            !existingContent.getOrganizationId().equals(input.organizationId()) ||
            !existingContent.getEnvironmentId().equals(input.environmentId()) ||
            !(existingContent instanceof OpenApiPageContent)
        ) {
            throw new PageContentNotFoundException(input.portalPageContentId());
        }

        OpenApiPageContent openApiContent = (OpenApiPageContent) existingContent;
        openApiContent.updateViewerSettings(input.configuration());

        portalPageContentCrudService.update(openApiContent);
        return new Output(openApiContent);
    }

    @Builder
    public record Input(String organizationId, String environmentId, String portalPageContentId, OpenApiConfiguration configuration) {}

    public record Output(OpenApiPageContent portalPageContent) {}
}
