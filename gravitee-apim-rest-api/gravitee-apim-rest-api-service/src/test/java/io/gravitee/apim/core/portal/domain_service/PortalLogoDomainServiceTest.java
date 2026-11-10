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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalLogoDomainServiceTest {

    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final String LOGO = "data:image/png;base64,AAA";

    private final ThemeQueryServiceInMemory themeQueryService = new ThemeQueryServiceInMemory();
    private PortalLogoDomainService cut;

    @BeforeEach
    void setUp() {
        themeQueryService.reset();
        cut = new PortalLogoDomainService(themeQueryService);
    }

    @Test
    void should_return_the_logo_of_the_enabled_next_gen_theme() {
        themeQueryService.initWith(List.of(theme(ThemeType.PORTAL_NEXT, ENV_ID, true, LOGO)));

        assertThat(cut.findLogo(ENV_ID)).contains(LOGO);
    }

    @Test
    void should_find_no_logo_when_the_next_gen_theme_is_disabled() {
        themeQueryService.initWith(List.of(theme(ThemeType.PORTAL_NEXT, ENV_ID, false, LOGO)));

        assertThat(cut.findLogo(ENV_ID)).isEmpty();
    }

    @Test
    void should_find_no_logo_when_the_enabled_next_gen_theme_has_none() {
        themeQueryService.initWith(
            List.of(theme(ThemeType.PORTAL_NEXT, ENV_ID, true, " "), theme(ThemeType.PORTAL_NEXT, OTHER_ENV_ID, true, null))
        );

        assertThat(cut.findLogo(ENV_ID)).isEmpty();
        assertThat(cut.findLogo(OTHER_ENV_ID)).isEmpty();
    }

    @Test
    void should_ignore_the_classic_portal_theme_and_other_environments() {
        themeQueryService.initWith(
            List.of(theme(ThemeType.PORTAL, ENV_ID, true, LOGO), theme(ThemeType.PORTAL_NEXT, OTHER_ENV_ID, true, LOGO))
        );

        assertThat(cut.findLogo(ENV_ID)).isEmpty();
    }

    private static Theme theme(ThemeType type, String environmentId, boolean enabled, String logo) {
        return Theme.builder()
            .id(type + "-" + environmentId + "-" + enabled)
            .type(type)
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .referenceId(environmentId)
            .enabled(enabled)
            .logo(logo)
            .build();
    }
}
