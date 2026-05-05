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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.permissions.RolePermission.API_NOTIFICATION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.ws.rs.core.SecurityContext;
import java.lang.reflect.Field;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApiNotificationSettingsResourceTest {

    private static final String API_ID = "my-api";
    private static final String AUTHENTICATED_USER = "authenticated-user";
    private static final String OTHER_USER = "other-user";

    @InjectMocks
    private ApiNotificationSettingsResource resource;

    @Mock
    private PortalNotificationConfigService portalNotificationConfigService;

    @Mock
    private SecurityContext securityContext;

    private final Principal mockPrincipal = () -> AUTHENTICATED_USER;

    @BeforeEach
    void setUp() throws Exception {
        Field apiField = ApiNotificationSettingsResource.class.getDeclaredField("api");
        apiField.setAccessible(true);
        apiField.set(resource, API_ID);
    }

    @Test
    public void updateApiPortalNotificationSettings_requiresUpdatePermission() throws NoSuchMethodException {
        var method = ApiNotificationSettingsResource.class.getDeclaredMethod(
            "updateApiPortalNotificationSettings",
            PortalNotificationConfigEntity.class
        );
        Permissions permissions = method.getAnnotation(Permissions.class);
        assertThat(permissions).isNotNull();
        assertThat(permissions.value()).hasSize(1);
        Permission permission = permissions.value()[0];
        assertThat(permission.value()).isEqualTo(API_NOTIFICATION);
        assertThat(permission.acls()).containsExactly(UPDATE);
    }

    @Test
    public void updateApiPortalNotificationSettings_savesWithAuthenticatedUser() {
        when(securityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(portalNotificationConfigService.save(any())).thenAnswer(i -> i.getArguments()[0]);

        PortalNotificationConfigEntity config = buildConfig(API_ID, AUTHENTICATED_USER);

        PortalNotificationConfigEntity result = resource.updateApiPortalNotificationSettings(config);

        assertThat(result.getUser()).isEqualTo(AUTHENTICATED_USER);
    }

    @Test
    public void updateApiPortalNotificationSettings_enforcesAuthenticatedUserIgnoringBodyUser() {
        when(securityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(portalNotificationConfigService.save(any())).thenAnswer(i -> i.getArguments()[0]);

        PortalNotificationConfigEntity config = buildConfig(API_ID, OTHER_USER);

        PortalNotificationConfigEntity result = resource.updateApiPortalNotificationSettings(config);

        assertThat(result.getUser()).isEqualTo(AUTHENTICATED_USER);
    }

    @Test
    public void updateApiPortalNotificationSettings_throwsForbidden_whenReferenceIdMismatch() {
        PortalNotificationConfigEntity config = buildConfig("different-api", AUTHENTICATED_USER);

        assertThatThrownBy(() -> resource.updateApiPortalNotificationSettings(config)).isInstanceOf(ForbiddenAccessException.class);
    }

    private PortalNotificationConfigEntity buildConfig(String referenceId, String user) {
        PortalNotificationConfigEntity config = new PortalNotificationConfigEntity();
        config.setReferenceType(NotificationReferenceType.API.name());
        config.setReferenceId(referenceId);
        config.setConfigType(NotificationConfigType.PORTAL);
        config.setUser(user);
        return config;
    }
}
