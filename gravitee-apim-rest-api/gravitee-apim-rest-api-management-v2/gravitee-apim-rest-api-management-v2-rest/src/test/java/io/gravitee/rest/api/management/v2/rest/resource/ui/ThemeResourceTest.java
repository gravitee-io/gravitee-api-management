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
package io.gravitee.rest.api.management.v2.rest.resource.ui;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import inmemory.ThemeCrudServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.management.v2.rest.model.PortalComponentDefinition;
import io.gravitee.rest.api.management.v2.rest.model.PortalCssDefinition;
import io.gravitee.rest.api.management.v2.rest.model.PortalDefinition;
import io.gravitee.rest.api.management.v2.rest.model.PortalNextDefinition;
import io.gravitee.rest.api.management.v2.rest.model.PortalNextDefinitionColor;
import io.gravitee.rest.api.management.v2.rest.model.PortalNextDefinitionFont;
import io.gravitee.rest.api.management.v2.rest.model.ThemesResponse;
import io.gravitee.rest.api.management.v2.rest.model.UpdateTheme;
import io.gravitee.rest.api.management.v2.rest.model.UpdateThemePortal;
import io.gravitee.rest.api.management.v2.rest.model.UpdateThemePortalNext;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ThemeResourceTest extends AbstractResourceTest {

    private final String ENVIRONMENT = "env-id";
    private final String THEME_ID = "theme-id";

    @Autowired
    private ThemeCrudServiceInMemory themeCrudService;

    @Autowired
    private ThemeQueryServiceInMemory themeQueryService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/ui/themes/" + THEME_ID;
    }

    @BeforeEach
    void init() {
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);
        doReturn(environmentEntity).when(environmentService).findById(ENVIRONMENT);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        themeQueryService.reset();
        themeCrudService.reset();
    }

    @Nested
    class UpdateTheme {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_THEME),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);
            final Response response = rootTarget().request().put(Entity.json(UpdateThemePortal.builder().id(THEME_ID).build()));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_not_update_if_path_theme_id_does_not_match_body() {
            final Response response = rootTarget().request().put(Entity.json(UpdateThemePortal.builder().id("another-id").build()));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Theme update of theme [ " + THEME_ID + " ] is invalid.");
        }

        @Test
        public void should_not_update_non_existing_theme() {
            final Response response = rootTarget().request().put(Entity.json(UpdateThemePortal.builder().id(THEME_ID).build()));
            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Theme [ " + THEME_ID + " ] not found");
        }

        @Test
        public void should_not_update_theme_with_different_environment() {
            themeQueryService.initWith(List.of(aPortalTheme().toBuilder().referenceId("another-env").build()));
            themeCrudService.initWith(List.of(aPortalTheme().toBuilder().referenceId("another-env").build()));

            final Response response = rootTarget().request().put(Entity.json(UpdateThemePortal.builder().id(THEME_ID).build()));
            MAPIAssertions
                .assertThat(response)
                .hasStatus(NOT_FOUND_404)
                .asError()
                .hasHttpStatus(NOT_FOUND_404)
                .hasMessage("Theme [ " + THEME_ID + " ] not found");
        }

        @Test
        public void should_update_portal_theme() {
            themeQueryService.initWith(List.of(aPortalTheme()));
            themeCrudService.initWith(List.of(aPortalTheme()));

            var updateTheme = UpdateThemePortal
                .builder()
                .id(THEME_ID)
                .name("new name")
                .enabled(false)
                .backgroundImage("background image")
                .logo("background image")
                .optionalLogo("background image")
                .definition(
                    PortalDefinition
                        .builder()
                        .data(
                            List.of(
                                PortalComponentDefinition
                                    .builder()
                                    .name("title")
                                    .css(List.of(PortalCssDefinition.builder().name("background").value("#000").build()))
                                    .build()
                            )
                        )
                        .build()
                )
                .build();

            final Response response = rootTarget().request().put(Entity.json(updateTheme));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(io.gravitee.rest.api.management.v2.rest.model.Theme.class)
                .extracting(io.gravitee.rest.api.management.v2.rest.model.Theme::getThemePortal)
                .satisfies(theme -> {
                    assertThat(theme.getId()).isEqualTo(updateTheme.getId());
                    assertThat(theme.getName()).isEqualTo(updateTheme.getName());
                    assertThat(theme.getDefinition()).isEqualTo(updateTheme.getDefinition());
                    assertThat(theme.getEnabled()).isEqualTo(updateTheme.getEnabled());
                    assertThat(theme.getLogo()).isEqualTo(updateTheme.getLogo());
                    assertThat(theme.getOptionalLogo()).isEqualTo(updateTheme.getOptionalLogo());
                    assertThat(theme.getBackgroundImage()).isEqualTo(updateTheme.getBackgroundImage());
                });
        }

        @Test
        public void should_update_portal_next_theme() {
            themeQueryService.initWith(List.of(aPortalNextTheme()));
            themeCrudService.initWith(List.of(aPortalNextTheme()));

            var updateTheme = UpdateThemePortalNext
                .builder()
                .id(THEME_ID)
                .name("new name")
                .enabled(false)
                .logo("background image")
                .optionalLogo("background image")
                .definition(
                    PortalNextDefinition
                        .builder()
                        .color(PortalNextDefinitionColor.builder().primary("#666").secondary("#444").build())
                        .font(PortalNextDefinitionFont.builder().fontFamily("Comic Sans").build())
                        .customCss("a new style")
                        .build()
                )
                .build();

            final Response response = rootTarget().request().put(Entity.json(updateTheme));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(io.gravitee.rest.api.management.v2.rest.model.Theme.class)
                .extracting(io.gravitee.rest.api.management.v2.rest.model.Theme::getThemePortalNext)
                .satisfies(theme -> {
                    assertThat(theme.getId()).isEqualTo(updateTheme.getId());
                    assertThat(theme.getName()).isEqualTo(updateTheme.getName());
                    assertThat(theme.getDefinition()).isEqualTo(updateTheme.getDefinition());
                    assertThat(theme.getEnabled()).isEqualTo(updateTheme.getEnabled());
                    assertThat(theme.getLogo()).isEqualTo(updateTheme.getLogo());
                    assertThat(theme.getOptionalLogo()).isEqualTo(updateTheme.getOptionalLogo());
                });
        }

        @Test
        public void should_update_enabled_theme() {
            var currentThemeEnabled = aPortalNextTheme().toBuilder().enabled(true).id("current-theme").build();
            var disabledTheme = aPortalNextTheme().toBuilder().enabled(false).build();

            themeQueryService.initWith(List.of(disabledTheme, currentThemeEnabled));
            themeCrudService.initWith(List.of(disabledTheme, currentThemeEnabled));

            var updateTheme = UpdateThemePortalNext
                .builder()
                .id(THEME_ID)
                .name("new name")
                .enabled(true)
                .logo("background image")
                .optionalLogo("background image")
                .definition(
                    PortalNextDefinition
                        .builder()
                        .color(PortalNextDefinitionColor.builder().primary("#666").secondary("#444").build())
                        .font(PortalNextDefinitionFont.builder().fontFamily("Comic Sans").build())
                        .customCss("a new style")
                        .build()
                )
                .build();

            final Response response = rootTarget().request().put(Entity.json(updateTheme));

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(io.gravitee.rest.api.management.v2.rest.model.Theme.class)
                .extracting(io.gravitee.rest.api.management.v2.rest.model.Theme::getThemePortalNext)
                .satisfies(theme -> {
                    assertThat(theme.getId()).isEqualTo(updateTheme.getId());
                    assertThat(theme.getName()).isEqualTo(updateTheme.getName());
                    assertThat(theme.getDefinition()).isEqualTo(updateTheme.getDefinition());
                    assertThat(theme.getEnabled()).isEqualTo(updateTheme.getEnabled());
                    assertThat(theme.getLogo()).isEqualTo(updateTheme.getLogo());
                    assertThat(theme.getOptionalLogo()).isEqualTo(updateTheme.getOptionalLogo());
                });

            assertThat(themeCrudService.get(currentThemeEnabled.getId())).extracting(Theme::isEnabled).isEqualTo(false);
        }
    }

    private Theme aPortalTheme() {
        var portalDefinition = new ThemeDefinition();
        portalDefinition.setData(List.of());
        return Theme
            .builder()
            .id(THEME_ID)
            .name(THEME_ID)
            .type(ThemeType.PORTAL)
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .referenceId(ENVIRONMENT)
            .definitionPortal(portalDefinition)
            .createdAt(ZonedDateTime.now())
            .updatedAt(ZonedDateTime.now())
            .backgroundImage("hehe")
            .enabled(true)
            .build();
    }

    private Theme aPortalNextTheme() {
        var portalDefinition = io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
            .builder()
            .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("#fff").build())
            .build();
        return Theme
            .builder()
            .id(THEME_ID)
            .name(THEME_ID)
            .type(ThemeType.PORTAL_NEXT)
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .referenceId(ENVIRONMENT)
            .definitionPortalNext(portalDefinition)
            .createdAt(ZonedDateTime.now())
            .updatedAt(ZonedDateTime.now())
            .enabled(true)
            .build();
    }
}
