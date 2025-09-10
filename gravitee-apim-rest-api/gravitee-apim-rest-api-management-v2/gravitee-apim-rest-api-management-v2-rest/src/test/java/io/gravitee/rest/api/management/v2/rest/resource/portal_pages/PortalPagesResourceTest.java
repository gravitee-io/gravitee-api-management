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
import io.gravitee.rest.api.management.v2.rest.model.PortalPageResponse;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    class GetPortalHomepage {

        @Test
        void should_return_homepage_portal_page() {
            setupPermission(true);
            setupHomepage();
            Response response = target.path("/_homepage").request().get();
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(PortalPageResponse.class)
                .satisfies(r -> {
                    assertThat(r.getContent()).isEqualTo("Welcome!");
                    assertThat(r.getContext()).isEqualTo(PortalPageResponse.ContextEnum.HOMEPAGE);
                    assertThat(r.getType()).isEqualTo(PortalPageResponse.TypeEnum.GRAVITEE_MARKDOWN);
                });
        }

        @Test
        void should_return_403_for_unauthorized_user() {
            setupPermission(false);
            Response response = target.path("/_homepage").request().get();
            assertThat(response).hasStatus(403);
        }
    }

    private void setupHomepage() {
        PortalPage page = PortalPage.create(new GraviteeMarkdown("Welcome!"));
        PortalPageView view = new PortalPageView(PortalViewContext.HOMEPAGE, true);
        PortalPageWithViewDetails details = new PortalPageWithViewDetails(page, view);
        portalPageContextCrudService.initWith(
            List.of(new PortalPageContext("x", page.id().toString(), PortalPageContextType.HOMEPAGE, ENVIRONMENT, true))
        );
        portalPageQueryService.initWith(List.of(details));
    }

    private void setupPermission(boolean hasPermission) {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_DOCUMENTATION),
                any(),
                eq(RolePermissionAction.READ)
            )
        )
            .thenReturn(hasPermission);
    }
}
