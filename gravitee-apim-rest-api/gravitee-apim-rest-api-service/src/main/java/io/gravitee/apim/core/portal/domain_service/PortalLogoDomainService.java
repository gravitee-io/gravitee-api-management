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
package io.gravitee.apim.core.portal.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.query_service.ThemeQueryService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class PortalLogoDomainService {

    private final ThemeQueryService themeQueryService;

    /**
     * The logo shown for every portal of {@code environmentId}, as the theme stores it.
     * Until PORTAL-319 gives each portal its own theme, it is the environment's enabled next-gen theme logo.
     */
    public Optional<String> findLogo(String environmentId) {
        return themeQueryService
            .findByThemeTypeAndEnvironmentId(ThemeType.PORTAL_NEXT, environmentId)
            .stream()
            .filter(Theme::isEnabled)
            .filter(Theme::hasLogo)
            .map(Theme::getLogo)
            .findFirst();
    }
}
