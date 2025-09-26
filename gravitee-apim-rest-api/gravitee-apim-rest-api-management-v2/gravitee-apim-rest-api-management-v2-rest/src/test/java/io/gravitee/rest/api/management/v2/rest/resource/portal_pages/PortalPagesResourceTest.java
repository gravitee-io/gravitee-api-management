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
package io.gravitee.rest.api.management.v2.rest.resource.portal_pages;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import io.gravitee.rest.api.management.v2.rest.model.PatchPortalPage;
import io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails;
import io.gravitee.rest.api.management.v2.rest.model.PortalPagesResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PortalPagesResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";
    WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portal-pages";
    }

    @BeforeEach
    public void init() {
        target = rootTarget();
        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
    }

    @Nested
    class GetPortalPages {

        @Test
        void should_return_homepage_portal_page() {
            setupPermission(true);
            setupHomepage(true);
            Response response = target.queryParam("type", "HOMEPAGE").request().get();
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PortalPagesResponse.class)
                .extracting(PortalPagesResponse::getPages)
                .extracting(List::getFirst)
                .satisfies(r -> {
                    assertThat(r.getContent()).isEqualTo("Welcome!");
                    assertThat(r.getContext()).isEqualTo(
                        io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails.ContextEnum.HOMEPAGE
                    );
                    assertThat(r.getType()).isEqualTo(
                        io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails.TypeEnum.GRAVITEE_MARKDOWN
                    );
                });
        }

        @Test
        void should_return_403_for_unauthorized_user() {
            setupPermission(false);
            Response response = target.queryParam("type", "HOMEPAGE").request().get();
            assertThat(response).hasStatus(403);
        }
    }

    private PortalPageWithViewDetails setupHomepage(boolean published) {
        PortalPage page = PortalPage.create(new GraviteeMarkdown("Welcome!"));
        PortalPageView view = new PortalPageView(PortalViewContext.HOMEPAGE, published);
        PortalPageWithViewDetails details = new PortalPageWithViewDetails(page, view);
        portalPageContextCrudService.initWith(
            List.of(new PortalPageContext("x", page.getId().toString(), PortalPageContextType.HOMEPAGE, ENVIRONMENT, published))
        );
        portalPageQueryService.initWith(List.of(details));
        portalPageCrudService.initWith(List.of(page));

        return details;
    }

    private void setupPermission(boolean hasPermission) {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_DOCUMENTATION),
                any(),
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(hasPermission);
    }

    private void setupPermissionForUpdate(boolean hasPermission) {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_DOCUMENTATION),
                any(),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(hasPermission);
    }

    @Nested
    class PatchPortalPageTest {

        @Test
        void should_update_homepage_portal_page() {
            setupPermissionForUpdate(true);
            String updatedContent = "Updated homepage!";
            var patchPortalPage = new PatchPortalPage();
            patchPortalPage.setContent(updatedContent);
            var existingHomepage = setupHomepage(true);
            Response response = target
                .path("/" + existingHomepage.page().getId())
                .request()
                .method("PATCH", jakarta.ws.rs.client.Entity.json(patchPortalPage));
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PortalPageWithDetails.class)
                .satisfies(r -> {
                    assertThat(r.getContent()).isEqualTo(updatedContent);
                    assertThat(r.getContext()).isEqualTo(
                        io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails.ContextEnum.HOMEPAGE
                    );
                    assertThat(r.getType()).isEqualTo(
                        io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails.TypeEnum.GRAVITEE_MARKDOWN
                    );
                });
        }

        @Test
        void should_return_403_for_unauthorized_user() {
            setupPermissionForUpdate(false);
            var patchPortalPage = new PatchPortalPage();
            patchPortalPage.setContent("Updated homepage!");
            Response response = target.path("/_homepage").request().method("PATCH", jakarta.ws.rs.client.Entity.json(patchPortalPage));
            assertThat(response).hasStatus(403);
        }
    }

    @Nested
    class PublishPortalPageTest {

        @Test
        void should_publish_homepage_portal_page() {
            setupPermissionForUpdate(true);
            var existingHomepage = setupHomepage(false);
            Response response = target.path("/" + existingHomepage.page().getId() + "/_publish").request().post(null);
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PortalPageWithDetails.class)
                .satisfies(r -> {
                    assertThat(r.getContent()).isEqualTo("Welcome!");
                    assertThat(r.getContext()).isEqualTo(
                        io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails.ContextEnum.HOMEPAGE
                    );
                    assertThat(r.getType()).isEqualTo(
                        io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails.TypeEnum.GRAVITEE_MARKDOWN
                    );
                });
            assertThat(portalPageContextCrudService.storage().getFirst().isPublished()).isTrue();
        }

        @Test
        void should_return_403_for_unauthorized_user() {
            setupPermissionForUpdate(false);
            Response response = target.path("/_homepage/_publish").request().post(null);
            assertThat(response).hasStatus(403);
        }
    }

    @Nested
    class UnpublishPortalPageTest {

        @Test
        void should_unpublish_homepage_portal_page() {
            setupPermissionForUpdate(true);
            var existingHomepage = setupHomepage(true);
            Response response = target.path("/" + existingHomepage.page().getId() + "/_unpublish").request().post(null);
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PortalPageWithDetails.class)
                .satisfies(r -> {
                    assertThat(r.getContent()).isEqualTo("Welcome!");
                    assertThat(r.getContext()).isEqualTo(
                        io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails.ContextEnum.HOMEPAGE
                    );
                    assertThat(r.getType()).isEqualTo(
                        io.gravitee.rest.api.management.v2.rest.model.PortalPageWithDetails.TypeEnum.GRAVITEE_MARKDOWN
                    );
                });
            assertThat(portalPageContextCrudService.storage().getFirst().isPublished()).isFalse();
        }

        @Test
        void should_return_403_for_unauthorized_user() {
            setupPermissionForUpdate(false);
            Response response = target.path("/_homepage/_unpublish").request().post(null);
            assertThat(response).hasStatus(403);
        }
    }
}
