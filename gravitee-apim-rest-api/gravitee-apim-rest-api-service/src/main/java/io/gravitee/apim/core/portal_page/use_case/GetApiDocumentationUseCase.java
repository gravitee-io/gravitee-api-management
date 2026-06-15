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
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal_page.exception.ApiDocumentationNotFoundException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApiDocumentationUseCase {

    private final PortalPageContentQueryService portalPageContentQueryService;

    public record Input(AuditInfo auditInfo, PortalPageContentId portalPageContentId) {}

    public record Output(PortalPageContent<?> pageContent) {}

    public Output execute(Input input) {
        var pageContent = portalPageContentQueryService
            .findById(input.portalPageContentId())
            .filter(pc -> pc.getAutomationMetadata() != null)
            .filter(pc -> pc.getAutomationMetadata().referenceType() == AutomationMetadata.ReferenceType.API)
            .orElseThrow(() -> new ApiDocumentationNotFoundException(input.portalPageContentId().toString()));
        return new Output(pageContent);
    }
}
