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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import inmemory.PortalCategoryCrudServiceInMemory;
import inmemory.PortalCategoryQueryServiceInMemory;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.rest.api.management.v2.rest.model.CreatePortalCategory;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalCategoriesResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";

    @Inject
    private PortalCategoryCrudServiceInMemory portalCategoryCrudServiceInMemory;

    @Inject
    private PortalCategoryQueryServiceInMemory portalCategoryQueryServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portal-categories";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        portalCategoryCrudServiceInMemory.reset();
        portalCategoryQueryServiceInMemory.reset();
    }

    @Nested
    class ListPortalCategories {

        @Test
        void should_return_portal_categories_sorted_alphabetically() {
            portalCategoryQueryServiceInMemory.initWith(
                List.of(
                    PortalCategory.of(PortalCategoryId.of("f47ac10b-58cc-4372-a567-0e02b2c3d479"), ENVIRONMENT, "Weather", null, true),
                    PortalCategory.of(PortalCategoryId.of("6ba7b810-9dad-11d1-80b4-00c04fd430c8"), ENVIRONMENT, "Analytics", null, false)
                )
            );

            var response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalCategory[].class);
            assertThat(body)
                .extracting(io.gravitee.rest.api.management.v2.rest.model.PortalCategory::getTitle)
                .containsExactly("Analytics", "Weather");
        }

        @Test
        void should_return_empty_array_when_no_categories() {
            var response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalCategory[].class);
            assertThat(body).isEmpty();
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ENVIRONMENT_DOCUMENTATION, ENVIRONMENT, RolePermissionAction.READ, () ->
                rootTarget().request().get()
            );
        }
    }

    @Nested
    class CreatePortalCategoryTests {

        @Test
        void should_create_portal_category() {
            var createPortalCategory = new CreatePortalCategory().title("Weather").description("Weather APIs").visible(true);

            var response = rootTarget().request().post(json(createPortalCategory));

            assertThat(response.getStatus()).isEqualTo(CREATED_201);
            var created = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalCategory.class);
            assertThat(created.getId()).isNotNull();
            assertThat(created.getTitle()).isEqualTo("Weather");
            assertThat(created.getDescription()).isEqualTo("Weather APIs");
            assertThat(created.getVisible()).isTrue();

            assertThat(portalCategoryCrudServiceInMemory.storage()).hasSize(1);
            assertThat(portalCategoryCrudServiceInMemory.storage().getFirst().getEnvironmentId()).isEqualTo(ENVIRONMENT);
        }

        @Test
        void should_return_400_when_title_is_missing() {
            var createPortalCategory = new CreatePortalCategory().description("Weather APIs");

            var response = rootTarget().request().post(json(createPortalCategory));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_400_when_title_is_blank() {
            var createPortalCategory = new CreatePortalCategory().title("   ");

            var response = rootTarget().request().post(json(createPortalCategory));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ENVIRONMENT_DOCUMENTATION, ENVIRONMENT, RolePermissionAction.CREATE, () ->
                rootTarget().request().post(json(new CreatePortalCategory().title("Weather")))
            );
        }
    }
}
