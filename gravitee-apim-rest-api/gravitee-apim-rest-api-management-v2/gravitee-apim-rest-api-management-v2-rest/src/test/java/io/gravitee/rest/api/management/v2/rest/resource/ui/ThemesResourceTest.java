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
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.management.v2.rest.model.ThemesResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ThemesResourceTest extends AbstractResourceTest {

    private final String ENVIRONMENT = "env-id";
    private final String PORTAL_THEME_ID = "portal-id";
    private final String PORTAL_NEXT_THEME_ID = "portal-next-id";

    @Autowired
    private ThemeQueryServiceInMemory themeQueryService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/ui/themes";
    }

    @BeforeEach
    void init() {
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        themeQueryService.reset();
    }

    @Nested
    class GetThemes {

        @BeforeEach
        void setUp() {
            themeQueryService.initWith(List.of(aPortalTheme(), aPortalNextTheme()));
        }

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_THEME),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);
            final Response response = rootTarget().request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_get_empty_list() {
            final Response response = rootTarget().queryParam("enabled", false).request().get();
            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ThemesResponse.class)
                .extracting(ThemesResponse::getData)
                .satisfies(list -> {
                    assertThat(list).isEmpty();
                });
        }

        @Test
        public void should_return_page_of_results() {
            final Response response = rootTarget().queryParam("type", "PORTAL").queryParam("enabled", true).request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ThemesResponse.class)
                .extracting(ThemesResponse::getData)
                .satisfies(list -> {
                    assertThat(list).hasSize(1);
                    assertThat(list.get(0).getThemePortal().getId()).isEqualTo(PORTAL_THEME_ID);
                });
        }

        @Test
        public void should_return_all_results_if_no_search_criteria_specified() {
            final Response response = rootTarget().request().get();
            assertThat(response.getStatus()).isEqualTo(200);

            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(ThemesResponse.class)
                .extracting(ThemesResponse::getData)
                .satisfies(list -> {
                    assertThat(list).hasSize(2);
                });
        }
    }

    @Nested
    class GetDefaultTheme {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_THEME),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);
            final Response response = rootTarget().path("_default").queryParam("type", "PORTAL_NEXT").request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_400_when_portal_theme_type() {
            final Response response = rootTarget().path("_default").queryParam("type", "PORTAL").request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("Theme type [PORTAL] is not supported");
        }

        @Test
        public void should_return_400_when_no_theme_type_specified() {
            final Response response = rootTarget().path("_default").request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("[ null ] theme is currently not supported");
        }

        @Test
        public void should_get_portal_next_default_theme() {
            final Response response = rootTarget().path("_default").queryParam("type", "PORTAL_NEXT").request().get();
            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(io.gravitee.rest.api.management.v2.rest.model.Theme.class)
                .isNotNull();
        }
    }

    @Nested
    class GetCurrentTheme {

        @Test
        public void should_return_403_if_incorrect_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_THEME),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);
            final Response response = rootTarget().path("_current").queryParam("type", "PORTAL_NEXT").request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(FORBIDDEN_403)
                .asError()
                .hasHttpStatus(FORBIDDEN_403)
                .hasMessage("You do not have sufficient rights to access this resource");
        }

        @Test
        public void should_return_400_when_no_theme_type_specified() {
            final Response response = rootTarget().path("_current").request().get();

            MAPIAssertions
                .assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasHttpStatus(BAD_REQUEST_400)
                .hasMessage("[ null ] theme is currently not supported");
        }

        @Test
        public void should_create_new_enabled_default_portal_next_theme() {
            final Response response = rootTarget().path("_current").queryParam("type", "PORTAL_NEXT").request().get();
            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(io.gravitee.rest.api.management.v2.rest.model.Theme.class)
                .isNotNull()
                .extracting(res -> res.getThemePortalNext().getName())
                .isEqualTo("Default Portal Next Theme");
        }

        @Test
        public void should_create_new_enabled_default_portal_theme() {
            final Response response = rootTarget().path("_current").queryParam("type", "PORTAL").request().get();
            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(io.gravitee.rest.api.management.v2.rest.model.Theme.class)
                .isNotNull()
                .extracting(res -> res.getThemePortal().getId())
                .isEqualTo("portal-default-theme");
        }

        @Test
        public void should_return_enabled_portal_next_theme() {
            themeQueryService.initWith(List.of(aPortalNextTheme()));
            final Response response = rootTarget().path("_current").queryParam("type", "PORTAL_NEXT").request().get();
            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(io.gravitee.rest.api.management.v2.rest.model.Theme.class)
                .isNotNull()
                .extracting(res -> res.getThemePortalNext().getId())
                .isEqualTo(aPortalNextTheme().getId());
        }

        @Test
        public void should_return_enabled_portal_theme() {
            themeQueryService.initWith(List.of(aPortalTheme()));
            final Response response = rootTarget().path("_current").queryParam("type", "PORTAL").request().get();
            MAPIAssertions
                .assertThat(response)
                .hasStatus(OK_200)
                .asEntity(io.gravitee.rest.api.management.v2.rest.model.Theme.class)
                .isNotNull()
                .extracting(res -> res.getThemePortal().getId())
                .isEqualTo(aPortalTheme().getId());
        }
    }

    private Theme aPortalTheme() {
        var portalDefinition = new ThemeDefinition();
        portalDefinition.setData(List.of());
        return Theme
            .builder()
            .id(PORTAL_THEME_ID)
            .name(PORTAL_THEME_ID)
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
            .id(PORTAL_NEXT_THEME_ID)
            .name(PORTAL_NEXT_THEME_ID)
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
