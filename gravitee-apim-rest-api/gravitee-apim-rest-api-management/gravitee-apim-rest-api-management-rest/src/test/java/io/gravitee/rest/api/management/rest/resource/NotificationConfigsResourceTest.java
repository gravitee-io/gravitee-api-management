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

import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_NOTIFICATION;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.notification.NotificationConfigType;
import io.gravitee.rest.api.model.notification.PortalNotificationConfigEntity;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationConfigsResourceTest {

    @InjectMocks
    private NotificationConfigsResource resource;

    @Mock
    private PortalNotificationConfigService portalNotificationConfigService;

    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private PermissionService permissionService;

    @Test
    public void getNotificationConfigs_hasNotPermission() {
        when(permissionService.hasPermission(any(), eq(ENVIRONMENT_NOTIFICATION), any(), eq(CREATE), eq(UPDATE), eq(DELETE)))
            .thenReturn(false);
        when(securityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        var portalNotificationConfig = PortalNotificationConfigEntity
            .builder()
            .referenceType("refType")
            .referenceId("refId")
            .user("user")
            .build();
        when(portalNotificationConfigService.findById(any(), eq(NotificationReferenceType.ENVIRONMENT), any()))
            .thenReturn(portalNotificationConfig);
        List<Object> configs = resource.getNotificationConfigs();
        assertThat(configs).containsExactlyInAnyOrder(portalNotificationConfig);
    }

    @Test
    public void getNotificationConfigs_hasPermission() {
        when(permissionService.hasPermission(any(), eq(ENVIRONMENT_NOTIFICATION), any(), eq(CREATE), eq(UPDATE), eq(DELETE)))
            .thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        var portalNotificationConfig = PortalNotificationConfigEntity
            .builder()
            .referenceType("refType")
            .referenceId("refId")
            .user("user")
            .build();
        when(portalNotificationConfigService.findById(any(), eq(NotificationReferenceType.ENVIRONMENT), any()))
            .thenReturn(portalNotificationConfig);
        List<GenericNotificationConfigEntity> genericNotificationConfigs = List.of(
            GenericNotificationConfigEntity.builder().id("gn1").build(),
            GenericNotificationConfigEntity.builder().id("gn2").build()
        );
        when(genericNotificationConfigService.findByReference(eq(NotificationReferenceType.ENVIRONMENT), any()))
            .thenReturn(genericNotificationConfigs);
        List<Object> configs = resource.getNotificationConfigs();
        assertThat(configs)
            .containsExactlyInAnyOrder(portalNotificationConfig, genericNotificationConfigs.get(0), genericNotificationConfigs.get(1));
    }

    @Test
    public void createGenericNotificationSetting_refTypeForbiddenAccess() {
        GenericNotificationConfigEntity genericNotificationConfig = GenericNotificationConfigEntity
            .builder()
            .referenceType(NotificationReferenceType.API.name())
            .build();
        assertThrows(ForbiddenAccessException.class, () -> resource.createGenericNotificationSetting(genericNotificationConfig));
    }

    @Test
    public void createGenericNotificationSetting_configTypeGeneric() {
        GenericNotificationConfigEntity config = GenericNotificationConfigEntity
            .builder()
            .referenceType(NotificationReferenceType.ENVIRONMENT.name())
            .configType(NotificationConfigType.GENERIC)
            .build();
        when(genericNotificationConfigService.create(config)).thenAnswer(i -> i.getArguments()[0]);
        when(securityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(permissionService.hasPermission(any(), eq(ENVIRONMENT_NOTIFICATION), eq("DEFAULT"), any())).thenReturn(true);
        GenericNotificationConfigEntity result = (GenericNotificationConfigEntity) resource.createGenericNotificationSetting(config);
        assertThat(result).isEqualTo(config);
        assertThat(result.getOrgId()).isEqualTo("DEFAULT");
    }

    @Test
    public void createGenericNotificationSetting_configTypePortal() {
        GenericNotificationConfigEntity config = GenericNotificationConfigEntity
            .builder()
            .referenceType(NotificationReferenceType.ENVIRONMENT.name())
            .referenceId("DEFAULT")
            .configType(NotificationConfigType.PORTAL)
            .build();
        when(securityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(portalNotificationConfigService.save(resource.convert(config))).thenAnswer(i -> i.getArguments()[0]);
        when(permissionService.hasPermission(any(), eq(ENVIRONMENT_NOTIFICATION), eq("DEFAULT"), any())).thenReturn(true);
        PortalNotificationConfigEntity result = (PortalNotificationConfigEntity) resource.createGenericNotificationSetting(config);
        assertThat(result).isEqualTo(resource.convert(config));
        assertThat(result.getOrgId()).isEqualTo("DEFAULT");
    }

    @Test
    public void updateGenericNotificationSettings_refTypeForbiddenAccess() {
        GenericNotificationConfigEntity genericNotificationConfig = GenericNotificationConfigEntity
            .builder()
            .id("gn1")
            .referenceId("DEFAULT")
            .referenceType(NotificationReferenceType.API.name())
            .configType(NotificationConfigType.GENERIC)
            .build();
        assertThrows(
            ForbiddenAccessException.class,
            () -> resource.updateGenericNotificationSettings(genericNotificationConfig.getId(), genericNotificationConfig)
        );
    }

    @Test
    public void updateGenericNotificationSettings_OK() {
        GenericNotificationConfigEntity config = GenericNotificationConfigEntity
            .builder()
            .id("gn1")
            .referenceId("DEFAULT")
            .referenceType(NotificationReferenceType.ENVIRONMENT.name())
            .configType(NotificationConfigType.GENERIC)
            .build();
        when(genericNotificationConfigService.update(config)).thenReturn(config);
        GenericNotificationConfigEntity result = resource.updateGenericNotificationSettings(config.getId(), config);
        assertThat(result).isEqualTo(config);
    }

    @Test
    public void updatePortalNotificationSettings_refTypeForbiddenAccess() {
        PortalNotificationConfigEntity config = PortalNotificationConfigEntity
            .builder()
            .referenceId("DEFAULT")
            .referenceType(NotificationReferenceType.API.name())
            .configType(NotificationConfigType.PORTAL)
            .build();
        assertThrows(ForbiddenAccessException.class, () -> resource.updatePortalNotificationSettings(config));
    }

    @Test
    public void updatePortalNotificationSettings_OK() {
        PortalNotificationConfigEntity config = PortalNotificationConfigEntity
            .builder()
            .referenceId("DEFAULT")
            .referenceType(NotificationReferenceType.ENVIRONMENT.name())
            .configType(NotificationConfigType.PORTAL)
            .build();
        when(securityContext.getUserPrincipal()).thenReturn(mockPrincipal);
        when(portalNotificationConfigService.save(config)).thenAnswer(i -> i.getArguments()[0]);
        PortalNotificationConfigEntity result = resource.updatePortalNotificationSettings(config);
        assertThat(result).isEqualTo(config);
        assertThat(result.getUser()).isEqualTo("user-test");
        assertThat(result.getOrgId()).isEqualTo("DEFAULT");
    }

    private final Principal mockPrincipal = () -> "user-test";
}
