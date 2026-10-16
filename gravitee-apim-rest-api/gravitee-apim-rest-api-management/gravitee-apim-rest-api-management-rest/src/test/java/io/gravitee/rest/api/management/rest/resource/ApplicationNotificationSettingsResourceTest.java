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

import static io.gravitee.rest.api.model.permissions.RolePermission.APPLICATION_NOTIFICATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.SecurityContext;
import java.lang.reflect.Field;
import java.security.Principal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationNotificationSettingsResourceTest {

    private static final String APPLICATION_ID = "my-application";
    private static final String ORGANIZATION_ID = "my-org";

    @InjectMocks
    private ApplicationNotificationSettingsResource resource;

    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private SecurityContext securityContext;

    private final Principal mockPrincipal = () -> "authenticated-user";

    @BeforeEach
    void setUp() throws Exception {
        Field applicationField = ApplicationNotificationSettingsResource.class.getDeclaredField("application");
        applicationField.setAccessible(true);
        applicationField.set(resource, APPLICATION_ID);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void createApplicationNotificationSettings_takesOrganizationFromContextIgnoringBody() {
        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        when(securityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(permissionService.hasPermission(any(), eq(APPLICATION_NOTIFICATION), eq(APPLICATION_ID), any())).thenReturn(true);
        when(genericNotificationConfigService.create(any())).thenAnswer(i -> i.getArguments()[0]);

        GenericNotificationConfigEntity config = GenericNotificationConfigEntity.builder()
            .referenceType(NotificationReferenceType.APPLICATION.name())
            .referenceId(APPLICATION_ID)
            .configType(NotificationConfigType.GENERIC)
            .organizationId("other-org")
            .build();

        GenericNotificationConfigEntity result = (GenericNotificationConfigEntity) resource.createApplicationNotificationSettings(config);

        assertThat(result.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
    }
}
