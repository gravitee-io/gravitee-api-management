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
package io.gravitee.apim.core.theme.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.exception.PortalThemeInUseException;
import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeletePortalThemeUseCase {

    private final ThemeCrudService themeCrudService;
    private final PortalCrudService portalCrudService;

    public record Input(AuditInfo auditInfo, String themeId) {}

    public void execute(Input input) {
        var envId = input.auditInfo().environmentId();
        var theme = themeCrudService
            .findByIdAndEnvironmentId(input.themeId(), envId)
            .orElseThrow(() -> new ThemeNotFoundException(input.themeId()));

        var referencingPortalIds = portalCrudService
            .findByActiveThemeIdAndEnvironmentId(theme.getId(), envId)
            .stream()
            .map(Portal::getId)
            .map(Object::toString)
            .toList();

        if (!referencingPortalIds.isEmpty()) {
            throw new PortalThemeInUseException(theme.getId(), referencingPortalIds);
        }

        themeCrudService.delete(theme.getId());
    }
}
