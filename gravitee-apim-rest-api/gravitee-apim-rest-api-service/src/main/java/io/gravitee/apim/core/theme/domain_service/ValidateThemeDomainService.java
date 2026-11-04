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
package io.gravitee.apim.core.theme.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.exception.ThemeDefinitionInvalidException;
import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import io.gravitee.apim.core.theme.exception.ThemeTypeInvalidException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeSearchCriteria;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.model.UpdateTheme;
import io.gravitee.apim.core.theme.query_service.ThemeQueryService;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.ZonedDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ValidateThemeDomainService {

    private final ThemeCrudService themeCrudService;

    public void validateUpdateTheme(UpdateTheme updateTheme, ExecutionContext executionContext) {
        var existingTheme = this.themeCrudService.get(updateTheme.getId());

        if (
            !Objects.equals(existingTheme.getReferenceId(), executionContext.getEnvironmentId()) &&
            Theme.ReferenceType.ENVIRONMENT.equals(existingTheme.getReferenceType())
        ) {
            throw new ThemeNotFoundException(updateTheme.getId());
        }

        if (!Objects.equals(existingTheme.getType(), updateTheme.getType())) {
            throw new ThemeTypeInvalidException(updateTheme.getType().name());
        }

        if (ThemeType.PORTAL_NEXT.equals(updateTheme.getType()) && Objects.isNull(updateTheme.getDefinitionPortalNext())) {
            throw new ThemeDefinitionInvalidException(updateTheme.getType().name(), null);
        }

        if (ThemeType.PORTAL.equals(updateTheme.getType()) && Objects.isNull(updateTheme.getDefinitionPortal())) {
            throw new ThemeDefinitionInvalidException(updateTheme.getType().name(), null);
        }
    }
}
