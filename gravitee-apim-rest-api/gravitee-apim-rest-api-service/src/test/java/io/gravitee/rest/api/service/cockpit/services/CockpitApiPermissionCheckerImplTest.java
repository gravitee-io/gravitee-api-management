/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.cockpit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CockpitApiPermissionCheckerImplTest {

    private static final String API_ID = "api#id";
    private static final String ENVIRONMENT_ID = "user#id";
    private static final String USER_ID = "user#id";

    @Mock
    private PermissionService permissionService;

    private CockpitApiPermissionChecker permissionChecker;

    @Before
    public void setUp() throws Exception {
        permissionChecker = new CockpitApiPermissionCheckerImpl(permissionService);
    }

    @Test
    public void returns_error_message_when_not_allowed_to_create_api() {
        var expectedMessage = "You are not allowed to create APIs on this environment.";

        when(permissionService.hasPermission(USER_ID, RolePermission.ENVIRONMENT_API, ENVIRONMENT_ID, RolePermissionAction.CREATE))
            .thenReturn(false);

        var result = permissionChecker.checkCreatePermission(USER_ID, ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED);

        assertThat(result).contains(expectedMessage);
    }

    @Test
    public void returns_empty_optional_when_allowed_to_create_api() {
        when(permissionService.hasPermission(USER_ID, RolePermission.ENVIRONMENT_API, ENVIRONMENT_ID, RolePermissionAction.CREATE))
            .thenReturn(true);

        var result = permissionChecker.checkCreatePermission(USER_ID, ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED);
        assertThat(result).isEmpty();
    }

    @Test
    public void returns_error_message_when_not_allowed_to_update_api() {
        var expectedMessage = "You are not allowed to update APIs on this environment.";

        when(permissionService.hasPermission(USER_ID, RolePermission.ENVIRONMENT_API, ENVIRONMENT_ID, RolePermissionAction.UPDATE))
            .thenReturn(false);

        var result = permissionChecker.checkUpdatePermission(USER_ID, ENVIRONMENT_ID, API_ID, DeploymentMode.API_DOCUMENTED);

        assertThat(result).contains(expectedMessage);
    }

    @Test
    public void returns_error_message_when_not_allowed_to_update_the_documentation() {
        var expectedMessage = "You are not allowed to update the documentation of this API.";

        allowApiUpdate();
        when(permissionService.hasPermission(USER_ID, RolePermission.API_DOCUMENTATION, API_ID, RolePermissionAction.UPDATE))
            .thenReturn(false);

        var result = permissionChecker.checkUpdatePermission(USER_ID, ENVIRONMENT_ID, API_ID, DeploymentMode.API_DOCUMENTED);

        assertThat(result).contains(expectedMessage);
    }

    @Test
    public void returns_error_message_when_not_allowed_to_update_the_api_definition_for_mocked_deployment_mode() {
        var expectedMessage = "You are not allowed to mock and deploy this API.";

        allowApiUpdate();
        allowApiDocumentationUpdate();
        when(permissionService.hasPermission(USER_ID, RolePermission.API_DEFINITION, API_ID, RolePermissionAction.UPDATE))
            .thenReturn(false);

        var result = permissionChecker.checkUpdatePermission(USER_ID, ENVIRONMENT_ID, API_ID, DeploymentMode.API_MOCKED);

        assertThat(result).contains(expectedMessage);
    }

    @Test
    public void returns_empty_optional_when_allowed_to_update_documented_api() {
        allowApiUpdate();
        allowApiDocumentationUpdate();

        var result = permissionChecker.checkUpdatePermission(USER_ID, ENVIRONMENT_ID, API_ID, DeploymentMode.API_DOCUMENTED);
        assertThat(result).isEmpty();
    }

    @Test
    public void returns_empty_optional_when_allowed_to_update_mocked_api() {
        allowApiUpdate();
        allowApiDocumentationUpdate();
        allowApiDefinitionUpdate();

        var result = permissionChecker.checkUpdatePermission(USER_ID, ENVIRONMENT_ID, API_ID, DeploymentMode.API_MOCKED);
        assertThat(result).isEmpty();
    }

    @Test
    public void returns_empty_optional_when_user_is_admin() {
        makeUserAdmin();

        var result = permissionChecker.checkUpdatePermission(USER_ID, ENVIRONMENT_ID, API_ID, DeploymentMode.API_MOCKED);
        assertThat(result).isEmpty();
    }

    private void allowApiUpdate() {
        when(permissionService.hasPermission(USER_ID, RolePermission.ENVIRONMENT_API, ENVIRONMENT_ID, RolePermissionAction.UPDATE))
            .thenReturn(true);
    }

    private void allowApiDocumentationUpdate() {
        when(permissionService.hasPermission(USER_ID, RolePermission.API_DOCUMENTATION, API_ID, RolePermissionAction.UPDATE))
            .thenReturn(true);
    }

    private void allowApiDefinitionUpdate() {
        when(permissionService.hasPermission(USER_ID, RolePermission.API_DEFINITION, API_ID, RolePermissionAction.UPDATE)).thenReturn(true);
    }

    private void makeUserAdmin() {
        when(permissionService.hasManagementRights(USER_ID)).thenReturn(true);
    }
}
