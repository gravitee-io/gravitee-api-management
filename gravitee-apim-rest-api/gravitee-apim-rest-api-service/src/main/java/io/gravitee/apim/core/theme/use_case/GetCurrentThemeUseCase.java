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
import io.gravitee.apim.core.portal.crud_service.PortalCrudService;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.domain_service.DefaultThemeDomainService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.query_service.ThemeQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetCurrentThemeUseCase {

    private final ThemeQueryService themeQueryService;
    private final DefaultThemeDomainService defaultThemeDomainService;
    private final PortalCrudService portalCrudService;
    private final ThemeCrudService themeCrudService;

    public Output execute(Input input) {
        var environmentId = input.executionContext().getEnvironmentId();
        var theme = resolveByPortal(input.portalId(), environmentId).orElseGet(() ->
            resolveEnabledOrCreate(input.type(), environmentId, input.executionContext())
        );
        return new Output(theme);
    }

    private Optional<Theme> resolveByPortal(PortalId portalId, String environmentId) {
        if (portalId == null) {
            return Optional.empty();
        }
        return portalCrudService
            .findByIdAndEnvironmentId(portalId, environmentId)
            .map(Portal::getActiveThemeId)
            .flatMap(themeId -> themeCrudService.findByIdAndEnvironmentId(themeId, environmentId));
    }

    private Theme resolveEnabledOrCreate(ThemeType type, String environmentId, ExecutionContext executionContext) {
        return themeQueryService
            .findByThemeTypeAndEnvironmentId(type, environmentId)
            .stream()
            .filter(theme -> Objects.equals(true, theme.isEnabled()))
            .findFirst()
            .orElseGet(() -> defaultThemeDomainService.createAndEnableDefaultTheme(type, executionContext));
    }

    @Builder
    public record Input(ThemeType type, ExecutionContext executionContext, PortalId portalId) {
        public Input(ThemeType type, ExecutionContext executionContext) {
            this(type, executionContext, null);
        }
    }

    public record Output(Theme result) {}
}
