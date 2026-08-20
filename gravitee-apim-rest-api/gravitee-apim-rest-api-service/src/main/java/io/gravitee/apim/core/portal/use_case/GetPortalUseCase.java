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
package io.gravitee.apim.core.portal.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeAutomationMetadata;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalUseCase {

    private final PortalCrudService portalCrudService;
    private final ThemeCrudService themeCrudService;

    public record Input(AuditInfo auditInfo, PortalId portalId) {}

    public record Output(Portal portal, PortalNavigationStructure structure, String activeThemeHrid) {
        public Output(Portal portal, PortalNavigationStructure structure) {
            this(portal, structure, null);
        }
    }

    public Output execute(Input input) {
        var portal = portalCrudService
            .findByIdAndEnvironmentId(input.portalId(), input.auditInfo().environmentId())
            .orElseThrow(() -> new PortalNotFoundException(input.portalId().toString()));
        var activeThemeHrid = resolveActiveThemeHrid(portal.getActiveThemeId(), input.auditInfo().environmentId());
        return new Output(portal, portal.getNavigationStructure(), activeThemeHrid);
    }

    private String resolveActiveThemeHrid(String activeThemeId, String environmentId) {
        if (activeThemeId == null) {
            return null;
        }
        return themeCrudService
            .findByIdAndEnvironmentId(activeThemeId, environmentId)
            .map(Theme::getAutomationMetadata)
            .map(ThemeAutomationMetadata::hrid)
            .orElse(null);
    }
}
