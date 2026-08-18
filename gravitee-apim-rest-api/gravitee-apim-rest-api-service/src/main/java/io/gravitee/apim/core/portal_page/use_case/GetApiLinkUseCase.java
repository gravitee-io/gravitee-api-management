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
import io.gravitee.apim.core.portal_page.exception.PortalLinkNotFoundException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApiLinkUseCase {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;

    public record Input(AuditInfo auditInfo, PortalNavigationItemId linkId) {}

    public record Output(PortalNavigationLink link) {}

    public Output execute(Input input) {
        var item = navigationItemsQueryService.findByIdAndEnvironmentId(input.auditInfo().environmentId(), input.linkId());
        if (!(item instanceof PortalNavigationLink link) || !isApiAttached(link)) {
            throw new PortalLinkNotFoundException(input.linkId().toString());
        }
        return new Output(link);
    }

    // Scopes the read to API-attached links so an /apis/{apiHrid}/links path cannot reach a
    // portal-attached one, mirroring GetApiDocumentationUseCase's referenceType filter.
    private static boolean isApiAttached(PortalNavigationLink link) {
        return link.getAutomationMetadata() != null && link.getAutomationMetadata().referenceType() == AutomationMetadata.ReferenceType.API;
    }
}
