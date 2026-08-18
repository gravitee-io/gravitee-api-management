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
import io.gravitee.apim.core.portal_page.domain_service.PortalLinkSyncDomainService;
import io.gravitee.apim.core.portal_page.exception.PortalLinkNotFoundException;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationLink;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteApiLinkUseCase {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalLinkSyncDomainService syncDomainService;

    public record Input(AuditInfo auditInfo, PortalNavigationItemId linkId) {}

    public void execute(Input input) {
        var item = navigationItemsQueryService.findByIdAndEnvironmentId(input.auditInfo().environmentId(), input.linkId());
        if (
            !(item instanceof PortalNavigationLink link) ||
            link.getAutomationMetadata() == null ||
            link.getAutomationMetadata().referenceType() != AutomationMetadata.ReferenceType.API
        ) {
            throw new PortalLinkNotFoundException(input.linkId().toString());
        }
        syncDomainService.dematerialize(input.auditInfo(), input.linkId());
    }
}
