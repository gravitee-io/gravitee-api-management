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
import static fixtures.core.model.PortalNavigationItemFixtures.API1_ID;
import static fixtures.core.model.PortalNavigationItemFixtures.API2_ID;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.portal_page.use_case.SeedDefaultPagesForApiNavigationItemsUseCase;
import io.gravitee.rest.api.management.v2.rest.model.SeedDefaultPagesRequest;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationItemsResource_SeedDefaultPagesTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "environment-id";

    @Inject
    private SeedDefaultPagesForApiNavigationItemsUseCase seedDefaultPagesForApiNavigationItemsUseCase;

    @Inject
    private EnvironmentService environmentService;

    private WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portal-navigation-items/_default-pages";
    }

    @BeforeEach
    public void setUp() {
        target = rootTarget();

        var environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.CREATE
            )
        ).thenReturn(true);
    }

    @AfterEach
    public void tearDown() {
        GraviteeContext.cleanContext();
        Mockito.reset(seedDefaultPagesForApiNavigationItemsUseCase);
    }

    @Test
    void should_not_seed_default_pages_when_no_permission() {
        var request = new SeedDefaultPagesRequest().ids(List.of(API1_ID));
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.ENVIRONMENT_DOCUMENTATION,
                ENVIRONMENT,
                RolePermissionAction.CREATE
            )
        ).thenReturn(false);

        Response response = target.request().post(json(request));

        assertThat(response).hasStatus(FORBIDDEN_403);
    }

    @Test
    void should_seed_default_pages() {
        var request = new SeedDefaultPagesRequest().ids(List.of(API1_ID, API2_ID));
        when(seedDefaultPagesForApiNavigationItemsUseCase.execute(any())).thenReturn(
            new SeedDefaultPagesForApiNavigationItemsUseCase.Output(List.of())
        );

        Response response = target.request().post(json(request));

        assertThat(response).hasStatus(NO_CONTENT_204);
        verify(seedDefaultPagesForApiNavigationItemsUseCase).execute(any());
    }

    @Test
    void should_return_validation_error_when_ids_is_null() {
        var request = new SeedDefaultPagesRequest().ids(null);

        Response response = target.request().post(json(request));

        assertThat(response).hasStatus(BAD_REQUEST_400);
        verifyNoInteractions(seedDefaultPagesForApiNavigationItemsUseCase);
    }
}
