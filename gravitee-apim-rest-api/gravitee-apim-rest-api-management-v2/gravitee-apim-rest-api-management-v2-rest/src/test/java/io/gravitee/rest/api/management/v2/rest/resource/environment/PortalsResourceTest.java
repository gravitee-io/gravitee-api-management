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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static io.gravitee.apim.core.portal.model.Portal.of;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.common.http.HttpStatusCode.TEMPORARY_REDIRECT_307;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.LicenseFixtures;
import inmemory.AccessPointQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import inmemory.PortalNavigationItemsCrudServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import io.gravitee.apim.core.portal.model.PortalVisibility;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.Portal;
import io.gravitee.rest.api.management.v2.rest.model.PortalQuota;
import io.gravitee.rest.api.management.v2.rest.model.PortalsResponse;
import io.gravitee.rest.api.management.v2.rest.model.PortalsStats;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PortalsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";
    private static final PortalId DEFAULT_PORTAL_ID = PortalId.of("8f6a2b2e-3c1d-4f5e-9a7b-1c2d3e4f5a6b");
    private static final PortalId PARTNERS_PORTAL_ID = PortalId.of("1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d");

    @Inject
    private PortalCrudServiceInMemory portalCrudService;

    @Inject
    private AccessPointQueryServiceInMemory accessPointQueryService;

    @Inject
    private PortalNavigationItemsCrudServiceInMemory portalNavigationItemsCrudService;

    @Inject
    private ThemeQueryServiceInMemory themeQueryService;

    @Inject
    private LicenseManager licenseManager;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portals";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        var elsewherePortalId = PortalId.random();
        portalCrudService.initWith(
            List.of(
                portal(PARTNERS_PORTAL_ID, ENVIRONMENT, "Partners", false),
                portal(DEFAULT_PORTAL_ID, ENVIRONMENT, "Developer Portal", true),
                portal(elsewherePortalId, "other-env", "Elsewhere", false)
            )
        );
        accessPointQueryService.initWith(
            List.of(
                portalAccessPoint(PARTNERS_PORTAL_ID, ENVIRONMENT, "partners.acme.io"),
                portalAccessPoint(DEFAULT_PORTAL_ID, ENVIRONMENT, "portal.acme.io"),
                portalAccessPoint(elsewherePortalId, "other-env", "elsewhere.acme.io")
            )
        );
        portalNavigationItemsCrudService.initWith(
            List.of(publishedApi(PARTNERS_PORTAL_ID, "api-1"), publishedApi(PARTNERS_PORTAL_ID, "api-2"))
        );
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        portalCrudService.reset();
        accessPointQueryService.reset();
        portalNavigationItemsCrudService.reset();
        themeQueryService.reset();
        when(licenseManager.getOrganizationLicenseOrPlatform(any())).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    private void denyPortalPermission() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_PORTAL),
                eq(ENVIRONMENT),
                any(RolePermissionAction[].class)
            )
        ).thenReturn(false);
    }

    @Nested
    class ListPortals {

        @Test
        void should_return_the_environment_portals_default_first() {
            var response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(PortalsResponse.class);
            assertThat(body.getData()).hasSize(2);

            var defaultPortal = body.getData().getFirst();
            assertThat(defaultPortal.getId()).isEqualTo(DEFAULT_PORTAL_ID.id());
            assertThat(defaultPortal.getName()).isEqualTo("Developer Portal");
            assertThat(defaultPortal.getUrl()).isEqualTo("https://portal.acme.io/next");
            assertThat(defaultPortal.getDefault()).isTrue();
            assertThat(defaultPortal.getLogoUrl()).isNull();
            assertThat(defaultPortal.getPublishedApisCount()).isZero();

            var partners = body.getData().get(1);
            assertThat(partners.getId()).isEqualTo(PARTNERS_PORTAL_ID.id());
            assertThat(partners.getUrl()).isEqualTo("https://partners.acme.io/next");
            assertThat(partners.getDefault()).isFalse();
            assertThat(partners.getPublishedApisCount()).isEqualTo(2);

            assertThat(body.getPagination().getPage()).isEqualTo(1);
            assertThat(body.getPagination().getTotalCount()).isEqualTo(2);
            assertThat(body.getPagination().getPageItemsCount()).isEqualTo(2);
        }

        @Test
        void should_filter_by_name_and_count_only_matching_portals() {
            var response = rootTarget().queryParam("q", "partn").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(PortalsResponse.class);
            assertThat(body.getData()).extracting(Portal::getName).containsExactly("Partners");
            assertThat(body.getPagination().getTotalCount()).isEqualTo(1);
        }

        @Test
        void should_page_the_portals() {
            var response = rootTarget().queryParam("page", 2).queryParam("perPage", 1).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(PortalsResponse.class);
            assertThat(body.getData()).extracting(Portal::getName).containsExactly("Partners");
            assertThat(body.getPagination().getTotalCount()).isEqualTo(2);
            assertThat(body.getLinks()).isNotNull();
        }

        @Test
        void should_point_logo_url_to_the_portal_logo_when_the_next_gen_theme_has_a_logo() {
            themeQueryService.initWith(List.of(nextGenThemeWithLogo()));

            var body = rootTarget().request().get().readEntity(PortalsResponse.class);

            assertThat(body.getData()).allSatisfy(portal ->
                assertThat(portal.getLogoUrl()).endsWith("/environments/" + ENVIRONMENT + "/portals/" + portal.getId() + "/logo")
            );
        }

        @Test
        void should_return_403_without_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_DOCUMENTATION, ENVIRONMENT, RolePermissionAction.READ, () ->
                rootTarget().request().get()
            );
        }

        @Test
        void should_list_portals_without_the_portal_permission() {
            denyPortalPermission();

            var response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(PortalsResponse.class).getData()).hasSize(2);
        }
    }

    @Nested
    class GetPortal {

        @Test
        void should_return_the_portal() {
            themeQueryService.initWith(List.of(nextGenThemeWithLogo()));

            var response = rootTarget(PARTNERS_PORTAL_ID.toString()).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(Portal.class);
            assertThat(body.getId()).isEqualTo(PARTNERS_PORTAL_ID.id());
            assertThat(body.getName()).isEqualTo("Partners");
            assertThat(body.getUrl()).isEqualTo("https://partners.acme.io/next");
            assertThat(body.getDefault()).isFalse();
            assertThat(body.getLogoUrl()).endsWith("/environments/" + ENVIRONMENT + "/portals/" + PARTNERS_PORTAL_ID + "/logo");
            assertThat(body.getPublishedApisCount()).isEqualTo(2);
        }

        @Test
        void should_return_404_for_an_unknown_portal() {
            var response = rootTarget(UUID.randomUUID().toString()).request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_404_for_a_portal_of_another_environment() {
            var otherEnvPortal = portalCrudService
                .storage()
                .stream()
                .filter(portal -> portal.getEnvironmentId().equals("other-env"))
                .findFirst()
                .orElseThrow();

            var response = rootTarget(otherEnvPortal.getId().toString()).request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_404_for_an_id_that_is_not_a_uuid() {
            var response = rootTarget("not-a-uuid").request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_DOCUMENTATION, ENVIRONMENT, RolePermissionAction.READ, () ->
                rootTarget(PARTNERS_PORTAL_ID.toString()).request().get()
            );
        }
    }

    @Nested
    class RenamePortal {

        @Test
        void should_rename_the_default_portal_and_return_the_new_name_on_read() {
            try (
                var response = patch(
                    DEFAULT_PORTAL_ID,
                    """
                    {"name":"Developer Hub"}
                    """
                )
            ) {
                assertThat(response.getStatus()).isEqualTo(OK_200);
                var renamed = response.readEntity(Portal.class);
                assertThat(renamed.getId()).isEqualTo(DEFAULT_PORTAL_ID.id());
                assertThat(renamed.getName()).isEqualTo("Developer Hub");
                assertThat(renamed.getDefault()).isTrue();
                assertThat(renamed.getUrl()).isEqualTo("https://portal.acme.io/next");
            }

            var detail = rootTarget(DEFAULT_PORTAL_ID.toString()).request().get(Portal.class);
            assertThat(detail.getName()).isEqualTo("Developer Hub");
            var list = rootTarget().queryParam("q", "developer hub").request().get(PortalsResponse.class);
            assertThat(list.getData()).extracting(Portal::getName).containsExactly("Developer Hub");
        }

        @Test
        void should_trim_the_name_and_allow_the_same_name_in_another_environment() {
            try (
                var response = patch(
                    PARTNERS_PORTAL_ID,
                    """
                    {"name":"\\u00A0Elsewhere\\u200B"}
                    """
                )
            ) {
                assertThat(response.getStatus()).isEqualTo(OK_200);
                var renamed = response.readEntity(Portal.class);
                assertThat(renamed.getName()).isEqualTo("Elsewhere");
                assertThat(renamed.getDefault()).isFalse();
                assertThat(renamed.getUrl()).isEqualTo("https://partners.acme.io/next");
                assertThat(renamed.getPublishedApisCount()).isEqualTo(2);
            }
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = { "{}|Partners", "{\"name\":\"Partners\"}|Partners", "{\"name\":\"PARTNERS\"}|PARTNERS" })
        void should_allow_an_omitted_name_or_the_portals_own_name(String body, String expectedName) {
            try (var response = patch(PARTNERS_PORTAL_ID, body)) {
                assertThat(response.getStatus()).isEqualTo(OK_200);
                var portal = response.readEntity(Portal.class);
                assertThat(portal.getName()).isEqualTo(expectedName);
            }
        }

        @ParameterizedTest
        @ValueSource(
            strings = {
                "",
                "null",
                "[]",
                "{\"name\":null}",
                "{\"name\":\"\"}",
                "{\"name\":\"   \"}",
                "{\"name\":\"\\u00A0\\u200B\"}",
                "{\"name\":7}",
                "{\"url\":\"https://example.org\"}",
                "{\"name\":\"Renamed\",\"url\":\"https://example.org\"}",
            }
        )
        void should_reject_invalid_payloads(String body) {
            try (var response = patch(PARTNERS_PORTAL_ID, body)) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
            var unchanged = portalCrudService.findByIdAndEnvironmentId(PARTNERS_PORTAL_ID, ENVIRONMENT).orElseThrow();
            assertThat(unchanged.getName()).isEqualTo("Partners");
        }

        @ParameterizedTest
        @CsvSource(
            delimiter = '|',
            value = {
                "Developer Portal|DEVELOPER PORTAL",
                "Developer Portal|' DEVELOPER PORTAL '",
                "' Developer Portal '|DEVELOPER PORTAL",
                "Developer Portal|'DEVELOPER PORTAL\u00A0'",
                "'Developer Portal\u00A0'|DEVELOPER PORTAL",
                "Developer Portal|'\u200BDEVELOPER PORTAL'",
                "'\u200BDeveloper Portal'|DEVELOPER PORTAL",
            }
        )
        void should_return_the_name_conflict_code_ignoring_case_and_surrounding_whitespace(String existingName, String requestedName) {
            portalCrudService.updateName(DEFAULT_PORTAL_ID, ENVIRONMENT, existingName);
            try (
                var response = patch(
                    PARTNERS_PORTAL_ID,
                    """
                    {"name":"%s"}
                    """.formatted(requestedName)
                )
            ) {
                assertThat(response.getStatus()).isEqualTo(409);
                assertThat(response.readEntity(Error.class).getTechnicalCode()).isEqualTo("portal.name.exists");
            }
        }

        @Test
        void should_return_404_for_unknown_and_foreign_portals() {
            var foreignId = portalCrudService.findByEnvironmentId("other-env").getFirst().getId();
            for (var id : List.of(foreignId, PortalId.random())) {
                try (
                    var response = patch(
                        id,
                        """
                        {"name":"New name"}
                        """
                    )
                ) {
                    assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
                }
            }
        }

        @Test
        void should_require_the_portal_update_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_PORTAL, ENVIRONMENT, RolePermissionAction.UPDATE, () ->
                patch(
                    PARTNERS_PORTAL_ID,
                    """
                    {"name":"New name"}
                    """
                )
            );
            var unchanged = portalCrudService.findByIdAndEnvironmentId(PARTNERS_PORTAL_ID, ENVIRONMENT).orElseThrow();
            assertThat(unchanged.getName()).isEqualTo("Partners");
        }

        @Test
        void should_rename_a_portal_without_the_documentation_update_permission() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.ENVIRONMENT_DOCUMENTATION,
                    ENVIRONMENT,
                    RolePermissionAction.UPDATE
                )
            ).thenReturn(false);

            try (
                var response = patch(
                    PARTNERS_PORTAL_ID,
                    """
                    {"name":"New name"}
                    """
                )
            ) {
                assertThat(response.getStatus()).isEqualTo(OK_200);
                assertThat(response.readEntity(Portal.class).getName()).isEqualTo("New name");
            }
        }

        private Response patch(PortalId portalId, String body) {
            return rootTarget(portalId.toString()).request().method("PATCH", Entity.json(body));
        }
    }

    @Nested
    class DeletePortal {

        @Test
        void should_delete_the_portal_and_its_listings() {
            var theme = nextGenThemeWithLogo();
            themeQueryService.initWith(List.of(theme));
            var response = rootTarget(PARTNERS_PORTAL_ID.toString()).request().delete();

            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);
            assertThat(portalCrudService.findByIdAndEnvironmentId(PARTNERS_PORTAL_ID, ENVIRONMENT)).isEmpty();
            assertThat(portalCrudService.findByIdAndEnvironmentId(DEFAULT_PORTAL_ID, ENVIRONMENT)).isPresent();
            assertThat(portalNavigationItemsCrudService.storage()).isEmpty();
            var remaining = rootTarget().request().get().readEntity(PortalsResponse.class);
            assertThat(remaining.getData()).extracting(Portal::getId).containsExactly(DEFAULT_PORTAL_ID.id());
            var stats = rootTarget("_stats").request().get().readEntity(PortalsStats.class);
            assertThat(stats.getQuota().getUsed()).isEqualTo(1);
            assertThat(themeQueryService.storage()).containsExactly(theme);
            assertThat(theme.isEnabled()).isTrue();
        }

        @Test
        void should_refuse_the_default_portal_without_mutation() {
            var portalsBefore = List.copyOf(portalCrudService.storage());
            var itemsBefore = List.copyOf(portalNavigationItemsCrudService.storage());

            var response = rootTarget(DEFAULT_PORTAL_ID.toString()).request().delete();

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            assertThat(response.readEntity(Error.class).getTechnicalCode()).isEqualTo("portal.default.notDeletable");
            assertThat(portalCrudService.storage()).containsExactlyElementsOf(portalsBefore);
            assertThat(portalNavigationItemsCrudService.storage()).containsExactlyElementsOf(itemsBefore);
        }

        @Test
        void should_return_404_without_mutation_for_unknown_or_foreign_portal() {
            var portalsBefore = List.copyOf(portalCrudService.storage());
            var itemsBefore = List.copyOf(portalNavigationItemsCrudService.storage());
            var other = portalsBefore
                .stream()
                .filter(portal -> portal.getEnvironmentId().equals("other-env"))
                .findFirst()
                .orElseThrow();

            assertThat(rootTarget(UUID.randomUUID().toString()).request().delete().getStatus()).isEqualTo(NOT_FOUND_404);
            assertThat(rootTarget(other.getId().toString()).request().delete().getStatus()).isEqualTo(NOT_FOUND_404);
            assertThat(portalCrudService.storage()).containsExactlyElementsOf(portalsBefore);
            assertThat(portalNavigationItemsCrudService.storage()).containsExactlyElementsOf(itemsBefore);
        }

        @Test
        void should_refuse_without_delete_permission_and_leave_data_untouched() {
            var portalsBefore = List.copyOf(portalCrudService.storage());
            var itemsBefore = List.copyOf(portalNavigationItemsCrudService.storage());

            shouldReturn403(RolePermission.ENVIRONMENT_PORTAL, ENVIRONMENT, RolePermissionAction.DELETE, () ->
                rootTarget(PARTNERS_PORTAL_ID.toString()).request().delete()
            );
            assertThat(portalCrudService.storage()).containsExactlyElementsOf(portalsBefore);
            assertThat(portalNavigationItemsCrudService.storage()).containsExactlyElementsOf(itemsBefore);
        }
    }

    @Nested
    class GetPortalsStats {

        @Test
        void should_return_the_quota_and_the_published_apis_of_every_portal() {
            when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION)).thenReturn(
                LicenseFixtures.aLicenseWithAttribute("apim-portals-max", 10)
            );
            portalNavigationItemsCrudService.initWith(
                List.of(
                    publishedApi(PARTNERS_PORTAL_ID, "api-1"),
                    publishedApi(DEFAULT_PORTAL_ID, "api-1"),
                    publishedApi(DEFAULT_PORTAL_ID, "api-3")
                )
            );

            var response = rootTarget("_stats").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(PortalsStats.class)).isEqualTo(
                new PortalsStats().quota(new PortalQuota().used(2).limit(10)).publishedApisCount(2)
            );
        }

        @Test
        void should_allow_one_portal_when_the_license_sets_no_portal_limit() {
            var body = rootTarget("_stats").request().get().readEntity(PortalsStats.class);

            assertThat(body.getQuota()).isEqualTo(new PortalQuota().used(2).limit(1));
        }

        @Test
        void should_return_403_without_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_DOCUMENTATION, ENVIRONMENT, RolePermissionAction.READ, () ->
                rootTarget("_stats").request().get()
            );
        }

        @Test
        void should_return_the_quota_without_the_portal_permission() {
            denyPortalPermission();

            var response = rootTarget("_stats").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(PortalsStats.class).getQuota()).isEqualTo(new PortalQuota().used(2).limit(1));
        }
    }

    @Nested
    class GetPortalLogo {

        private static final byte[] PNG_BYTES = { (byte) 0x89, 'P', 'N', 'G' };

        @Test
        void should_return_the_next_gen_theme_logo_with_its_media_type() {
            themeQueryService.initWith(List.of(nextGenTheme("data:image/png;base64," + Base64.getEncoder().encodeToString(PNG_BYTES))));

            var response = rootTarget(DEFAULT_PORTAL_ID + "/logo").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.getMediaType()).hasToString("image/png");
            assertThat(response.readEntity(byte[].class)).isEqualTo(PNG_BYTES);
        }

        @Test
        void should_redirect_to_a_logo_stored_as_a_url() {
            themeQueryService.initWith(List.of(nextGenTheme("https://cdn.acme.io/logo.png")));

            var response = rootTarget(DEFAULT_PORTAL_ID + "/logo").request().get();

            assertThat(response.getStatus()).isEqualTo(TEMPORARY_REDIRECT_307);
            assertThat(response.getLocation()).isEqualTo(URI.create("https://cdn.acme.io/logo.png"));
        }

        @Test
        void should_return_404_when_the_theme_has_no_logo() {
            var response = rootTarget(DEFAULT_PORTAL_ID + "/logo").request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_404_for_a_portal_of_another_environment() {
            themeQueryService.initWith(List.of(nextGenThemeWithLogo()));
            var otherEnvPortal = portalCrudService
                .storage()
                .stream()
                .filter(portal -> portal.getEnvironmentId().equals("other-env"))
                .findFirst()
                .orElseThrow();

            var response = rootTarget(otherEnvPortal.getId() + "/logo").request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_DOCUMENTATION, ENVIRONMENT, RolePermissionAction.READ, () ->
                rootTarget(DEFAULT_PORTAL_ID + "/logo").request().get()
            );
        }
    }

    private static Theme nextGenThemeWithLogo() {
        return nextGenTheme("data:image/png;base64,AAA");
    }

    private static Theme nextGenTheme(String logo) {
        return Theme.builder()
            .id("theme-next")
            .type(ThemeType.PORTAL_NEXT)
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .referenceId(ENVIRONMENT)
            .enabled(true)
            .logo(logo)
            .build();
    }

    private static PortalNavigationApi publishedApi(PortalId portalId, String apiId) {
        return PortalNavigationApi.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORGANIZATION)
            .environmentId(ENVIRONMENT)
            .reference(new NavigationItemReference.PortalReference(portalId))
            .title(apiId)
            .segment(apiId)
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId(apiId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private static io.gravitee.apim.core.portal.model.Portal portal(PortalId id, String environmentId, String name, boolean defaultPortal) {
        return of(id, environmentId, ORGANIZATION, name, PortalNavigationStructure.empty(), null, defaultPortal, "ap-" + id);
    }

    private static AccessPoint portalAccessPoint(PortalId portalId, String environmentId, String host) {
        return AccessPoint.builder()
            .id("ap-" + portalId)
            .referenceType(AccessPoint.ReferenceType.ENVIRONMENT)
            .referenceId(environmentId)
            .target(AccessPoint.Target.PORTAL_INSTANCE)
            .host(host)
            .secured(true)
            .build();
    }
}
