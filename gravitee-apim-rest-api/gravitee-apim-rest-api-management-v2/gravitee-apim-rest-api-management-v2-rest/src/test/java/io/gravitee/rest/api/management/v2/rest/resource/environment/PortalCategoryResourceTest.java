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
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import inmemory.PortalCategoryCrudServiceInMemory;
import io.gravitee.apim.core.portal_category.model.PortalCategory;
import io.gravitee.apim.core.portal_category.model.PortalCategoryId;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePortalCategory;
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
class PortalCategoryResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";
    private static final String CATEGORY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

    @Inject
    private PortalCategoryCrudServiceInMemory portalCategoryCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/portal-categories/" + CATEGORY_ID;
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        portalCategoryCrudServiceInMemory.initWith(
            List.of(PortalCategory.of(PortalCategoryId.of(CATEGORY_ID), ENVIRONMENT, "Weather", "Weather APIs", true))
        );
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        portalCategoryCrudServiceInMemory.reset();
    }

    @Nested
    class UpdatePortalCategoryTests {

        @Test
        void should_update_portal_category() {
            var updatePortalCategory = new UpdatePortalCategory()
                .title("Updated Weather")
                .description("Updated description")
                .visible(false);

            var response = rootTarget().request().put(json(updatePortalCategory));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var updated = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.PortalCategory.class);
            assertThat(updated.getTitle()).isEqualTo("Updated Weather");
            assertThat(updated.getDescription()).isEqualTo("Updated description");
            assertThat(updated.getVisible()).isFalse();

            assertThat(portalCategoryCrudServiceInMemory.storage().getFirst().getTitle()).isEqualTo("Updated Weather");
        }

        @Test
        void should_return_400_when_title_is_blank() {
            var updatePortalCategory = new UpdatePortalCategory().title(" ");

            var response = rootTarget().request().put(json(updatePortalCategory));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_404_when_category_does_not_exist() {
            portalCategoryCrudServiceInMemory.reset();

            var response = rootTarget().request().put(json(new UpdatePortalCategory().title("Weather")));

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ENVIRONMENT_DOCUMENTATION, ENVIRONMENT, RolePermissionAction.UPDATE, () ->
                rootTarget().request().put(json(new UpdatePortalCategory().title("Weather")))
            );
        }
    }

    @Nested
    class DeletePortalCategoryTests {

        @Test
        void should_delete_portal_category() {
            var response = rootTarget().request().delete();

            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);
            assertThat(portalCategoryCrudServiceInMemory.storage()).isEmpty();
        }

        @Test
        void should_return_404_when_category_does_not_exist() {
            portalCategoryCrudServiceInMemory.reset();

            var response = rootTarget().request().delete();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.ENVIRONMENT_DOCUMENTATION, ENVIRONMENT, RolePermissionAction.DELETE, () ->
                rootTarget().request().delete()
            );
        }
    }
}
