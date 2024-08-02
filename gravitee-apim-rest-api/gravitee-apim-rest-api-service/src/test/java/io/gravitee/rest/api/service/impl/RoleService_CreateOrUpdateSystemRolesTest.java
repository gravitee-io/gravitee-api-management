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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.model.permissions.EnvironmentPermission.DOCUMENTATION;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.model.Role;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class RoleService_CreateOrUpdateSystemRolesTest {

    private static final String REFERENCE_ID = "DEFAULT";
    private static final RoleReferenceType REFERENCE_TYPE = RoleReferenceType.ORGANIZATION;
    private static int[] envtAdminPermissions = Arrays.stream(EnvironmentPermission.values()).mapToInt(ep -> ep.getMask() + 15).toArray();

    @InjectMocks
    private RoleServiceImpl roleService = new RoleServiceImpl();

    @Mock
    private RoleRepository mockRoleRepository;

    @Mock
    private AuditService auditService;

    @Test
    public void shouldCreateSystemRole() throws TechnicalException {
        when(mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(any(), any(), any(), any())).thenReturn(empty());

        roleService.createOrUpdateSystemRoles(GraviteeContext.getExecutionContext(), REFERENCE_ID);

        verify(mockRoleRepository, times(6))
            .findByScopeAndNameAndReferenceIdAndReferenceType(any(), anyString(), eq(REFERENCE_ID), eq(REFERENCE_TYPE));
        verify(mockRoleRepository, never()).update(any());
        verify(mockRoleRepository, times(6)).create(any());
    }

    @Test
    public void shouldUpdateSystemRole() throws TechnicalException {
        Role mgmtAdminRole = mock(Role.class);
        when(mgmtAdminRole.getPermissions()).thenReturn(new int[] { 20, 21 });
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.ENVIRONMENT,
                "ADMIN",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(of(mgmtAdminRole));
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.ORGANIZATION,
                "ADMIN",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(empty());
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.API,
                "PRIMARY_OWNER",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(empty());
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.APPLICATION,
                "PRIMARY_OWNER",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(empty());
        when(mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(RoleScope.GROUP, "ADMIN", REFERENCE_ID, REFERENCE_TYPE))
            .thenReturn(empty());

        roleService.createOrUpdateSystemRoles(GraviteeContext.getExecutionContext(), REFERENCE_ID);

        verify(mockRoleRepository, times(6))
            .findByScopeAndNameAndReferenceIdAndReferenceType(any(), anyString(), eq(REFERENCE_ID), eq(REFERENCE_TYPE));
        verify(mockRoleRepository, times(1))
            .update(
                argThat(o ->
                    o.getScope().equals(RoleScope.ENVIRONMENT) &&
                    Arrays.stream(o.getPermissions()).reduce(Math::addExact).orElse(0) ==
                    Arrays.stream(envtAdminPermissions).reduce(Math::addExact).orElse(0)
                )
            );
        verify(mockRoleRepository, times(5))
            .create(
                argThat(o ->
                    o.getScope().equals(RoleScope.API) ||
                    o.getScope().equals(RoleScope.APPLICATION) ||
                    o.getScope().equals(RoleScope.ORGANIZATION) ||
                    o.getScope().equals(RoleScope.PLATFORM) ||
                    o.getScope().equals(RoleScope.GROUP) ||
                    o.getScope().equals(RoleScope.INTEGRATION)
                )
            );
    }

    @Test
    public void shouldDoNothing() throws TechnicalException {
        Role mgmtAdminRole = mock(Role.class);
        when(mgmtAdminRole.getPermissions()).thenReturn(envtAdminPermissions);
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.ENVIRONMENT,
                "ADMIN",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(of(mgmtAdminRole));
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.API,
                "PRIMARY_OWNER",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(empty());
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.APPLICATION,
                "PRIMARY_OWNER",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenReturn(empty());
        when(mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(RoleScope.GROUP, "ADMIN", REFERENCE_ID, REFERENCE_TYPE))
            .thenReturn(empty());

        roleService.createOrUpdateSystemRoles(GraviteeContext.getExecutionContext(), REFERENCE_ID);

        verify(mockRoleRepository, times(6))
            .findByScopeAndNameAndReferenceIdAndReferenceType(any(), anyString(), eq(REFERENCE_ID), eq(REFERENCE_TYPE));
        verify(mockRoleRepository, never()).update(any());
        verify(mockRoleRepository, times(5))
            .create(
                argThat(o ->
                    o.getScope().equals(RoleScope.API) ||
                    o.getScope().equals(RoleScope.APPLICATION) ||
                    o.getScope().equals(RoleScope.ORGANIZATION) ||
                    o.getScope().equals(RoleScope.PLATFORM) ||
                    o.getScope().equals(RoleScope.GROUP) ||
                    o.getScope().equals(RoleScope.INTEGRATION)
                )
            );
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateBecauseOfTechnicalManagementException() throws TechnicalException {
        Role mgmtAdminRole = mock(Role.class);
        when(
            mockRoleRepository.findByScopeAndNameAndReferenceIdAndReferenceType(
                RoleScope.ENVIRONMENT,
                "ADMIN",
                REFERENCE_ID,
                REFERENCE_TYPE
            )
        )
            .thenThrow(new TechnicalException());

        roleService.createOrUpdateSystemRoles(GraviteeContext.getExecutionContext(), REFERENCE_ID);
    }
}
