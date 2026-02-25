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

import static assertions.MAPIAssertions.assertThat;
import static fixtures.core.model.PortalPageContentFixtures.CONTENT_ID;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.Mockito.when;

import fixtures.core.model.PortalPageContentFixtures;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageContentUseCase;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalPageContentsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";

    @Inject
    private GetPortalPageContentUseCase getPortalPageContentUseCase;

    @Inject
    private EnvironmentService environmentService;

    private WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portal-page-contents";
    }

    @BeforeEach
    public void setUp() {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        portalPageContentQueryService.initWith(PortalPageContentFixtures.samplePortalPageContents());
        portalPageContentCrudService.initWith(PortalPageContentFixtures.samplePortalPageContents());
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        portalPageContentQueryService.reset();
        portalPageContentCrudService.reset();
    }

    @Test
    void should_return_portal_page_content() {
        // Given
        var content = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
            PortalPageContentId.of(CONTENT_ID),
            ORGANIZATION,
            ENVIRONMENT,
            PortalPageContentFixtures.CONTENT
        );

        when(getPortalPageContentUseCase.execute(new GetPortalPageContentUseCase.Input(PortalPageContentId.of(CONTENT_ID)))).thenReturn(
            new GetPortalPageContentUseCase.Output(content)
        );

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target.path(CONTENT_ID).request().get();

        // Then
        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(io.gravitee.rest.api.management.v2.rest.model.PortalPageContent.class)
            .isNotNull()
            .satisfies(result -> {
                assertThat(result.getId()).isEqualTo(CONTENT_ID);
                assertThat(result.getContent()).isEqualTo(PortalPageContentFixtures.CONTENT);
                assertThat(result.getType()).isEqualTo(
                    io.gravitee.rest.api.management.v2.rest.model.PortalPageContentType.GRAVITEE_MARKDOWN
                );
            });
    }

    @Test
    void should_return_403_when_insufficient_permissions() {
        // Given
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(false);

        // When
        Response response = target.path(CONTENT_ID).request().get();

        // Then
        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_return_400_when_content_id_has_invalid_format() {
        // Given
        String invalidContentId = "invalid-uuid-format";

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.READ
            )
        ).thenReturn(true);

        // When
        Response response = target.path(invalidContentId).request().get();

        // Then
        assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Nested
    class UpdatePortalPageContent {

        @BeforeEach
        void setUp() {
            var content = PortalPageContentFixtures.aGraviteeMarkdownPageContent(
                PortalPageContentId.of(CONTENT_ID),
                ORGANIZATION,
                ENVIRONMENT,
                PortalPageContentFixtures.CONTENT
            );

            portalPageContentCrudService.initWith(List.of(content));
            portalPageContentQueryService.initWith(List.of(content));
        }

        @Test
        void should_update_portal_page_content() {
            // Given
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.ENVIRONMENT_DOCUMENTATION,
                    ENVIRONMENT,
                    RolePermissionAction.UPDATE
                )
            ).thenReturn(true);

            var updateRequest = new io.gravitee.rest.api.management.v2.rest.model.UpdatePortalPageContent();
            updateRequest.setContent("Updated content");

            // When
            Response response = target.path(CONTENT_ID).request().put(Entity.json(updateRequest));

            // Then
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(io.gravitee.rest.api.management.v2.rest.model.PortalPageContent.class)
                .satisfies(result -> {
                    assertThat(result.getId()).isEqualTo(CONTENT_ID);
                    assertThat(result.getContent()).isEqualTo("Updated content");
                });
        }

        @Test
        void should_return_400_when_content_is_empty() {
            // Given
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.ENVIRONMENT_DOCUMENTATION,
                    ENVIRONMENT,
                    RolePermissionAction.UPDATE
                )
            ).thenReturn(true);

            var updateRequest = new io.gravitee.rest.api.management.v2.rest.model.UpdatePortalPageContent();
            updateRequest.setContent(" ");

            // When
            Response response = target.path(CONTENT_ID).request().put(Entity.json(updateRequest));

            // Then
            assertThat(response).hasStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_content_is_null() {
            // Given
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.ENVIRONMENT_DOCUMENTATION,
                    ENVIRONMENT,
                    RolePermissionAction.UPDATE
                )
            ).thenReturn(true);

            var updateRequest = new io.gravitee.rest.api.management.v2.rest.model.UpdatePortalPageContent();

            // When
            Response response = target.path(CONTENT_ID).request().put(Entity.json(updateRequest));

            // Then
            assertThat(response).hasStatus(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_when_insufficient_permissions() {
            // Given
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.ENVIRONMENT_DOCUMENTATION,
                    ENVIRONMENT,
                    RolePermissionAction.UPDATE
                )
            ).thenReturn(false);

            var updateRequest = new io.gravitee.rest.api.management.v2.rest.model.UpdatePortalPageContent();
            updateRequest.setContent("Updated content");

            // When
            Response response = target.path(CONTENT_ID).request().put(Entity.json(updateRequest));

            // Then
            assertThat(response).hasStatus(FORBIDDEN_403);
        }
    }
}
