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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.ApiProductPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiProductMembersResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/api-products/" + API_PRODUCT_ID + "/members";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(membershipService);
    }

    @Nested
    class GetApiProductMemberPermissionsTest {

        /**
         * JerseySpringTest.AuthenticationFilter always returns {@code true} for {@code isUserInRole},
         * so {@code isAdmin()} is {@code true} in all tests — only the admin branch of
         * {@link io.gravitee.rest.api.management.v2.rest.resource.api_product.ApiProductMembersResource#getApiProductMemberPermissions()}
         * can be exercised here. The non-admin branch (which delegates to
         * {@code membershipService.getUserMemberPermissions}) cannot be reached through this framework
         * without replacing the underlying JAX-RS security filter.
         */
        @Test
        void should_return_200_with_all_api_product_permissions_for_admin() {
            Response response = rootTarget("permissions").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);

            @SuppressWarnings("unchecked")
            Map<String, String> permissions = response.readEntity(Map.class);

            assertThat(permissions).isNotNull().hasSize(ApiProductPermission.values().length);

            // Each permission key maps to a string serialised from char[] — verify CRUD chars present
            final String expectedRights = new String(
                new char[] {
                    RolePermissionAction.CREATE.getId(),
                    RolePermissionAction.READ.getId(),
                    RolePermissionAction.UPDATE.getId(),
                    RolePermissionAction.DELETE.getId(),
                }
            );

            Arrays.stream(ApiProductPermission.values()).forEach(perm ->
                assertThat(permissions)
                    .as("Permission map should contain '%s' with full CRUD rights", perm.getName())
                    .containsEntry(perm.getName(), expectedRights)
            );
        }
    }
}
