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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.PortalCrudServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.portal.domain_service.PortalLogoDomainService;
import io.gravitee.apim.core.portal.exception.PortalLogoNotFoundException;
import io.gravitee.apim.core.portal.exception.PortalNotFoundException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalLogo;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalLogoUseCaseTest {

    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final byte[] PNG_BYTES = { (byte) 0x89, 'P', 'N', 'G' };

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final ThemeQueryServiceInMemory themeQueryService = new ThemeQueryServiceInMemory();
    private GetPortalLogoUseCase cut;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        themeQueryService.reset();
        cut = new GetPortalLogoUseCase(portalCrudService, new PortalLogoDomainService(themeQueryService));
    }

    @Test
    void should_return_the_inline_logo_with_its_media_type() {
        var portal = givenPortal(ENV_ID);
        givenNextGenThemeLogo("data:image/png;base64," + Base64.getEncoder().encodeToString(PNG_BYTES));

        var logo = cut.execute(new GetPortalLogoUseCase.Input(ENV_ID, portal.getId())).logo();

        assertThat(logo).isInstanceOfSatisfying(PortalLogo.Inline.class, inline -> {
            assertThat(inline.mediaType()).isEqualTo("image/png");
            assertThat(inline.content()).isEqualTo(PNG_BYTES);
        });
    }

    @Test
    void should_return_an_external_logo_url() {
        var portal = givenPortal(ENV_ID);
        givenNextGenThemeLogo("https://cdn.acme.io/logo.png");

        var logo = cut.execute(new GetPortalLogoUseCase.Input(ENV_ID, portal.getId())).logo();

        assertThat(logo).isEqualTo(new PortalLogo.External(URI.create("https://cdn.acme.io/logo.png")));
    }

    @Test
    void should_throw_when_the_theme_has_no_logo() {
        var portal = givenPortal(ENV_ID);

        assertThatThrownBy(() -> cut.execute(new GetPortalLogoUseCase.Input(ENV_ID, portal.getId()))).isInstanceOf(
            PortalLogoNotFoundException.class
        );
    }

    @Test
    void should_throw_when_the_portal_belongs_to_another_environment() {
        var portal = givenPortal(OTHER_ENV_ID);
        givenNextGenThemeLogo("data:image/png;base64," + Base64.getEncoder().encodeToString(PNG_BYTES));

        assertThatThrownBy(() -> cut.execute(new GetPortalLogoUseCase.Input(ENV_ID, portal.getId()))).isInstanceOf(
            PortalNotFoundException.class
        );
    }

    private void givenNextGenThemeLogo(String logo) {
        themeQueryService.initWith(
            List.of(
                Theme.builder()
                    .id("theme-next")
                    .type(ThemeType.PORTAL_NEXT)
                    .referenceType(Theme.ReferenceType.ENVIRONMENT)
                    .referenceId(ENV_ID)
                    .enabled(true)
                    .logo(logo)
                    .build()
            )
        );
    }

    @SneakyThrows
    private Portal givenPortal(String environmentId) {
        var portal = Portal.of(
            PortalId.random(),
            environmentId,
            "org-1",
            "Portal",
            PortalNavigationStructure.empty(),
            null,
            true,
            URI.create("https://portal.acme.io").toURL()
        );
        portalCrudService.initWith(List.of(portal));
        return portal;
    }
}
